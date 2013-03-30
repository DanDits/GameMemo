package dan.dit.gameMemo.appCore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import dan.dit.gameMemo.R;
import dan.dit.gameMemo.gameData.game.Game;
import dan.dit.gameMemo.gameData.game.GameKey;
import dan.dit.gameMemo.gameData.player.ChoosePlayerDialogFragment;
import dan.dit.gameMemo.gameData.player.ChoosePlayerDialogFragment.ChoosePlayerDialogListener;
import dan.dit.gameMemo.gameData.player.Player;
import dan.dit.gameMemo.gameData.player.PlayerPool;
import dan.dit.gameMemo.storage.GameStorageHelper;
import dan.dit.gameMemo.util.ShowStacktraceUncaughtExceptionHandler;

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
public class GameSetupActivity extends FragmentActivity implements ChoosePlayerDialogListener {
	public static final String EXTRA_FLAG_SUGGEST_UNFINISHED_GAME = "dan.dit.gameMemo.SUGGEST_UNFINISHED"; // default false
	public static final String EXTRA_TEAM_NAMES = "dan.dit.gameMemo.EXTRA_TEAM_NAMES"; // if not given default to 'Team x'
	public static final String EXTRA_TEAM_MIN_PLAYERS = "dan.dit.gameMemo.EXTRA_TEAM_MIN_PLAYERS"; // negative values will be interpreted as 0
	public static final String EXTRA_TEAM_MAX_PLAYERS = "dan.dit.gameMemo.EXTRA_TEAM_MAX_PLAYERS"; // if not given default 10, must be > 0 each
	public static final String EXTRA_PLAYER_NAMES = "dan.dit.gameMemo.EXTRA_PLAYER_NAMES"; // not required, to give suggestions for the slots, if result = ok these contains names, null for not required players of a team
	public static final String EXTRA_OPTIONS_BOOLEAN_VALUES = "dan.dit.gameMemo.EXTRA_OPTIONS_BOOLEAN_VALUES";
	public static final String EXTRA_OPTIONS_BOOLEAN_NAMES = "dan.dit.gameMemo.EXTRA_OPTIONS_BOOLEAN_NAMES";
	public static final String EXTRA_OPTIONS_NUMBER_VALUES = "dan.dit.gameMemo.EXTRA_OPTIONS_INT_VALUES";
	public static final String EXTRA_OPTIONS_NUMBER_NAMES = "dan.dit.gameMemo.EXTRA_OPTIONS_INT_NAMES";
	public static final String EXTRA_OPTIONS_NUMBER_MIN_VALUES = "dan.dit.gameMemo.EXTRA_OPTIONS_NUMBER_MIN_VALUES";
	public static final String EXTRA_OPTIONS_NUMBER_MAX_VALUES = "dan.dit.gameMemo.EXTRA_OPTIONS_NUMBER_MAX_VALUES";
	private static final String STORAGE_OPTIONS_NUMBER_VALUES_DEFAULT = "STORAGE_OPTIONS_INT_VALUES_DEFAULT";
	private static final String STORAGE_OPTIONS_BOOLEAN_VALUES_DEFAULT = "STORAGE_OPTIONS_BOOLEAN_VALUES_DEFAULT";
	
	private static final long SHUFFLE_PERIOD = 200; // in ms
	protected static final long SHUFFLE_DURATION = 2000; // in ms
	private static final int DEFAULT_MAX_TEAM_SIZE = 10;
	private int mGameKey;
	private boolean mSuggestUnfinished;
	private Player[] players;
	private int[] mMinTeamSizes;
	private int[] mMaxTeamSizes;
	private String[] mTeamNames;
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
	
	private LinearLayout mOptionsContainer;
	private LinearLayout mTeamsContainer;
	private Button mStartGame;
	private int mChoosingPlayerSlot;
	private Button[] mPlayerButtons;
	private Button mShuffle;
	private Button mClear;
	private ProgressBar mShuffleProgress;
	private Timer mShufflePlayersTimer;
	private final Handler mTimerHandler = new Handler();
	
	@SuppressLint("NewApi")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.game_setup);
		Thread.setDefaultUncaughtExceptionHandler(new ShowStacktraceUncaughtExceptionHandler(this));
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			ActionBar bar = getActionBar();
			if (bar != null) {
				bar.hide();
			}
		}
		mShuffle = (Button) findViewById(R.id.shuffle);
		mTeamsContainer = (LinearLayout) findViewById(R.id.teams_container);
		mOptionsContainer = (LinearLayout) findViewById(R.id.options_container);
		mShuffleProgress = (ProgressBar) findViewById(R.id.progressBar);
		mStartGame = (Button) findViewById(R.id.startGame);
		mClear = (Button) findViewById(R.id.clear);
		Player[] temp = null;
		mSuggestUnfinished = getIntent().getExtras().getBoolean(EXTRA_FLAG_SUGGEST_UNFINISHED_GAME);
		if (savedInstanceState == null) {
			buildUIFromBundle(getIntent().getExtras());
			temp = new Player[players.length];
			loadPlayersFromBundle(GameKey.getPool(mGameKey), temp, getIntent().getExtras());
		} else {
			buildUIFromBundle(savedInstanceState);
			temp = new Player[players.length];
			loadPlayersFromBundle(GameKey.getPool(mGameKey), temp, savedInstanceState);
		}
		for (int index = 0; index < players.length; index++) {
			setPlayer(index, temp[index]);
		}
		initListeners();
		applyButtonsState();
	}
	
	private void initListeners() {
		mClear.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				clearFields();
			}
			
		});
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
		mShuffle.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (shuffleButtonCondition()) {
					startShuffle();
				}
				applyButtonsState();
			}
			
		});
		
	}
	
	private void clearFields() {
		for (int i = 0; i < players.length; i++) {
			clearPlayer(i);					
		}
		for (int i = 0; i < mOptionBooleanCheckers.length; i++) {
			mOptionBooleanCheckers[i].setChecked(mOptionsBooleanDefault[i]);
		}
		for (int i = 0; i < mOptionNumberFields.length; i++) {
			mOptionNumberFields[i].setText(String.valueOf(mOptionsNumbersDefault[i]));
		}
		mShuffleProgress.setProgress(0);
		applyButtonsState();
	}
	
	private void applyButtonsState() {
		mStartGame.setEnabled(startGameButtonCondition());
		mShuffle.setEnabled(shuffleButtonCondition());
	}
	
	private boolean startGameButtonCondition() {
		return hasAllPlayers();
	}
	
	private void onGameStart() {
		if (!mSuggestUnfinished || !searchAndSuggestUnfinished()) {
			startNewGame(); // either not suggesting or did not find an unfinished game, start new one
		}
	}
	
	private boolean searchAndSuggestUnfinished() {
		List<Player> team = new ArrayList<Player>(players.length);
		for (int i = 0; i < players.length; i++) {
			team.add(players[i]);
		}
		final long unfinishedId = Game.getUnfinishedGame(mGameKey, getContentResolver(), team);
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
		savePlayerToIntent(returnIntent);
		returnIntent.putExtra(EXTRA_OPTIONS_NUMBER_VALUES, mOptionsNumbers);
		returnIntent.putExtra(EXTRA_OPTIONS_BOOLEAN_VALUES, mOptionsBoolean);
		setResult(RESULT_OK, returnIntent);
		finish();
	}
	
	private boolean shuffleButtonCondition() {
		return getPlayerCount() > 0 && mShufflePlayersTimer == null;
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
		if (mShuffleProgress.getMax() != SHUFFLE_DURATION) {
			mShuffleProgress.setMax((int) SHUFFLE_DURATION);
		}
		if (timeShuffledByNow <= SHUFFLE_DURATION) {
			mShuffleProgress.setProgress((int) timeShuffledByNow);
		} else {
			mShuffleProgress.setProgress((int) SHUFFLE_DURATION);
		}
		List<Integer> permutation = getPermutation(players.length);
		Player[] newPlayers = new Player[players.length];
		for (int i = 0; i < players.length; i++) {
			newPlayers[permutation.get(i)] = players[i];
			clearPlayer(i);
		}
		for (int i = 0; i < players.length; i++) {
			setPlayer(i, newPlayers[i]);
		}
	}
	
	private List<Integer> getPermutation(int size) {
		List<Integer> perm = new ArrayList<Integer>(size);
		for (int i = 0; i < size; i++) {
			perm.add(i);
		}
		Collections.shuffle(perm, new Random());
		return perm;
	}
	
	private int getPlayerCount() {
		int count = 0;
		for (int i = 0; i < players.length; i++) {
			if (players[i] != null) {
				count++;
			}
		}
		return count;
	}
	
	private boolean hasAllPlayers() {
		int playerIndex = 0;
		for (int teamIndex = 0 ; teamIndex < mMinTeamSizes.length; teamIndex++) {
			int validPlayersForTeam = 0;
			for (int i = 0; i < mMaxTeamSizes[teamIndex]; i++) {
				if (players[playerIndex] != null) {
					validPlayersForTeam++;
				}
				playerIndex++;
			}
			if (validPlayersForTeam < mMinTeamSizes[teamIndex]) {
				return false;
			}
		}
		return true;
	}
	
	private void clearPlayer(int index) {
		mPlayerButtons[index].setText(getResources().getString(R.string.game_setup_select_player));
		players[index] = null;
	}
	
	private boolean setPlayer(int index, Player player) {
		if (player != null && !isPlayerActive(player)) {
			clearPlayer(index);
			players[index] = player;
			mPlayerButtons[index].setText(player.getName());
			return true;
		}
		return false;
	}
	
	private boolean isPlayerActive(Player player) {
		if (player != null) {
			for (int i = 0; i < players.length; i++) {
				if (player.equals(players[i])) {
					return true;
				}
			}
		}
		// a null-player is no player and therefore not active
		return false;
	}
	
	@Override
	public void onPause() {
		super.onPause();
		stopShuffle();
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		savePlayerToBundle(outState);
		outState.putIntArray(EXTRA_TEAM_MIN_PLAYERS, mMinTeamSizes);
		outState.putIntArray(EXTRA_TEAM_MAX_PLAYERS, mMaxTeamSizes);
		outState.putStringArray(EXTRA_TEAM_NAMES, mTeamNames);
		outState.putInt(GameKey.EXTRA_GAMEKEY, mGameKey);
		outState.putIntArray(EXTRA_OPTIONS_NUMBER_MAX_VALUES, mOptionsNumberMaxValues);
		outState.putIntArray(EXTRA_OPTIONS_NUMBER_MIN_VALUES, mOptionsNumberMinValues);
		outState.putIntArray(EXTRA_OPTIONS_NUMBER_VALUES, mOptionsNumbers);
		outState.putBooleanArray(EXTRA_OPTIONS_BOOLEAN_VALUES, mOptionsBoolean);
		outState.putBooleanArray(STORAGE_OPTIONS_BOOLEAN_VALUES_DEFAULT, mOptionsBooleanDefault);
		outState.putStringArray(EXTRA_OPTIONS_NUMBER_NAMES, mOptionsNumberNames);
		outState.putStringArray(EXTRA_OPTIONS_BOOLEAN_NAMES, mOptionsBooleanNames);
		outState.putIntArray(STORAGE_OPTIONS_NUMBER_VALUES_DEFAULT, mOptionsNumbersDefault);
	}
	
	private void savePlayerToBundle(Bundle out) {
		if (out == null || getPlayerCount() == 0) {
			return;
		}
		String[] playerNames = new String[players.length];
		for (int i = 0; i < playerNames.length; i++) {
			playerNames[i] = players[i] != null ? players[i].getName() : null;
		}
		out.putStringArray(EXTRA_PLAYER_NAMES, playerNames);
	}
	
	private void savePlayerToIntent(Intent out) {
		if (out == null || getPlayerCount() == 0) {
			return;
		}
		String[] playerNames = new String[players.length];
		for (int i = 0; i < playerNames.length; i++) {
			playerNames[i] = players[i] != null ? players[i].getName() : null;
		}
		out.putExtra(EXTRA_PLAYER_NAMES, playerNames);
	}
	
	private static void loadPlayersFromBundle(PlayerPool pool, Player[] players, Bundle in) {
		if (in == null) {
			return;
		}
		String[] playerNames = in.getStringArray(EXTRA_PLAYER_NAMES);
		if (playerNames != null) {
			for (int i = 0; i < playerNames.length; i++) {
				players[i] = !Player.isValidPlayerName(playerNames[i]) ? null : pool.populatePlayer(playerNames[i]);
			}
		}
	}
	
	private void buildUIFromBundle(Bundle in) {
		mGameKey = in.getInt(GameKey.EXTRA_GAMEKEY);
		int[] minTeamSize = in.getIntArray(EXTRA_TEAM_MIN_PLAYERS);
		int[] maxTeamSize = in.getIntArray(EXTRA_TEAM_MAX_PLAYERS);
		String[] teamNames = in.getStringArray(EXTRA_TEAM_NAMES);
		if (minTeamSize == null || minTeamSize.length == 0) {
			setResult(RESULT_CANCELED);
			Log.e(getClass().getName(), "Need at least one team minimum size.");
			finish();
			return;
		}
		String defaultTeamName = getResources().getString(R.string.game_team) + ' ';
		if (teamNames == null) {
			teamNames = new String[minTeamSize.length];
		} else if (teamNames.length < minTeamSize.length) {
			String[] temp = teamNames;
			teamNames = new String[minTeamSize.length];
			System.arraycopy(temp, 0, teamNames, 0, temp.length);
		}
		int teamNameIndex = 1;
		for (int i = 0; i < teamNames.length; i++) {
			if (teamNames[i] == null) {
				teamNames[i] = defaultTeamName + teamNameIndex;
				teamNameIndex++;
			}
		}
		for (int i = 0; i < minTeamSize.length; i++) {
			minTeamSize[i] = Math.max(0, minTeamSize[i]);
		}
		if (maxTeamSize == null) {
			maxTeamSize = new int[minTeamSize.length];
			Arrays.fill(maxTeamSize, DEFAULT_MAX_TEAM_SIZE);
		} else if (maxTeamSize.length < minTeamSize.length) {
			int[] temp = maxTeamSize;
			maxTeamSize = new int[minTeamSize.length];
			Arrays.fill(maxTeamSize, DEFAULT_MAX_TEAM_SIZE);
			System.arraycopy(temp, 0, maxTeamSize, 0, temp.length);
		}
		int maxPlayerCount = 0;
		for (int i = 0; i < maxTeamSize.length; i++) {
			maxTeamSize[i] = Math.max(minTeamSize[i], maxTeamSize[i]); //so max >= min
			maxPlayerCount += maxTeamSize[i];
		}
		mPlayerButtons = new Button[maxPlayerCount];
		players = new Player[maxPlayerCount];
		mMaxTeamSizes = maxTeamSize;
		mMinTeamSizes = minTeamSize;
		mTeamNames = teamNames;
		initTitle();
		initTeamsUI();
		initOptionsUI(in);
		applyBackgroundTheme();
	}
	
	private void applyBackgroundTheme() {
		int btnResId = GameKey.getButtonResource(mGameKey);
		mShuffle.setBackgroundResource(btnResId);
		mStartGame.setBackgroundResource(btnResId);
		mClear.setBackgroundResource(btnResId);
		for (Button p : mPlayerButtons) {
			p.setBackgroundResource(btnResId);
		}
	}
	
	private void initTitle() {
		mStartGame.setCompoundDrawablesWithIntrinsicBounds(GameKey.getGameIconId(mGameKey), 0, 0, 0);
		mStartGame.setText(getResources().getString(R.string.game_setup_start, GameKey.getGameName(mGameKey)));
	}
	
	private void initTeamsUI() {
		mTeamsContainer.removeAllViews();
		int teamsCount = Math.min(mMinTeamSizes.length, mMaxTeamSizes.length);
		OnClickListener listener = new OnClickListener() {

			@Override
			public void onClick(View v) {
				for (int i = 0; i < players.length; i++) {
					if (v == mPlayerButtons[i]) {
						mChoosingPlayerSlot = i;
				        DialogFragment dialog = new ChoosePlayerDialogFragment();
				        dialog.show(getSupportFragmentManager(), "ChoosePlayerDialogFragment");
						break;
					}
				}
			}
			
		};
		int index = 0;
		for (int i = 0; i < teamsCount; i++) {
			TextView teamLabel = new TextView(this);
			teamLabel.setText(mTeamNames[i]);
			mTeamsContainer.addView(teamLabel);
			for (int j = 0; j < mMaxTeamSizes[i]; j++) {
				mPlayerButtons[index] = new Button(this);
				mPlayerButtons[index].setOnClickListener(listener);
				mTeamsContainer.addView(mPlayerButtons[index]);
				clearPlayer(index);
				index++;
			}
		}
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
			optionsTitle.setText(getResources().getString(R.string.game_options));
			mOptionsContainer.addView(optionsTitle, 0);
		}
	}

	@Override
	public PlayerPool getPool() {
		return GameKey.getPool(mGameKey);
	}

	@Override
	public List<Player> toFilter() {
		List<Player> filter = new LinkedList<Player>();
		for (int i = 0; i < players.length; i++) {
			if (players[i] != null) {
				filter.add(players[i]);
			}
		}
		return filter;
	}

	@Override
	public void playerChosen(Player chosen) {
		if (chosen != null) {
			setPlayer(mChoosingPlayerSlot, chosen);
			applyButtonsState();
		}
	}	
}
