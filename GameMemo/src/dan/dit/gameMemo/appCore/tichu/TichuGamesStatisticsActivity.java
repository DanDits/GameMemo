package dan.dit.gameMemo.appCore.tichu;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import dan.dit.gameMemo.R;
import dan.dit.gameMemo.gameData.game.GameKey;
import dan.dit.gameMemo.gameData.game.tichu.TichuGame;
import dan.dit.gameMemo.gameData.player.Player;
import dan.dit.gameMemo.gameData.statistics.AsyncLoadAndBuildStatistics;
import dan.dit.gameMemo.gameData.statistics.GameStatistic;
import dan.dit.gameMemo.gameData.statistics.onStatisticBuildCompleteListener;
import dan.dit.gameMemo.gameData.statistics.tichu.TichuStatistic;
import dan.dit.gameMemo.gameData.statistics.tichu.TichuStatisticType;
import dan.dit.gameMemo.storage.GameStorageHelper;
import dan.dit.gameMemo.util.ShowStacktraceUncaughtExceptionHandler;

/**
 * Activity for visualizing tichu game statistics. Offers two main tabs, one
 * where statistics are visualized for a player that can be selected from a
 * spinner and one tab where a statistic of a certain type is visualized for all
 * players.<br>
 * Loading games and building statistics is done in a background thread and will
 * not block the UI.
 * 
 * @author Daniel
 * 
 */
public class TichuGamesStatisticsActivity extends Activity {
	private static final String STORAGE_ONLY_SHOW_SIGNIFICANT = "statistics.only_show_significant";
	private static final String STORAGE_SORT_BY_PERCENTAGE = "statistics.sort_by_percentage";
	private static final boolean DEFAULT_SORT_BY_PERCENTAGE = false;
	private static final boolean DEFAULT_ONLY_SHOW_SIGNIFICANT = false;
	private static final boolean DEFAULT_SHOW_WON_PERCENTAGE = true;
	private static final float DEFAULT_SIGNIFICANT_BUT_ANYWAYS_SHOWN_ALPHA = 0.3f;

	private List<Player> players;
	private AsyncLoadAndBuildStatistics statBuilder;
	private TichuStatistic stat;
	private Spinner mSelectPlayer;
	private Spinner mSelectGenre;
	private boolean mSortByPercentage = DEFAULT_SORT_BY_PERCENTAGE;
	private boolean mOnlyShowSignificantForGenre = DEFAULT_ONLY_SHOW_SIGNIFICANT;

	// TODO this activity needs to be reworked, offering a more general approach
	// to visualize and handle statistics, making it easier and more flexible to
	// add new ones
	@SuppressLint("NewApi")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.tichu_statistics);
		Thread.setDefaultUncaughtExceptionHandler(new ShowStacktraceUncaughtExceptionHandler(
				this));
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			ActionBar bar = getActionBar();
			if (bar != null) {
				bar.hide();
			}
		}
		if (savedInstanceState != null) {
			mOnlyShowSignificantForGenre = savedInstanceState.getBoolean(
					STORAGE_ONLY_SHOW_SIGNIFICANT,
					DEFAULT_ONLY_SHOW_SIGNIFICANT);
			mSortByPercentage = savedInstanceState.getBoolean(
					STORAGE_SORT_BY_PERCENTAGE, DEFAULT_SORT_BY_PERCENTAGE);
		}
		mSelectPlayer = ((Spinner) findViewById(R.id.statistics_select_player));
		mSelectGenre = ((Spinner) findViewById(R.id.statistics_select_genre));

		// init tabs
		TabHost tabs = (TabHost) findViewById(android.R.id.tabhost);
		tabs.setup();
		TabHost.TabSpec spec1 = tabs.newTabSpec("tag1");
		spec1.setContent(R.id.statistics_by_player);
		spec1.setIndicator(getResources().getString(
				R.string.statistics_by_player));
		TabHost.TabSpec spec2 = tabs.newTabSpec("tag2");
		spec2.setContent(R.id.statistics_b_genre);
		spec2.setIndicator(getResources().getString(
				R.string.statistics_by_genre));
		tabs.addTab(spec1);
		tabs.addTab(spec2);
		// init stats
		refreshStatistics();
	}

	private void refreshStatistics() {
		cancelBuilding();
		this.players = new ArrayList<Player>(TichuGame.PLAYERS.getAll());
		if (this.players.size() == 0) {
			clearGenresStatistics();
			clearPlayerStatistics();
			return; // nothing to do here, no players, no stats
		}
		refreshByPlayerTab();
		refreshByGenreTab();
		loadAndBuildStats();
	}

	private void refreshByPlayerTab() {
		ArrayAdapter<Player> playerAdapter = new ArrayAdapter<Player>(this,
				android.R.layout.simple_spinner_item, android.R.id.text1);
		playerAdapter
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mSelectPlayer.setAdapter(playerAdapter);
		mSelectPlayer.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parentView,
					View selectedItemView, int position, long id) {
				fillPlayerStatistics();
			}

			@Override
			public void onNothingSelected(AdapterView<?> a) {
				clearPlayerStatistics();
			}

		});
		for (Player p : this.players) {
			playerAdapter.add(p);
		}
	}

	private void refreshByGenreTab() {
		ArrayAdapter<TichuStatisticType> typeAdapter = new ArrayAdapter<TichuStatisticType>(
				this, android.R.layout.simple_spinner_item, android.R.id.text1);
		typeAdapter
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mSelectGenre.setAdapter(typeAdapter);
		mSelectGenre.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parentView,
					View selectedItemView, int position, long id) {
				fillGenresStatistics();
			}

			@Override
			public void onNothingSelected(AdapterView<?> a) {
				clearGenresStatistics();
			}

		});
		for (TichuStatisticType t : TichuStatisticType.values()) {
			setTichuStatisticTypeName(t);
			typeAdapter.add(t);
		}
	}

	private void loadAndBuildStats() {
		this.statBuilder = new AsyncLoadAndBuildStatistics(
				getContentResolver(), players, GameKey.TICHU);
		this.statBuilder.addListener(new onStatisticBuildCompleteListener() {

			@Override
			public void statisticComplete(GameStatistic result) {
				stat = (TichuStatistic) result;
				fillPlayerStatistics();
				fillGenresStatistics();
			}
		});
		this.statBuilder.execute(GameStorageHelper
				.getUriAllItems(GameKey.TICHU));
	}

	private void clearPlayerStatistics() {
		((TextView) findViewById(R.id.statistic_average_finisher_pos))
				.setText("");
		((TextView) findViewById(R.id.statistic_big_tichu_bids)).setText("");
		((TextView) findViewById(R.id.statistic_big_tichus_won)).setText("");
		((TextView) findViewById(R.id.statistic_small_tichu_bids)).setText("");
		((TextView) findViewById(R.id.statistic_small_tichus_won)).setText("");
		((TextView) findViewById(R.id.statistic_games_played)).setText("");
		((TextView) findViewById(R.id.statistic_games_won)).setText("");
		((TextView) findViewById(R.id.statistic_rounds_played)).setText("");
		((TextView) findViewById(R.id.statistic_rounds_won)).setText("");
	}

	private void fillPlayerStatistics() {
		if (stat != null) {
			Object selItem = mSelectPlayer.getSelectedItem();
			Player selectedPlayer = selItem == null ? null : (Player) selItem;
			int index = stat.getIndex(selectedPlayer);
			if (index >= 0) {
				double[] data = stat.getStatistics(index);
				((TextView) findViewById(R.id.statistics_score_per_round_incl_tichus))
						.setText(dataToString(data[TichuStatisticType.SCORE_PER_ROUND_INCL_TICHUS
								.ordinal()]));
				((TextView) findViewById(R.id.statistics_score_per_round_no_tichus))
						.setText(dataToString(data[TichuStatisticType.SCORE_PER_ROUND_NO_TICHUS
								.ordinal()]));
				((TextView) findViewById(R.id.statistic_average_finisher_pos))
						.setText(dataToString(data[TichuStatisticType.FINISHER_POS_AVERAGE
								.ordinal()]));
				fillRow_TotalWonPercentage(
						(TextView) findViewById(R.id.statistic_big_tichu_bids),
						(TextView) findViewById(R.id.statistic_big_tichus_won),
						(int) data[TichuStatisticType.BIG_TICHU_BIDS_ABS
								.ordinal()],
						(int) data[TichuStatisticType.BIG_TICHUS_WON_ABS
								.ordinal()]);
				fillRow_TotalWonPercentage(
						(TextView) findViewById(R.id.statistic_small_tichu_bids),
						(TextView) findViewById(R.id.statistic_small_tichus_won),
						(int) data[TichuStatisticType.SMALL_TICHU_BIDS_ABS
								.ordinal()],
						(int) data[TichuStatisticType.SMALL_TICHUS_WON_ABS
								.ordinal()]);
				fillRow_TotalWonPercentage(
						(TextView) findViewById(R.id.statistic_games_played),
						(TextView) findViewById(R.id.statistic_games_won),
						(int) data[TichuStatisticType.GAMES_PLAYED.ordinal()],
						(int) data[TichuStatisticType.GAMES_WON_ABS.ordinal()]);
				fillRow_TotalWonPercentage(
						(TextView) findViewById(R.id.statistic_rounds_played),
						(TextView) findViewById(R.id.statistic_rounds_won),
						(int) data[TichuStatisticType.GAME_ROUNDS_PLAYED
								.ordinal()],
						(int) data[TichuStatisticType.GAME_ROUNDS_WON_RAWSCORE_ABS
								.ordinal()]);
			} else {
				clearPlayerStatistics();
			}
		} else {
			clearPlayerStatistics();
		}
	}

	private void clearGenresStatistics() {
		TableLayout table = ((TableLayout) findViewById(R.id.statistics_by_genre_table));
		// remove every row except the first one
		for (int i = table.getChildCount() - 1; i > 0; i--) {
			table.removeViewAt(i);
		}
	}

	@SuppressLint("NewApi")
	private void fillGenresStatistics() {
		clearGenresStatistics();
		if (stat != null) {
			Object selItem = mSelectGenre.getSelectedItem();
			TichuStatisticType selectedType = selItem == null ? null
					: (TichuStatisticType) selItem;
			if (selectedType != null) {
				double[] data = stat.getStatistic(selectedType);
				boolean[] dataIncluded = new boolean[data.length];
				// selection sort, since i need the permutation indices which
				// are not supplied by Collections.sort, there will never be too
				// many players anyway
				boolean done = false;
				TableLayout table = ((TableLayout) findViewById(R.id.statistics_by_genre_table));
				while (!done) {
					done = true;
					double max = Double.NEGATIVE_INFINITY;
					int highestIndex = -1;
					for (int i = 0; i < data.length; i++) {
						double currData;
						if (mSortByPercentage
								&& selectedType.getAbsoluteType() != null) {
							int absData = (int) stat.getStatistic(
									selectedType.getAbsoluteType(), i);
							currData = absData == 0 ? (-Double.MAX_VALUE)
									: data[i] / absData;
						} else {
							currData = data[i];
						}
						if (!dataIncluded[i] && currData > max) {
							done = false;
							max = currData;
							highestIndex = i;
						}
					}

					if (highestIndex >= 0) {
						// found a valid player index, check if this player data
						// should be shown
						dataIncluded[highestIndex] = true;
						boolean isDataMeaningful = selectedType
								.isMeaningfulData(max, stat.getStatistic(
										TichuStatisticType.GAME_ROUNDS_PLAYED,
										highestIndex), stat.getStatistic(
										TichuStatisticType.GAMES_PLAYED,
										highestIndex));
						if (!mOnlyShowSignificantForGenre || isDataMeaningful) {
							// showing every player data or player data is
							// significant as defined by the data type
							TableRow row = (TableRow) getLayoutInflater()
									.inflate(
											R.layout.statistics_by_genre_table_row,
											table, false);
							TextView playerNameView = ((TextView) row
									.findViewById(R.id.statistics_by_genre_row_player_name));
							TextView valueView = ((TextView) row
									.findViewById(R.id.statistics_by_genre_row_value));
							if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
								if (isDataMeaningful) {
									playerNameView.setAlpha(1f);
									valueView.setAlpha(1f);
								} else {
									playerNameView
											.setAlpha(DEFAULT_SIGNIFICANT_BUT_ANYWAYS_SHOWN_ALPHA);
									valueView
											.setAlpha(DEFAULT_SIGNIFICANT_BUT_ANYWAYS_SHOWN_ALPHA);
								}
							}
							playerNameView.setText(players.get(highestIndex)
									.getName());
							valueView
									.setText(dataToString(
											data[highestIndex],
											selectedType.getAbsoluteType() != null ? (int) stat
													.getStatistic(selectedType
															.getAbsoluteType(),
															highestIndex)
													: Integer.MIN_VALUE));
							if (selectedType.isHigherBetter()) {
								table.addView(row); // add to end of table
							} else {
								table.addView(row, 1); // add to front, at 0.row
														// there is the header
							}
						}
					}
				}
			}
		}
	}

	private void cancelBuilding() {
		if (this.statBuilder != null) {
			this.statBuilder.cancel(true);
		}
	}

	private void fillRow_TotalWonPercentage(TextView totalValueView,
			TextView wonValueView, int total, int won) {
		totalValueView.setText(String.valueOf(total));
		wonValueView.setText(dataToString(won, total));
	}

	private String dataToString(double data, int baseData) {
		if (baseData != Integer.MIN_VALUE && baseData != 0) {
			// baseData not null and existing
			StringBuilder builder = new StringBuilder();
			builder.append(dataToString(data));
			if (DEFAULT_SHOW_WON_PERCENTAGE) {
				builder.append(' ');
				builder.append('(');
				builder.append(NumberFormat.getPercentInstance().format(
						data / (double) baseData));
				builder.append(')');
			}
			return builder.toString();
		} else {
			return dataToString(data);
		}
	}

	private String dataToString(double data) {
		if (Math.abs(((int) data) - data) < 10E-10) {
			return NumberFormat.getIntegerInstance().format(data);
		} else {
			return NumberFormat.getNumberInstance().format(data);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.statistics, menu);
		menu.findItem(R.id.statistics_toggle_sort_type).setTitle(
				getIdOfNextSortType());
		menu.findItem(R.id.statistics_only_show_significant).setChecked(
				mOnlyShowSignificantForGenre);
		return true;
	}

	private int getIdOfNextSortType() {
		if (mSortByPercentage) {
			return R.string.statistics_sort_by_value;
		} else {
			return R.string.statistics_sort_by_percentage;
		}
	}

	private void toggleSortType() {
		mSortByPercentage = !mSortByPercentage;
		fillGenresStatistics();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.statistics_toggle_sort_type:
			toggleSortType();
			item.setTitle(getIdOfNextSortType());
			return true;
		case R.id.statistics_only_show_significant:
			boolean newState = !item.isChecked();
			item.setChecked(newState);
			mOnlyShowSignificantForGenre = newState;
			fillGenresStatistics();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void setTichuStatisticTypeName(TichuStatisticType type) {
		// cannot get resources from TichuStatisticType, so i do it here..
		if (type == null) {
			return;
		}
		switch (type) {
		case BIG_TICHU_BIDS_ABS:
			type.setName(getResources().getString(
					R.string.statistics_big_tichus_descr));
			break;
		case BIG_TICHUS_WON_ABS:
			type.setName(getResources().getString(
					R.string.statistics_big_tichus_won_descr));
			break;
		case FINISHER_POS_AVERAGE:
			type.setName(getResources().getString(
					R.string.statistics_average_finisher_pos_descr));
			break;
		case GAME_ROUNDS_PLAYED:
			type.setName(getResources().getString(
					R.string.statistics_rounds_played_descr));
			break;
		case GAME_ROUNDS_WON_RAWSCORE_ABS:
			type.setName(getResources().getString(
					R.string.statistics_rounds_won_descr));
			break;
		case GAMES_PLAYED:
			type.setName(getResources().getString(
					R.string.statistics_games_played_descr));
			break;
		case GAMES_WON_ABS:
			type.setName(getResources().getString(
					R.string.statistics_games_won_descr));
			break;
		case SMALL_TICHU_BIDS_ABS:
			type.setName(getResources().getString(
					R.string.statistics_small_tichus_descr));
			break;
		case SMALL_TICHUS_WON_ABS:
			type.setName(getResources().getString(
					R.string.statistics_small_tichus_won_descr));
			break;
		case SCORE_PER_ROUND_INCL_TICHUS:
			type.setName(getResources().getString(
					R.string.statistics_score_per_round_incl_tichus));
			break;
		case SCORE_PER_ROUND_NO_TICHUS:
			type.setName(getResources().getString(
					R.string.statistics_score_per_round_no_tichus));
			break;
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(STORAGE_ONLY_SHOW_SIGNIFICANT,
				mOnlyShowSignificantForGenre);
		outState.putBoolean(STORAGE_SORT_BY_PERCENTAGE, mSortByPercentage);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		cancelBuilding();
	}
}
