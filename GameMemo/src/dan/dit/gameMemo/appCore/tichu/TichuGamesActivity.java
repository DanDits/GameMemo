package dan.dit.gameMemo.appCore.tichu;

import java.util.List;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import dan.dit.gameMemo.R;
import dan.dit.gameMemo.appCore.GamePlayerSelectionActivity;
import dan.dit.gameMemo.appCore.tichu.TichuGameDetailFragment.CloseDetailViewRequestListener;
import dan.dit.gameMemo.appCore.tichu.TichuGamesOverviewListFragment.GameSelectionListener;
import dan.dit.gameMemo.dataExchange.bluetooth.BluetoothDataExchangeActivity;
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

/**
 * This fragment activity is the home activity for tichu games. It holds
 * a {@link TichuGamesOverviewListFragment} and offers an option menu to start
 * the {@link GamePlayerSelectionActivity}, the {@link TichuGamesStatisticsActivity}
 * or the {@link BluetoothDataExchangeActivity}.<br>
 * Depending on the layout, it also holds a {@link TichuGameDetailFragment} or starts a {@link TichuGameDetailActivity}
 * when there is a game being selected.
 * @author Daniel
 *
 */
public class TichuGamesActivity extends FragmentActivity implements CloseDetailViewRequestListener, GameSelectionListener, RenamePlayerCallback, ChoosePlayerDialogListener  {
	private static final int PLAYER_SELECTION_ACTIVITY = 1; // when user wants to start a new game and wants to continue an existing one or selected players
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
	private TichuGameDetailFragment mDetailsFragment;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Thread.setDefaultUncaughtExceptionHandler(new ShowStacktraceUncaughtExceptionHandler(this));
		setContentView(R.layout.tichu_main);
		mHandler = new Handler();
		checkIfHandlesDetailsFragment();
		if (savedInstanceState == null) {
			// initial setup
			Player.loadPlayers(GameKey.TICHU, TichuGame.PLAYERS, getContentResolver());
			loadGameDetails(getIntent().getExtras());
		}
		restoreFromSave(savedInstanceState);
	}
	
	@Override
	public void onRestart() {
		super.onRestart();
		checkIfHandlesDetailsFragment();
		if (mHandlesDetailFragment) {
			TichuGamesOverviewListFragment overview = (TichuGamesOverviewListFragment) getSupportFragmentManager().findFragmentById(R.id.game_list);
			if (mDetailsFragment != null && mDetailsFragment.getDisplayedGameId() != overview.getHighlightedGameId()) {
				setHighlightedGame(mDetailsFragment.getDisplayedGameId());
			}
		}
	}
	
	private void checkIfHandlesDetailsFragment() {
		View detailsFrame = findViewById(R.id.game_detail_frame);
		mHandlesDetailFragment =  detailsFrame != null && detailsFrame.getVisibility() == View.VISIBLE;
	}
	
	private void restoreFromSave(Bundle savedInstanceState) {
		if (savedInstanceState == null) {
			return; // this is no save restorage but activity got created for the first time
		}
		if (mHandlesDetailFragment) {
			mDetailsFragment = (TichuGameDetailFragment) getSupportFragmentManager().findFragmentById(R.id.game_detail_frame);
		} // else it does not keep any data that is to be restored
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
		case R.id.new_tichu_game:
			createTichuGame();
			return true;
		case R.id.show_statistics:
			showStatistics();
			return true;
		case R.id.open_data_exchanger_bluetooth:
			openDataExchangerBluetooth();
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

	private void createTichuGame() {
		Intent i = new Intent(this, GamePlayerSelectionActivity.class);
		i.putExtra(GameKey.EXTRA_GAMEKEY, GameKey.TICHU);
		i.putExtra(GamePlayerSelectionActivity.EXTRA_TEAM_MIN_PLAYERS, TICHU_GAME_MIN_PLAYERS);
		i.putExtra(GamePlayerSelectionActivity.EXTRA_TEAM_MAX_PLAYERS, TICHU_GAME_MAX_PLAYERS);
		i.putExtra(GamePlayerSelectionActivity.EXTRA_OPTIONS_NUMBER_MAX_VALUES, TICHU_OPTIONS_MAX_NUMBERS);
		i.putExtra(GamePlayerSelectionActivity.EXTRA_OPTIONS_NUMBER_MIN_VALUES, TICHU_OPTIONS_MIN_NUMBERS);
		i.putExtra(GamePlayerSelectionActivity.EXTRA_OPTIONS_NUMBER_VALUES, TICHU_OPTIONS_NUMBER);
		i.putExtra(GamePlayerSelectionActivity.EXTRA_OPTIONS_BOOLEAN_VALUES, TICHU_OPTIONS_BOOLEAN);
		i.putExtra(GamePlayerSelectionActivity.EXTRA_OPTIONS_NUMBER_NAMES, new String[] {getResources().getString(R.string.tichu_game_score_limit)});
		i.putExtra(GamePlayerSelectionActivity.EXTRA_OPTIONS_BOOLEAN_NAMES, new String[] {getResources().getString(R.string.tichu_game_mery_rule)});
		long highlightedId = getHighlightedGame();
		if (Game.isValidId(highlightedId)) {
			// extract player data from highlighted game and pass it as a suggestion to the selection activity, this allows simple rematches
			List<Player> players = Game.getPlayers(getContentResolver(), GameKey.TICHU, highlightedId);
			if (players != null && players.size() > 0) {
				String[] playerNames = new String[players.size()];
				for (int index = 0; index < playerNames.length; index++) {
					Player curr = players.get(index);
					playerNames[index] = curr == null ? null : curr.getName();
				}
				i.putExtra(GamePlayerSelectionActivity.EXTRA_PLAYER_NAMES, playerNames);
			}
		}
		startActivityForResult(i, PLAYER_SELECTION_ACTIVITY);
	}
	
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		final Bundle extras = data == null ? null : data.getExtras();
		switch (requestCode) {
		case PLAYER_SELECTION_ACTIVITY:
			if (resultCode == RESULT_OK && extras != null) {
				mHandler.post(new Runnable() {
					@Override
					public void run() {
						reactToPlayerSelectionActivity(extras);
					}
				});
			}
			break;
		case GAME_DETAIL_ACTIVITY:
			if (resultCode == RESULT_OK && extras != null) {
				mHandler.post(new Runnable() {
					@Override
					public void run() {
						setHighlightedGame(extras.getLong(GameStorageHelper.getCursorItemType(GameKey.TICHU), Game.NO_ID));
					}
				});
			}
			break;
		}
	}
	
	private void reactToPlayerSelectionActivity(Bundle extras) {
		if (extras != null) {
			if (extras.containsKey(GameStorageHelper.getCursorItemType(GameKey.TICHU))) {
				selectGame(extras.getLong(GameStorageHelper.getCursorItemType(GameKey.TICHU)));
			} else {
				String[] playerNames = extras.getStringArray(GamePlayerSelectionActivity.EXTRA_PLAYER_NAMES);
				boolean[] boolOptions = extras.getBooleanArray(GamePlayerSelectionActivity.EXTRA_OPTIONS_BOOLEAN_VALUES);
				int[] numberOptions = extras.getIntArray(GamePlayerSelectionActivity.EXTRA_OPTIONS_NUMBER_VALUES);
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
		new AlertDialog.Builder(this)
		.setTitle(getResources().getString(R.string.about))
		.setMessage(getResources().getString(R.string.about_summary))
		.setIcon(android.R.drawable.ic_dialog_info)
		.setNeutralButton(android.R.string.ok, null)
		.show();
	}
	
	private void showHintsDialog() {
		View hints = getLayoutInflater().inflate(R.layout.tichu_hints, null);
		WebView hints_list = (WebView) hints.findViewById(R.id.hints_list);
		hints_list.loadData(getResources().getString(R.string.hints_message), "text/html", null);
		new AlertDialog.Builder(this)
		.setTitle(getResources().getString(R.string.hints))
		.setView(hints)
		.setIcon(android.R.drawable.ic_dialog_info)
		.setNeutralButton(android.R.string.ok, null)
		.show();
	}
	
	private void openDataExchangerBluetooth() {
		Intent i = new Intent(this, BluetoothDataExchangeActivity.class);
		i.putExtra(GameKey.EXTRA_GAMEKEY, GameKey.TICHU);
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
	public void closeDetailView(boolean error) {
		if (mHandlesDetailFragment) {
			boolean displayedId = mDetailsFragment != null;
			long idToDisplay = Game.NO_ID;
			if (displayedId) {
				idToDisplay = mDetailsFragment.getDisplayedGameId();
			}
			selectGame(Game.NO_ID);
			if (displayedId) {
				setHighlightedGame(idToDisplay);
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
                	mDetailsFragment = TichuGameDetailFragment.newInstance(gameId);
                	if (mDetailsFragment == null) {
                        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                	} else {
                		ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
                	}
                    ft.replace(R.id.game_detail_frame, mDetailsFragment);
                } else if (mDetailsFragment != null) {
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
		TichuGamesOverviewListFragment frag = (TichuGamesOverviewListFragment) getSupportFragmentManager().findFragmentById(R.id.game_list);
		long selectedId = (highlightedGameIdHint != Game.NO_ID || mDetailsFragment == null) ? 
				highlightedGameIdHint : 
				mDetailsFragment.getDisplayedGameId();
		frag.setHighlightedGameId(selectedId);
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
}