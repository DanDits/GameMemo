package dan.dit.gameMemo.appCore.gameSetup;

import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import dan.dit.gameMemo.R;
import dan.dit.gameMemo.gameData.game.Game;
import dan.dit.gameMemo.gameData.game.GameKey;
import dan.dit.gameMemo.gameData.player.AbstractPlayerTeam;
import dan.dit.gameMemo.gameData.player.ChoosePlayerDialogFragment.ChoosePlayerDialogListener;
import dan.dit.gameMemo.gameData.player.DummyPlayer;
import dan.dit.gameMemo.gameData.player.Player;
import dan.dit.gameMemo.gameData.player.PlayerPool;
import dan.dit.gameMemo.storage.GameStorageHelper;
import dan.dit.gameMemo.util.ColorPickerView.OnColorChangedListener;
import dan.dit.gameMemo.util.NotifyMajorChangeCallback;

/**
 * This activity is used to select teams for a new Game and setup game options. Players
 * can be chosen from a pool of already existing players or newly created by entering a valid name.
 * If the user selected the necessary amount of players and wants to start the game, this activity finishes successfully
 * and returns a bundle in the EXTRA_TEAMS_PARAMETER containing player names, team names and colors selected by the user.
 * The order is defined by the minimum player amounts, not required
 * slots can have <code>null</code> players.
 * If the user wants to continue an unfinished game with the selected players, the id of this unfinished game
 * is returned in the intent data by the key <code>GameStorageHelper.getCursorItemType(GameKey)</code>.
 * The boolean and integer options are returned in the extras OPTIONS_BOOLEAN_VALUES and OPTIONS_NUMBER_VALUES.
 * @author Daniel
 *
 */
public class GameSetupActivity extends FragmentActivity implements ChoosePlayerDialogListener, OnColorChangedListener {
    public static final String EXTRA_TEAMS_PARAMETERS = "dan.dit.gameMemo.TEAMS_PARAMETERS"; // see TeamSetupTeamsController for more information
	public static final String EXTRA_OPTIONS_PARAMETERS = "dan.dit.gameMemo.OPTIONS_PARAMETERS"; // see TeamSetupOptionsController for more information
    public static final String EXTRA_FLAG_SUGGEST_UNFINISHED_GAME = "dan.dit.gameMemo.SUGGEST_UNFINISHED"; // boolean, default false
    private static final long SHUFFLE_PERIOD = 150; // in ms
	private static final long SHUFFLE_DURATION = 900; // in ms
	
	private int mGameKey;
	private TeamSetupTeamsController mTeamsController;
	private GameSetupOptionsController mOptionsController;
	private boolean mSuggestUnfinished;
	
	private ScrollView mScrollView;
	private LinearLayout mOptionsContainer;
	private Button mShowOptions;
	private Button mStartGame;
	private Timer mShufflePlayersTimer;
	private final Handler mTimerHandler = new Handler();

	private MenuItem mAddTeam;
	private MenuItem mShuffle;

	@SuppressLint("NewApi")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().requestFeature(Window.FEATURE_PROGRESS);
		mGameKey = getIntent().getExtras().getInt(GameKey.EXTRA_GAMEKEY);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			if (getActionBar() != null) {
				getActionBar().setDisplayShowTitleEnabled(false);
				getActionBar().setDisplayHomeAsUpEnabled(true);
				getActionBar().setIcon(GameKey.getGameIconId(mGameKey));
			}
		}
		setContentView(R.layout.game_setup);
		DummyPlayer.initNames(getResources());
		mScrollView = (ScrollView) findViewById(R.id.game_setup_mainscroll);
		mOptionsContainer = (LinearLayout) findViewById(R.id.options_container);
		mStartGame = (Button) findViewById(R.id.startGame);
		mShowOptions = (Button) findViewById(R.id.options_show);
		showOptions();
		mShowOptions.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				showOptions();
			}
			
		});
		// load flags and immutable parameters here, the rest after all creation is done
		mSuggestUnfinished = getIntent().getExtras().getBoolean(EXTRA_FLAG_SUGGEST_UNFINISHED_GAME);
		initListeners();
	}
	
	private void showOptions() {
		mShowOptions.setVisibility(View.GONE);
		mOptionsContainer.setVisibility(View.VISIBLE);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.game_setup, menu);
		mAddTeam = menu.findItem(R.id.add_team);
		mShuffle = menu.findItem(R.id.shuffle);
		applyButtonsState();
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.add_team:
			onNewTeamClicked();
			return true;
		case R.id.shuffle:				
			if (shuffleButtonCondition()) {
				startShuffle();
			}
			applyButtonsState();
			return true;
		case R.id.reset:
			resetFields();
			return true;
		case android.R.id.home:
			setResult(RESULT_CANCELED);
			finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		// done here since the TeamViews are being restored and overwrite/hide the actual team views added here
		if (savedInstanceState == null) {
			buildUIFromBundle(getIntent().getExtras());
		} else {
			buildUIFromBundle(savedInstanceState);
		}
		applyButtonsState();
		mScrollView.fullScroll(ScrollView.FOCUS_UP);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		applyButtonsState();
	}
	
	private void initListeners() {
		mStartGame.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (startGameButtonCondition()) {
					onGameStart();
				} else {
					applyButtonsState();
				}
			}
			
		});
	}
	
	private void resetFields() {
	    mTeamsController.resetFields();
	    mOptionsController.reset();
		applyButtonsState();		
		mScrollView.fullScroll(View.FOCUS_UP);
	}
	
	private void applyButtonsState() {
		mStartGame.setEnabled(startGameButtonCondition());
		if (hasDummy()) {
		    mStartGame.setCompoundDrawablesWithIntrinsicBounds(GameKey.getGameIconId(mGameKey), 0, R.drawable.warning_icon, 0);
		} else {
            mStartGame.setCompoundDrawablesWithIntrinsicBounds(GameKey.getGameIconId(mGameKey), 0, 0, 0);
		}
		if (mShuffle != null) {
			mShuffle.setEnabled(shuffleButtonCondition());
		}
		if (mAddTeam != null && mTeamsController != null) {
			boolean enableNewTeam = mTeamsController.hasMissingTeam();
			mAddTeam.setEnabled(enableNewTeam);
			mAddTeam.setVisible(enableNewTeam);
		}
	}
	
	private boolean hasDummy() {
	    if (mTeamsController != null) {
	        List<Player> players = mTeamsController.getAllPlayers(true);
	        for (Player p : players) {
	            if (p instanceof DummyPlayer) {
	                return true;
	            }
	        }
	    }
	    return false;
	}
	
	private boolean startGameButtonCondition() {
		return mTeamsController == null ? false : mTeamsController.hasRequiredPlayers();
	}
	
	private void onGameStart() {
		if (!mSuggestUnfinished || !searchAndSuggestUnfinished()) {
			startNewGame(); // either not suggesting or did not find an unfinished game, start new one
		}
	}
	
	private boolean searchAndSuggestUnfinished() {
		List<Player> team = mTeamsController.getAllPlayers(false);
		if (team.size() == 0) {
			return false;
		}
		// if dummys are being used then look for a subset, else an exact match
		final long unfinishedId = team.size() > 0 ? Game.getUnfinishedGame(mGameKey, getContentResolver(), team, false) : Game.NO_ID;
		if (Game.isValidId(unfinishedId)) {
			 // there is an unfinished game with exactly the selected players, ask if they want to continue this game
			new AlertDialog.Builder(this)
 			.setTitle(getResources().getString(R.string.unfinished_game_found_title))
 			.setIcon(android.R.drawable.ic_dialog_alert)
 			.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

 			    public void onClick(DialogInterface dialog, int whichButton) {
 					continueUnfinishedGame(unfinishedId);
 			    }})
 			 .setNegativeButton(getResources().getString(R.string.unfinished_game_found_but_start_new), new DialogInterface.OnClickListener() {

  			    public void onClick(DialogInterface dialog, int whichButton) {
 					startNewGame();
  			    }})
  			  .show();
			return true;
		}
		return false;
	}
	
	private void continueUnfinishedGame(long unfinishedGameId) {
		Intent returnIntent = new Intent();
		returnIntent.putExtra(GameStorageHelper.getCursorItemType(mGameKey), unfinishedGameId);
		setResult(RESULT_OK, returnIntent);
		finish();
	}
	
	private void startNewGame() {
		Intent returnIntent = new Intent();
		PlayerPool pool = getPool();
		// for all used dummys we need to populate a real player (with the same color) since dummys are only used by the setup activity
		for (Player p : mTeamsController.getAllPlayers(true)) {
			if (p instanceof DummyPlayer) {
				Player dedummiedPlayer = pool.populatePlayer(p.getName());
				dedummiedPlayer.setColor(p.getColor());
			}
		}
		for (AbstractPlayerTeam team : mTeamsController.getAllTeams(false)) {
		    if (team.getPlayerCount() > 0) {
		        team.saveColor(this);
		    }
		}
		returnIntent.putExtra(EXTRA_TEAMS_PARAMETERS, mTeamsController.getParameters());
		returnIntent.putExtra(EXTRA_OPTIONS_PARAMETERS, mOptionsController.getParameters());
		setResult(RESULT_OK, returnIntent);
		finish();
	}
	
	private boolean shuffleButtonCondition() {
		return mShufflePlayersTimer == null;
	}
	
	private void startShuffle() {
		if (mShufflePlayersTimer == null) {
			mShufflePlayersTimer = new Timer("ShufflePlayersTimer",true);
			applyButtonsState();
			mShufflePlayersTimer.scheduleAtFixedRate(new TimerTask() {
				private int shuffleCount = 0;
				@Override
				public void run() {
					mTimerHandler.post(new Runnable() {

						@Override
						public void run() {
							performShuffle(shuffleCount * SHUFFLE_PERIOD);
						}
						
					});
					shuffleCount++;
					if (shuffleCount * SHUFFLE_PERIOD >= SHUFFLE_DURATION) {
						mTimerHandler.post(new Runnable() {
							@Override
							public void run() {
								stopShuffle();
							}
						});
					}
				}
				
			}, 0, SHUFFLE_PERIOD);
		}
	}
	
	private void stopShuffle() {
		if (mShufflePlayersTimer != null) {
			mShufflePlayersTimer.cancel();
			mShufflePlayersTimer = null;
			applyButtonsState();
		}
	}
	
	private void performShuffle(long timeShuffledByNow) {
		if (timeShuffledByNow <= SHUFFLE_DURATION) {
			getWindow().setFeatureInt(Window.FEATURE_PROGRESS, 
					(int) ((double) 10000 * timeShuffledByNow / SHUFFLE_DURATION));
		} else {
			getWindow().setFeatureInt(Window.FEATURE_PROGRESS, 10000);
		}
		mTeamsController.performShuffle();
	}
	
	@Override
	public void onPause() {
		super.onPause();
		stopShuffle();
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(GameKey.EXTRA_GAMEKEY, mGameKey);
		if (mOptionsController != null) {
		    outState.putBundle(EXTRA_OPTIONS_PARAMETERS, mOptionsController.getParameters());
		}
		if (mTeamsController != null) {
		    outState.putBundle(EXTRA_TEAMS_PARAMETERS, mTeamsController.getParameters());
		}
	}
	
	private void buildUIFromBundle(Bundle in) {
		initTitle();
		initTeamsUI(in.getBundle(EXTRA_TEAMS_PARAMETERS));
		initOptionsUI(in.getBundle(EXTRA_OPTIONS_PARAMETERS));
		applyBackgroundTheme();
	}
	
	private void applyBackgroundTheme() {
	    applyTheme(mStartGame);
	    applyTheme(mShowOptions);
	    mTeamsController.applyTheme();
	}
	
	private void applyTheme(View view) {
        GameKey.applyTheme(mGameKey, getResources(), view);	    
	}
	
	private void initTitle() {
		mStartGame.setCompoundDrawablesWithIntrinsicBounds(GameKey.getGameIconId(mGameKey), 0, 0, 0);
		mStartGame.setText(getResources().getString(R.string.game_setup_start, GameKey.getGameName(mGameKey)));
	}
	
	private void initTeamsUI(Bundle parameters) {
	    NotifyMajorChangeCallback cb = new NotifyMajorChangeCallback() {
            
            @Override
            public void onMajorChange() {
                applyButtonsState();
            }
        };
	    mTeamsController = new TeamSetupTeamsController(mGameKey, this, cb, parameters, (LinearLayout) findViewById(R.id.teams_container));
		applyButtonsState();
	}
	
	private void onNewTeamClicked() {
		if (mTeamsController.hasMissingTeam()) {
			mTeamsController.addMissingTeam();
		}
		applyButtonsState();
	}
	
	private void initOptionsUI(Bundle args) {
	    mOptionsController = GameKey.makeGameSetupOptionsController(mGameKey, this, mOptionsContainer, args);
	}

	@Override
	public PlayerPool getPool() {
		return GameKey.getPool(mGameKey);
	}

	@Override
	public List<Player> toFilter() {
		if (mTeamsController != null) {
		    return mTeamsController.toFilter();
		} else {
		    return Collections.emptyList();
		}
	}

	@Override
	public void onColorChanged(int color) {
		mTeamsController.onColorChanged(color);
	}

    @Override
    public void playerChosen(int arg, Player chosen) {
        mTeamsController.playerChosen(arg, chosen);
    }

    @Override
    public void onPlayerColorChanged(int arg, Player concernedPlayer) {
        mTeamsController.onPlayerColorChanged(arg, concernedPlayer);
        concernedPlayer.saveColor(this);
    }

}
