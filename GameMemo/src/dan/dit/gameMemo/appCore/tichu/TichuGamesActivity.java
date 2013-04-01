package dan.dit.gameMemo.appCore.tichu;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import dan.dit.gameMemo.R;
import dan.dit.gameMemo.appCore.GameSetupActivity;
import dan.dit.gameMemo.appCore.tichu.TichuGameDetailFragment.DetailViewCallback;
import dan.dit.gameMemo.appCore.tichu.TichuGamesOverviewListFragment.GameOverviewCallback;
import dan.dit.gameMemo.dataExchange.DataExchangeActivity;
import dan.dit.gameMemo.dataExchange.bluetooth.BluetoothDataExchangeActivity;
import dan.dit.gameMemo.dataExchange.file.FileWriteDataExchangeActivity;
import dan.dit.gameMemo.gameData.game.Game;
import dan.dit.gameMemo.gameData.game.GameKey;
import dan.dit.gameMemo.gameData.game.tichu.TichuGame;
import dan.dit.gameMemo.gameData.player.ChoosePlayerDialogFragment.ChoosePlayerDialogListener;
import dan.dit.gameMemo.gameData.player.Player;
import dan.dit.gameMemo.gameData.player.PlayerPool;
import dan.dit.gameMemo.gameData.player.RenamePlayerDialogFragment;
import dan.dit.gameMemo.gameData.player.RenamePlayerDialogFragment.RenamePlayerCallback;
import dan.dit.gameMemo.storage.GameStorageHelper;
import dan.dit.gameMemo.util.ShowStacktraceUncaughtExceptionHandler;
import dan.dit.gameMemo.util.compression.CompressedDataCorruptException;

/**
 * This fragment activity is the home activity for tichu games. It holds
 * a {@link TichuGamesOverviewListFragment} and offers an option menu to start
 * the {@link GameSetupActivity}, the {@link TichuGamesStatisticsActivity}
 * or the {@link BluetoothDataExchangeActivity}.<br>
 * Depending on the layout, it also holds a {@link TichuGameDetailFragment} or starts a {@link TichuGameDetailActivity}
 * when there is a game being selected.
 * @author Daniel
 *
 */
public class TichuGamesActivity extends FragmentActivity implements DetailViewCallback, GameOverviewCallback, RenamePlayerCallback, ChoosePlayerDialogListener  {
	public static final String EXTRA_RESULT_WANT_REMATCH = "dan.dit.gameMemo.WANT_REMATCH";
	private static final int GAME_SETUP_ACTIVITY = 1; // when user wants to start a new game and wants to continue an existing one or selected players
	private static final int[] TICHU_GAME_MIN_PLAYERS = new int[] {2, 2};
	private static final int[] TICHU_GAME_MAX_PLAYERS = new int[] {2, 2};
	private static final boolean[] TICHU_OPTIONS_BOOLEAN = new boolean[] {false}; // {MERCY_RULE}
	private static final int[] TICHU_OPTIONS_NUMBER = new int[] {TichuGame.DEFAULT_SCORE_LIMIT};//{SCORE_LIMIT}
	private static final int[] TICHU_OPTIONS_MIN_NUMBERS = new int[] {TichuGame.MIN_SCORE_LIMIT};
	private static final int[] TICHU_OPTIONS_MAX_NUMBERS = new int[] {TichuGame.MAX_SCORE_LIMIT};
	
	private static final int GAME_DETAIL_ACTIVITY = 2; // when user selected a game, used to return the id of the game to highlight game (and game had no id previously for example)

	private Handler mHandler; // to send messages to this activities UI thread
	
	// member variables if this activity handles the details fragment
	private boolean mHandlesDetailFragment;
	private boolean mIsActivityInitialLaunch;
	private TichuGameDetailFragment mDetailsFragment;
	private TichuGamesOverviewListFragment mOverviewFragment;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Thread.setDefaultUncaughtExceptionHandler(new ShowStacktraceUncaughtExceptionHandler(this));
		setContentView(R.layout.tichu_main);
		mHandler = new Handler();
		mOverviewFragment = (TichuGamesOverviewListFragment) getSupportFragmentManager().findFragmentById(R.id.game_list);
		checkIfHandlesDetailsFragment();
		if (savedInstanceState == null) {
			// initial setup
			mIsActivityInitialLaunch = true;
			Player.loadPlayers(GameKey.TICHU, TichuGame.PLAYERS, getContentResolver());
			loadGameDetails(getIntent().getExtras());
		}
	}
	
	@Override
	public void onStart() {
		super.onStart();
		checkIfHandlesDetailsFragment();
		if (mDetailsFragment == null) {
			// in case reference was lost 
			mDetailsFragment = (TichuGameDetailFragment) getSupportFragmentManager().findFragmentById(R.id.game_detail_frame);
		}
		if (mHandlesDetailFragment) {
			if (mDetailsFragment != null && mDetailsFragment.getDisplayedGameId() != mOverviewFragment.getHighlightedGameId()) {
				setHighlightedGame(mDetailsFragment.getDisplayedGameId());
			}
		} else {
			if (mDetailsFragment != null) {
				// we still have a fragment we do not care about, close it
                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                ft.remove(mDetailsFragment);
                ft.commit();
    			mDetailsFragment = null;
			}
		}
	}
	
	private void checkIfHandlesDetailsFragment() {
		View detailsFrame = findViewById(R.id.game_detail_frame);
		mHandlesDetailFragment =  detailsFrame != null && detailsFrame.getVisibility() == View.VISIBLE;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.tichu_list, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.setup_game:
			startGameSetup(Game.NO_ID);
			return true;
		case R.id.show_statistics:
			showStatistics();
			return true;
		case R.id.open_data_exchanger_bluetooth:
			openDataExchangerBluetooth();
			return true;
		case R.id.share:
			openFileWriterExchanger();
			return true;
		case R.id.rename_player:
			renamePlayerDialog();
			return true;
		case R.id.about:
			showAboutDialog();
			return true;
		case R.id.hints:
			showHintsDialog();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void startGameSetup(long id) {
		Intent i = new Intent(this, GameSetupActivity.class);
		i.putExtra(GameKey.EXTRA_GAMEKEY, GameKey.TICHU);
		i.putExtra(GameSetupActivity.EXTRA_TEAM_MIN_PLAYERS, TICHU_GAME_MIN_PLAYERS);
		i.putExtra(GameSetupActivity.EXTRA_TEAM_MAX_PLAYERS, TICHU_GAME_MAX_PLAYERS);
		i.putExtra(GameSetupActivity.EXTRA_OPTIONS_NUMBER_MAX_VALUES, TICHU_OPTIONS_MAX_NUMBERS);
		i.putExtra(GameSetupActivity.EXTRA_OPTIONS_NUMBER_MIN_VALUES, TICHU_OPTIONS_MIN_NUMBERS);
		i.putExtra(GameSetupActivity.EXTRA_OPTIONS_NUMBER_VALUES, TICHU_OPTIONS_NUMBER);
		i.putExtra(GameSetupActivity.EXTRA_OPTIONS_BOOLEAN_VALUES, TICHU_OPTIONS_BOOLEAN);
		i.putExtra(GameSetupActivity.EXTRA_OPTIONS_NUMBER_NAMES, new String[] {getResources().getString(R.string.tichu_game_score_limit)});
		i.putExtra(GameSetupActivity.EXTRA_OPTIONS_BOOLEAN_NAMES, new String[] {getResources().getString(R.string.tichu_game_mery_rule)});
		i.putExtra(GameSetupActivity.EXTRA_FLAG_SUGGEST_UNFINISHED_GAME, true);
		// priority to copy info from: parameter id, highlighted id, single checked id
		long copyGameSetupId = Game.isValidId(id) ? id : getHighlightedGame();
		if (!Game.isValidId(copyGameSetupId)) {
			Collection<Long> checked = mOverviewFragment.getCheckedIds();
			if (checked.size() == 1) {
				copyGameSetupId = checked.iterator().next();
			}
		}
		if (Game.isValidId(copyGameSetupId)) {
			List<Game> games = null;
			try {
				games = GameKey.loadGames(GameKey.TICHU, getContentResolver(), GameStorageHelper.getUri(GameKey.TICHU, copyGameSetupId));
			} catch (CompressedDataCorruptException e) {
				// fail silently and do not change default information
			}
			if (games != null && games.size() > 0) {
				TichuGame game = (TichuGame) games.get(0);
				List<Player> players = new ArrayList<Player>(TichuGame.TOTAL_PLAYERS);
				for (Player p : game.getTeam1()) {players.add(p);}
				for (Player p : game.getTeam2()) {players.add(p);}				
				String[] playerNames = new String[TichuGame.TOTAL_PLAYERS];
				for (int index = 0; index < playerNames.length; index++) {
					Player curr = players.get(index);
					playerNames[index] = curr == null ? null : curr.getName();
				}
				i.putExtra(GameSetupActivity.EXTRA_PLAYER_NAMES, playerNames);
				i.putExtra(GameSetupActivity.EXTRA_OPTIONS_NUMBER_VALUES, new int[] {game.getScoreLimit()});
				i.putExtra(GameSetupActivity.EXTRA_OPTIONS_BOOLEAN_VALUES, new boolean[] {game.usesMercyRule()});
			}
		}
		startActivityForResult(i, GAME_SETUP_ACTIVITY);
	}
	
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		final Bundle extras = data == null ? null : data.getExtras();
		switch (requestCode) {
		case GAME_SETUP_ACTIVITY:
			if (resultCode == RESULT_OK && extras != null) {
				mHandler.post(new Runnable() {
					@Override
					public void run() {
						reactToGameSetupActivity(extras);
					}
				});
			}
			break;
		case GAME_DETAIL_ACTIVITY:
			if (resultCode == RESULT_OK && extras != null) {
				mHandler.post(new Runnable() {
					@Override
					public void run() {
						long id = extras.getLong(GameStorageHelper.getCursorItemType(GameKey.TICHU), Game.NO_ID);
						setHighlightedGame(id);
						if (extras.getBoolean(EXTRA_RESULT_WANT_REMATCH)) {
							startGameSetup(id);
						}
					}
				});
			}
			break;
		}
	}
	
	private void reactToGameSetupActivity(Bundle extras) {
		if (extras != null) {
			if (extras.containsKey(GameStorageHelper.getCursorItemType(GameKey.TICHU))) {
				selectGame(extras.getLong(GameStorageHelper.getCursorItemType(GameKey.TICHU)));
			} else {
				String[] playerNames = extras.getStringArray(GameSetupActivity.EXTRA_PLAYER_NAMES);
				boolean[] boolOptions = extras.getBooleanArray(GameSetupActivity.EXTRA_OPTIONS_BOOLEAN_VALUES);
				int[] numberOptions = extras.getIntArray(GameSetupActivity.EXTRA_OPTIONS_NUMBER_VALUES);
				if (playerNames != null && playerNames.length >= TichuGame.TOTAL_PLAYERS) {
					extras.putString(TichuGameDetailFragment.EXTRAS_TEAM1_PLAYER_1, playerNames[0]);
					extras.putString(TichuGameDetailFragment.EXTRAS_TEAM1_PLAYER_2, playerNames[1]);
					extras.putString(TichuGameDetailFragment.EXTRAS_TEAM2_PLAYER_1, playerNames[2]);
					extras.putString(TichuGameDetailFragment.EXTRAS_TEAM2_PLAYER_2, playerNames[3]);
					if (numberOptions != null) {
						extras.putInt(TichuGameDetailFragment.EXTRA_NEW_GAME_SCORE_LIMIT, numberOptions[0]);
					}
					if (boolOptions != null) {
						extras.putBoolean(TichuGameDetailFragment.EXTRA_NEW_GAME_USE_MERCY_RULE, boolOptions[0]);
					}
					loadGameDetails(extras);
				}
			}
		}
	}
	
	private void showStatistics() {
		Intent i = new Intent(this, TichuGamesStatisticsActivity.class);
		startActivity(i);
	}
	
	private void showAboutDialog() {
		String versionName = "";
		try {
			versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
		} catch (NameNotFoundException e) {
			// will not happen since we ask for own own package which exists
		}
		new AlertDialog.Builder(this)
		.setTitle(getResources().getString(R.string.about))
		.setMessage(getResources().getString(R.string.about_summary, versionName))
		.setIcon(android.R.drawable.ic_dialog_info)
		.setNeutralButton(android.R.string.ok, null)
		.show();
	}
	
	private void showHintsDialog() {
		View hints = getLayoutInflater().inflate(R.layout.tichu_hints, null);
		WebView hints_list = (WebView) hints.findViewById(R.id.hints_list);
		hints_list.loadDataWithBaseURL(null, getResources().getString(R.string.hints_message), "text/html", "utf-8", null);
		new AlertDialog.Builder(this)
		.setTitle(getResources().getString(R.string.hints))
		.setView(hints)
		.setIcon(android.R.drawable.ic_dialog_info)
		.setNeutralButton(android.R.string.ok, null)
		.show();
	}
	
	private void openDataExchangerBluetooth() {
		Intent i = new Intent(this, BluetoothDataExchangeActivity.class);
		i.putExtra(GameKey.EXTRA_GAMEKEY, new int[] {GameKey.TICHU});
		startActivity(i);
	}
	
	private void openFileWriterExchanger() {
		Intent i = new Intent(this, FileWriteDataExchangeActivity.class);
		i.putExtra(GameKey.EXTRA_GAMEKEY, new int[] {GameKey.TICHU});
		i.putExtra(FileWriteDataExchangeActivity.EXTRA_FLAG_START_SHARE_IMMEDIATELY, true);
		Collection<Long> checkedStarttimes = mOverviewFragment.getCheckedGamesStarttimes();
		if (checkedStarttimes.size() > 0) {
			i.putExtra(DataExchangeActivity.EXTRA_SINGLE_GAME_OFFERS, GameKey.toArray(checkedStarttimes));
		}
		startActivity(i);
	}
	
	private void renamePlayerDialog() {
        DialogFragment dialog = new RenamePlayerDialogFragment();
        Bundle args = new Bundle();
        args.putInt(GameKey.EXTRA_GAMEKEY, GameKey.TICHU);
        dialog.setArguments(args);
        dialog.show(getSupportFragmentManager(), "RenamePlayerDialogFragment");
	}

	@Override
	public void closeDetailView(boolean error, boolean rematch) {
		if (mHandlesDetailFragment) {
			boolean displayedId = mDetailsFragment != null;
			long idToDisplay = Game.NO_ID;
			if (displayedId) {
				idToDisplay = mDetailsFragment.getDisplayedGameId();
			}
			selectGame(Game.NO_ID);
			if (displayedId) {
				setHighlightedGame(idToDisplay);
				if (rematch) {
					startGameSetup(idToDisplay);
				}
			}
		}
	}

	@Override
	public void selectGame(long gameId) {
		if (mHandlesDetailFragment) {
			// Check what fragment is currently shown, replace if needed.
            if (mDetailsFragment == null || gameId == Game.NO_ID || mDetailsFragment.getDisplayedGameId() != gameId) {
                // Execute a transaction, replacing any existing fragment
                // with this one inside the frame.
                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                if (Game.isValidId(gameId)) {              
                	// Make new fragment to show this selection.
                	if (mDetailsFragment == null) {
                        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                	} else {
                		ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
                	}
                	Log.d("Tichu", "Detail fragment is " + mDetailsFragment + " replaced by game id " + gameId);
                	mDetailsFragment = TichuGameDetailFragment.newInstance(gameId);
                    ft.replace(R.id.game_detail_frame, mDetailsFragment);
                } else if (mDetailsFragment != null) {
                	Log.d("Tichu", "Removing detail fragment");
                    ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE);
                	ft.remove(mDetailsFragment);
                	mDetailsFragment = null;
                }
                ft.commit();
            }
    		setHighlightedGame(gameId);
		} else if (Game.isValidId(gameId)) {
			Intent i = new Intent(this, TichuGameDetailActivity.class);
			i.putExtra(GameStorageHelper.getCursorItemType(GameKey.TICHU), gameId);
			startActivityForResult(i, GAME_DETAIL_ACTIVITY);			
		}
	}

	@Override
	public long getSelectedGameId() {
		return getHighlightedGame();
	}
	
	private void loadGameDetails(Bundle extras) {
		if (extras == null) {
			return;
		}
		if (mHandlesDetailFragment) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            mDetailsFragment = TichuGameDetailFragment.newInstance(extras);
        	if (mDetailsFragment == null) {
                ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        	} else {
        		ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        	}
            ft.replace(R.id.game_detail_frame, mDetailsFragment);
            ft.commit();
    		setHighlightedGame(extras.getLong(GameStorageHelper.getCursorItemType(GameKey.TICHU), Game.NO_ID));
		} else {
			Intent i = new Intent(this, TichuGameDetailActivity.class);
			TichuGameDetailFragment.copyExtrasToIntent(i, extras);
			startActivityForResult(i, GAME_DETAIL_ACTIVITY);
		}
	}
	
	private void setHighlightedGame(long highlightedGameIdHint) {
		long selectedId = (highlightedGameIdHint != Game.NO_ID || mDetailsFragment == null) ? 
				highlightedGameIdHint : 
				mDetailsFragment.getDisplayedGameId();
		mOverviewFragment.setHighlightedGameId(selectedId);
	}
	
	private long getHighlightedGame() {
		TichuGamesOverviewListFragment frag = (TichuGamesOverviewListFragment) getSupportFragmentManager().findFragmentById(R.id.game_list);
		return frag.getHighlightedGameId();
	}

	@Override
	public void onRenameSuccess(Player newPlayer, String oldName) {
		if (mDetailsFragment != null) {
			mDetailsFragment.onRenameSuccess(newPlayer, oldName);
		}
	}

	@Override
	public PlayerPool getPool() {
		if (mDetailsFragment != null) {
			return mDetailsFragment.getPool();
		} else {
			return null;
		}
	}

	@Override
	public List<Player> toFilter() {
		if (mDetailsFragment != null) {
			return mDetailsFragment.toFilter();
		} else {
			return null;
		}
	}

	@Override
	public void playerChosen(Player chosen) {
		if (mDetailsFragment != null) {
			mDetailsFragment.playerChosen(chosen);
		}
	}

	@Override
	public void setupGame() {
		if (mIsActivityInitialLaunch) {
			startGameSetup(Game.NO_ID);
		}
	}

	@Override
	public void setInfo(CharSequence main, CharSequence extra) {
		if (mDetailsFragment != null) {
			mDetailsFragment.setInfoText(main, extra);
		}
	}

}
