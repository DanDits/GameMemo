package dan.dit.gameMemo.appCore;

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
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import dan.dit.gameMemo.R;
import dan.dit.gameMemo.gameData.game.Game;
import dan.dit.gameMemo.gameData.game.GameKey;
import dan.dit.gameMemo.gameData.player.ChoosePlayerDialogFragment.ChoosePlayerDialogListener;
import dan.dit.gameMemo.gameData.player.DummyPlayer;
import dan.dit.gameMemo.gameData.player.Player;
import dan.dit.gameMemo.gameData.player.PlayerPool;
import dan.dit.gameMemo.gameData.player.TeamSetupTeamsController;
import dan.dit.gameMemo.storage.GameStorageHelper;
import dan.dit.gameMemo.util.ColorPickerView.OnColorChangedListener;
import dan.dit.gameMemo.util.NotifyMajorChangeCallback;

/**
 * This activity is used to select teams for a new Game and setup game options. Players
 * can be chosen from a pool of already existing players or newly created by entering a valid name.
 * If the user selected the necessary amount of players and wants to start the game, this activity finishes successfully
 * and returns the player names in the EXTRA_PLAYER_NAMES in the order defined by the minimum player amounts, not required
 * slots can have <code>null</code> players.
 * If the user wants to continue an unfinished game with the selected players, the id of this unfinished game
 * is returned in the intent data by the key <code>GameStorageHelper.getCursorItemType(GameKey)</code>.
 * The boolean and integer options are returned in the extras OPTIONS_BOOLEAN_VALUES and OPTIONS_NUMBER_VALUES.
 * @author Daniel
 *
 */
public class GameSetupActivity extends FragmentActivity implements ChoosePlayerDialogListener, OnColorChangedListener {
    public static final String EXTRA_TEAMS_PARAMETERS = "dan.dit.gameMemo.TEAMS_PARAMETERS";
	public static final String EXTRA_FLAG_SUGGEST_UNFINISHED_GAME = "dan.dit.gameMemo.SUGGEST_UNFINISHED"; // boolean, default false
	public static final String EXTRA_OPTIONS_BOOLEAN_VALUES = "dan.dit.gameMemo.EXTRA_OPTIONS_BOOLEAN_VALUES"; // optional boolean[]
	public static final String EXTRA_OPTIONS_BOOLEAN_NAMES = "dan.dit.gameMemo.EXTRA_OPTIONS_BOOLEAN_NAMES"; // optional String[], comes with OPTIONS_BOOLEAN_VALUES
	public static final String EXTRA_OPTIONS_NUMBER_VALUES = "dan.dit.gameMemo.EXTRA_OPTIONS_INT_VALUES"; // optinal int[]
	public static final String EXTRA_OPTIONS_NUMBER_NAMES = "dan.dit.gameMemo.EXTRA_OPTIONS_INT_NAMES"; // optional String[], comes with OPTIONS_INT_VALUES
	public static final String EXTRA_OPTIONS_NUMBER_MIN_VALUES = "dan.dit.gameMemo.EXTRA_OPTIONS_NUMBER_MIN_VALUES"; // optional int[], comes with OPTIONS_INT_VALUES
	public static final String EXTRA_OPTIONS_NUMBER_MAX_VALUES = "dan.dit.gameMemo.EXTRA_OPTIONS_NUMBER_MAX_VALUES"; // optional int[], comes with OPTIONS_INT_VALUES
	private static final String STORAGE_OPTIONS_NUMBER_VALUES_DEFAULT = "STORAGE_OPTIONS_INT_VALUES_DEFAULT"; // int[] for current values of OPTIONS_INT_VALUES
	private static final String STORAGE_OPTIONS_BOOLEAN_VALUES_DEFAULT = "STORAGE_OPTIONS_BOOLEAN_VALUES_DEFAULT"; // boolean[] for current values of OPTIONS_BOOLEAN_VALUES
	private static final long SHUFFLE_PERIOD = 150; // in ms
	protected static final long SHUFFLE_DURATION = 900; // in ms
	
	private int mGameKey;
	private TeamSetupTeamsController mController;
	private boolean mSuggestUnfinished;

	private boolean[] mOptionsBoolean;
	private boolean[] mOptionsBooleanDefault;
	private CheckBox[] mOptionBooleanCheckers;
	private String[] mOptionsBooleanNames;
	private EditText[] mOptionNumberFields;
	private int[] mOptionsNumbers;
	private int[] mOptionsNumbersDefault;
	private String[] mOptionsNumberNames;
	private int[] mOptionsNumberMaxValues;
	private int[] mOptionsNumberMinValues;
	
	private ScrollView mScrollView;
	private LinearLayout mOptionsContainer;
	private Button mShowOptions;
	private Button mStartGame;
	private Timer mShufflePlayersTimer;
	private final Handler mTimerHandler = new Handler();

	private MenuItem mAddTeam;
	private MenuItem mShuffle;

	//TODO make a builder class for all the extras and then hide them, make an option to hide options container by default and show options
	//TODO let each game have a controller and a view for options
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
	    mController.resetFields();
		if (mOptionBooleanCheckers != null) {
			for (int i = 0; i < mOptionBooleanCheckers.length; i++) {
				mOptionBooleanCheckers[i].setChecked(mOptionsBooleanDefault[i]);
			}
		}
		if (mOptionNumberFields != null) {
			for (int i = 0; i < mOptionNumberFields.length; i++) {
				mOptionNumberFields[i].setText(String.valueOf(mOptionsNumbersDefault[i]));
			}
		}
		applyButtonsState();		
		mScrollView.fullScroll(View.FOCUS_UP);
	}
	
	private void applyButtonsState() {
		mStartGame.setEnabled(startGameButtonCondition());
		if (mShuffle != null) {
			mShuffle.setEnabled(shuffleButtonCondition());
		}
		if (mAddTeam != null && mController != null) {
			boolean enableNewTeam = mController.hasMissingTeam();
			mAddTeam.setEnabled(enableNewTeam);
			mAddTeam.setVisible(enableNewTeam);
		}
	}
	
	private boolean startGameButtonCondition() {
		return mController == null ? false : mController.hasRequiredPlayers();
	}
	
	private void onGameStart() {
		if (!mSuggestUnfinished || !searchAndSuggestUnfinished()) {
			startNewGame(); // either not suggesting or did not find an unfinished game, start new one
		}
	}
	
	private boolean searchAndSuggestUnfinished() {
		List<Player> team = mController.getAllPlayers(false);
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
		for (Player p : mController.getAllPlayers(true)) {
			if (p instanceof DummyPlayer) {
				Player dedummiedPlayer = pool.populatePlayer(p.getName());
				dedummiedPlayer.setColor(p.getColor());
			}
		}
		returnIntent.putExtra(EXTRA_TEAMS_PARAMETERS, mController.getParameters());
		returnIntent.putExtra(EXTRA_OPTIONS_NUMBER_VALUES, mOptionsNumbers);
		returnIntent.putExtra(EXTRA_OPTIONS_BOOLEAN_VALUES, mOptionsBoolean);
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
		mController.performShuffle();
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
		outState.putIntArray(EXTRA_OPTIONS_NUMBER_MAX_VALUES, mOptionsNumberMaxValues);
		outState.putIntArray(EXTRA_OPTIONS_NUMBER_MIN_VALUES, mOptionsNumberMinValues);
		outState.putIntArray(EXTRA_OPTIONS_NUMBER_VALUES, mOptionsNumbers);
		outState.putBooleanArray(EXTRA_OPTIONS_BOOLEAN_VALUES, mOptionsBoolean);
		outState.putBooleanArray(STORAGE_OPTIONS_BOOLEAN_VALUES_DEFAULT, mOptionsBooleanDefault);
		outState.putStringArray(EXTRA_OPTIONS_NUMBER_NAMES, mOptionsNumberNames);
		outState.putStringArray(EXTRA_OPTIONS_BOOLEAN_NAMES, mOptionsBooleanNames);
		outState.putIntArray(STORAGE_OPTIONS_NUMBER_VALUES_DEFAULT, mOptionsNumbersDefault);
		if (mController != null) {
		    outState.putBundle(EXTRA_TEAMS_PARAMETERS, mController.getParameters());
		}
	}
	
	private void buildUIFromBundle(Bundle in) {
		initTitle();
		initTeamsUI(in.getBundle(EXTRA_TEAMS_PARAMETERS));
		initOptionsUI(in);
		applyBackgroundTheme();
	}
	
	private void applyBackgroundTheme() {
	    applyTheme(mStartGame);
	    applyTheme(mShowOptions);
	    mController.applyTheme();
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
	    mController = new TeamSetupTeamsController(mGameKey, this, cb, parameters, (LinearLayout) findViewById(R.id.teams_container));
		applyButtonsState();
	}
	
	private void onNewTeamClicked() {
		if (mController.hasMissingTeam()) {
			mController.addMissingTeam();
		}
		applyButtonsState();
	}
	
	private void initOptionsUI(Bundle args) {
		mOptionsBooleanDefault = args.getBooleanArray(STORAGE_OPTIONS_BOOLEAN_VALUES_DEFAULT);
		mOptionsBoolean = args.getBooleanArray(EXTRA_OPTIONS_BOOLEAN_VALUES);
		mOptionsBooleanNames = args.getStringArray(EXTRA_OPTIONS_BOOLEAN_NAMES);
		mOptionsNumbersDefault = args.getIntArray(STORAGE_OPTIONS_NUMBER_VALUES_DEFAULT);
		mOptionsNumbers = args.getIntArray(EXTRA_OPTIONS_NUMBER_VALUES);
		mOptionsNumberNames = args.getStringArray(EXTRA_OPTIONS_NUMBER_NAMES);
		mOptionsNumberMinValues = args.getIntArray(EXTRA_OPTIONS_NUMBER_MIN_VALUES);
		mOptionsNumberMaxValues = args.getIntArray(EXTRA_OPTIONS_NUMBER_MAX_VALUES);
		mOptionsContainer.removeAllViews();
		boolean atLeastOneOptionAdded = false;
		if (mOptionsBoolean != null && mOptionsBooleanNames != null) {
			if (mOptionsBooleanDefault == null) {
				mOptionsBooleanDefault = new boolean[mOptionsBoolean.length];
				System.arraycopy(mOptionsBoolean, 0, mOptionsBooleanDefault, 0, mOptionsBoolean.length);
			}
			int boolOptions = Math.min(mOptionsBoolean.length, mOptionsBooleanNames.length);
			mOptionBooleanCheckers = new CheckBox[boolOptions];
			for (int i = 0; i < boolOptions; i++) {
				atLeastOneOptionAdded = true;
				CheckBox checker = new CheckBox(this);
				final int index = i;
				checker.setOnCheckedChangeListener(new OnCheckedChangeListener() {
					
					@Override
					public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
						mOptionsBoolean[index] = isChecked;
					}
				});
				checker.setChecked(mOptionsBoolean[i]);
				checker.setText(mOptionsBooleanNames[i]);
				mOptionsContainer.addView(checker);
				mOptionBooleanCheckers[i] = checker;
			}
		}
		if (mOptionsNumbers != null && mOptionsNumberNames != null && mOptionsNumberMinValues != null & mOptionsNumberMaxValues != null) {
			if (mOptionsNumbersDefault == null) {
				mOptionsNumbersDefault = new int[mOptionsNumbers.length];
				System.arraycopy(mOptionsNumbers, 0, mOptionsNumbersDefault, 0, mOptionsNumbers.length);
			}
			int numberOptionsCount = Math.min(mOptionsNumbers.length, Math.min(mOptionsNumberNames.length, Math.min(mOptionsNumberMinValues.length, mOptionsNumberMaxValues.length)));
			mOptionNumberFields = new EditText[numberOptionsCount];
			for (int i = 0; i < numberOptionsCount; i++) {
				atLeastOneOptionAdded = true;
				View numberOptions = getLayoutInflater().inflate(R.layout.game_options_number, null);
				TextView description = ((TextView) numberOptions.findViewById(R.id.description));
				description.setText(mOptionsNumberNames[i]);
				View numberInputRaw = numberOptions.findViewById(R.id.number_input);
				mOptionsNumberMinValues[i] = Math.min(mOptionsNumberMinValues[i], mOptionsNumbers[i]);
				mOptionsNumberMaxValues[i] = Math.max(mOptionsNumbers[i], mOptionsNumberMaxValues[i]);
				final int index = i;
				final TextView currValue = (TextView) numberOptions.findViewById(R.id.curr_number);
				EditText numberInput = (EditText) numberInputRaw;
				mOptionNumberFields[i] = numberInput;
				if (mOptionsNumberMinValues[i] >= 0) {
					numberInput.setInputType(InputType.TYPE_CLASS_NUMBER);
				} else {
					numberInput.setInputType(InputType.TYPE_NUMBER_FLAG_SIGNED);
				}
				numberInput.addTextChangedListener(new TextWatcher() {

					@Override
					public void afterTextChanged(Editable e) {
						int number = 0;
						try {
							number = Integer.parseInt(e.toString());
						} catch (NumberFormatException nfe) {
							return;
						}
						if (number >= mOptionsNumberMinValues[index] && number <= mOptionsNumberMaxValues[index]) {
							mOptionsNumbers[index] = number;
							currValue.setText("= " + mOptionsNumbers[index]);
						}
					}

					@Override
					public void beforeTextChanged(CharSequence arg0,
							int arg1, int arg2, int arg3) {}

					@Override
					public void onTextChanged(CharSequence arg0, int arg1,
							int arg2, int arg3) {}
					
				});
				numberInput.setText(String.valueOf(mOptionsNumbers[i]));
				mOptionsContainer.addView(numberOptions);
			}
		}
		if (atLeastOneOptionAdded) {
			TextView optionsTitle = new TextView(this);
			optionsTitle.setText(getResources().getString(R.string.game_setup_options));
			mOptionsContainer.addView(optionsTitle, 0);
		}
	}

	@Override
	public PlayerPool getPool() {
		return GameKey.getPool(mGameKey);
	}

	@Override
	public List<Player> toFilter() {
		if (mController != null) {
		    return mController.toFilter();
		} else {
		    return Collections.emptyList();
		}
	}

	@Override
	public void onColorChanged(int color) {
		mController.onColorChanged(color);
	}

    @Override
    public void playerChosen(int arg, Player chosen) {
        mController.playerChosen(arg, chosen);
    }

    @Override
    public void onPlayerColorChanged(int arg, Player concernedPlayer) {
        mController.onPlayerColorChanged(arg, concernedPlayer);
    }

}
