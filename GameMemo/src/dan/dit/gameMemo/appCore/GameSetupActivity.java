package dan.dit.gameMemo.appCore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.SortedSet;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.TreeSet;

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
import android.util.Log;
import android.util.SparseArray;
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
import dan.dit.gameMemo.gameData.player.ChoosePlayerDialogFragment;
import dan.dit.gameMemo.gameData.player.ChoosePlayerDialogFragment.ChoosePlayerDialogListener;
import dan.dit.gameMemo.gameData.player.DummyPlayer;
import dan.dit.gameMemo.gameData.player.DummyPlayer.DummyPool;
import dan.dit.gameMemo.gameData.player.Player;
import dan.dit.gameMemo.gameData.player.PlayerPool;
import dan.dit.gameMemo.gameData.player.TeamSetupViewController;
import dan.dit.gameMemo.gameData.player.TeamSetupViewController.TeamSetupCallback;
import dan.dit.gameMemo.storage.GameStorageHelper;
import dan.dit.gameMemo.util.ColorPickerDialog;
import dan.dit.gameMemo.util.ColorPickerView.OnColorChangedListener;

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
public class GameSetupActivity extends FragmentActivity implements ChoosePlayerDialogListener, TeamSetupCallback, OnColorChangedListener {
	public static final String EXTRA_TEAM_COLORS = "dan.dit.gameMemo.TEAM_COLORS"; // optional int[]
	public static final String EXTRA_FLAG_ALLOW_PLAYER_COLOR_EDITING = "dan.dit.gameMemo.ALLOW_PLAYER_COLOR_EDITING"; // boolean, default false
	public static final String EXTRA_FLAG_ALLOW_TEAM_COLOR_EDITING = "dan.dit.gameMemo.ALLOW_TEAM_COLOR_EDITING"; // boolean, default false
	public static final String EXTRA_FLAG_ALLOW_TEAM_NAME_EDITING = "dan.dit.gameMemo.ALLOW_TEAM_NAME_EDITING"; // boolean, default false
	public static final String EXTRA_FLAG_SUGGEST_UNFINISHED_GAME = "dan.dit.gameMemo.SUGGEST_UNFINISHED"; // boolean, default false
	public static final String EXTRA_FLAG_USE_DUMMY_PLAYERS = "dan.dit.gameMemo.USE_DUMMY_PLAYERS"; // boolean, default false
	public static final String EXTRA_TEAM_NAMES = "dan.dit.gameMemo.EXTRA_TEAM_NAMES"; // optional String[], if not given default name is used
	public static final String EXTRA_TEAM_IS_OPTIONAL = "dan.dit.gameMemo.EXTRA_TEAM_IS_OPTIONAL"; // optional boolean[], defaults to false for all not supplied values
	public static final String EXTRA_TEAM_MIN_PLAYERS = "dan.dit.gameMemo.EXTRA_TEAM_MIN_PLAYERS"; // int[], negative values will be interpreted as 0
	public static final String EXTRA_TEAM_MAX_PLAYERS = "dan.dit.gameMemo.EXTRA_TEAM_MAX_PLAYERS"; // int[], if not given default 10, must be > 0 each
	public static final String EXTRA_PLAYER_NAMES = "dan.dit.gameMemo.EXTRA_PLAYER_NAMES"; // optional String[], give suggestions for the slots, if result = ok these contains names, null for not required players of a team
	public static final String EXTRA_OPTIONS_BOOLEAN_VALUES = "dan.dit.gameMemo.EXTRA_OPTIONS_BOOLEAN_VALUES"; // optional boolean[]
	public static final String EXTRA_OPTIONS_BOOLEAN_NAMES = "dan.dit.gameMemo.EXTRA_OPTIONS_BOOLEAN_NAMES"; // optional String[], comes with OPTIONS_BOOLEAN_VALUES
	public static final String EXTRA_OPTIONS_NUMBER_VALUES = "dan.dit.gameMemo.EXTRA_OPTIONS_INT_VALUES"; // optinal int[]
	public static final String EXTRA_OPTIONS_NUMBER_NAMES = "dan.dit.gameMemo.EXTRA_OPTIONS_INT_NAMES"; // optional String[], comes with OPTIONS_INT_VALUES
	public static final String EXTRA_OPTIONS_NUMBER_MIN_VALUES = "dan.dit.gameMemo.EXTRA_OPTIONS_NUMBER_MIN_VALUES"; // optional int[], comes with OPTIONS_INT_VALUES
	public static final String EXTRA_OPTIONS_NUMBER_MAX_VALUES = "dan.dit.gameMemo.EXTRA_OPTIONS_NUMBER_MAX_VALUES"; // optional int[], comes with OPTIONS_INT_VALUES
	private static final String STORAGE_OPTIONS_NUMBER_VALUES_DEFAULT = "STORAGE_OPTIONS_INT_VALUES_DEFAULT"; // int[] for current values of OPTIONS_INT_VALUES
	private static final String STORAGE_OPTIONS_BOOLEAN_VALUES_DEFAULT = "STORAGE_OPTIONS_BOOLEAN_VALUES_DEFAULT"; // boolean[] for current values of OPTIONS_BOOLEAN_VALUES
	private static final String STORAGE_CHOOSING_PLAYER_CONTROLLER_INDEX = "STORAGE_CHOOSING_PLAYER_CONTROLLER_INDEX"; //int ignored if no choose player dialog open, index of controller to chose player for 
	private static final String STORAGE_CHOOSING_COLOR_CONTROLLER_INDEX = "STORAGE_CHOOSING_COLOR_CONTROLLER_INDEX"; //int ignored if no color chooser dialog open, index of controller to chose color for
	private static final DummyPool DUMMY_PLAYERS = new DummyPool();
	private static final long SHUFFLE_PERIOD = 200; // in ms
	protected static final long SHUFFLE_DURATION = 2000; // in ms
	
	private int mGameKey;
	private boolean mSuggestUnfinished;
	private boolean mUseDummys;
	private boolean mAllowTeamNameEditing;
	private boolean mAllowPlayerColorChoosing;
	private boolean mAllowTeamColorChoosing;
	private int[] mTeamColor;
	private int[] mMinTeamSizes;
	private int[] mMaxTeamSizes;
	private boolean[] mTeamIsOptional;
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
	
	private ScrollView mScrollView;
	private LinearLayout mOptionsContainer;
	private LinearLayout mTeamsContainer;
	private Button mStartGame;
	private SortedSet<Integer> mHiddenTeams;
	private Map<Integer, TeamSetupViewController> mTeamControllers;
	private Timer mShufflePlayersTimer;
	private final Handler mTimerHandler = new Handler();

	private int mChoosingPlayerControllerIndex = -1;
	private int mChoosingColorControllerIndex = -1;
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
		DUMMY_PLAYERS.setDummyBaseName(getResources().getString(R.string.player_dummy_name), getResources().getString(R.string.player_dummy_name_regex));
		mScrollView = (ScrollView) findViewById(R.id.game_setup_mainscroll);
		mTeamsContainer = (LinearLayout) findViewById(R.id.teams_container);
		mOptionsContainer = (LinearLayout) findViewById(R.id.options_container);
		mStartGame = (Button) findViewById(R.id.startGame);
		// load flags and immutable parameters here, the rest after all creation is done
		mSuggestUnfinished = getIntent().getExtras().getBoolean(EXTRA_FLAG_SUGGEST_UNFINISHED_GAME);
		mUseDummys = getIntent().getExtras().getBoolean(EXTRA_FLAG_USE_DUMMY_PLAYERS);
		mAllowTeamNameEditing = getIntent().getExtras().getBoolean(EXTRA_FLAG_ALLOW_TEAM_NAME_EDITING);
		mAllowTeamColorChoosing = getIntent().getExtras().getBoolean(EXTRA_FLAG_ALLOW_TEAM_COLOR_EDITING);
		mAllowPlayerColorChoosing = getIntent().getExtras().getBoolean(EXTRA_FLAG_ALLOW_PLAYER_COLOR_EDITING);
		initListeners();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.game_setup, menu);
		mAddTeam = menu.findItem(R.id.add_team);
		mShuffle = menu.findItem(R.id.shuffle);
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
		mScrollView.fullScroll(View.FOCUS_UP);
		scrollTop();
	}
	
	@Override
	public void onResume() {
		super.onResume();
		applyButtonsState();
	}
	
	private void scrollTop() {
		View firstTeamView = mTeamsContainer.getChildAt(0);
		if (firstTeamView != null) {
			firstTeamView.requestFocus();
		}
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		for (TeamSetupViewController ctr : mTeamControllers.values()) {
			ctr.close();
		}
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
		for (int i = 0; i < mMinTeamSizes.length; i++) {
			TeamSetupViewController ctr = mTeamControllers.get(i);
			if (ctr != null && isTeamDeletable(i)) {
				removeTeam(i);
			} else if (ctr != null) {
				ctr.reset();
			}
		}
		for (int i = 0; i < mOptionBooleanCheckers.length; i++) {
			mOptionBooleanCheckers[i].setChecked(mOptionsBooleanDefault[i]);
		}
		for (int i = 0; i < mOptionNumberFields.length; i++) {
			mOptionNumberFields[i].setText(String.valueOf(mOptionsNumbersDefault[i]));
		}
		applyButtonsState();		
		mScrollView.fullScroll(View.FOCUS_UP);
	}
	
	private void applyButtonsState() {
		mStartGame.setEnabled(startGameButtonCondition());
		if (mShuffle != null) {
			mShuffle.setEnabled(shuffleButtonCondition());
		}
		if (mAddTeam != null && mTeamControllers != null && mMinTeamSizes != null) {
			boolean enableNewTeam = mTeamControllers.size() < mMinTeamSizes.length;
			mAddTeam.setEnabled(enableNewTeam);
			mAddTeam.setVisible(enableNewTeam);
		}
	}
	
	private boolean startGameButtonCondition() {
		return hasAllPlayers();
	}
	
	private void onGameStart() {
		if (!mSuggestUnfinished || !searchAndSuggestUnfinished()) {
			startNewGame(); // either not suggesting or did not find an unfinished game, start new one
		}
	}
	
	private List<Player> getAllPlayers(boolean includeDummys) {
		List<Player> team = new ArrayList<Player>();
		for (TeamSetupViewController ctr : mTeamControllers.values()) {
			team.addAll(ctr.getPlayers(includeDummys));
		}
		return team;
	}
	
	private boolean searchAndSuggestUnfinished() {
		List<Player> team = getAllPlayers(false);
		if (team.size() == 0) {
			return false;
		}
		// if dummys are being used then look for a subset, else an exact match
		final long unfinishedId = team.size() > 0 ? Game.getUnfinishedGame(mGameKey, getContentResolver(), team, mUseDummys) : Game.NO_ID;
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
		PlayerPool pool = getPool();
		// for all used dummys we need to populate a real player (with the same color) since dummys are only used by the setup activity
		for (Player p : getAllPlayers(true)) {
			if (p instanceof DummyPlayer) {
				Player dedummiedPlayer = pool.populatePlayer(p.getName());
				dedummiedPlayer.setColor(p.getColor());
			}
		}
		refreshTeamNamesAndColors();
		returnIntent.putExtra(EXTRA_TEAM_NAMES, mTeamNames);
		returnIntent.putExtra(EXTRA_TEAM_COLORS, mTeamColor);
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
		List<Integer> playerCountForTeams = new ArrayList<Integer>(mTeamControllers.size());
		List<Player> allPlayers = new ArrayList<Player>();
		for (TeamSetupViewController ctr : mTeamControllers.values()) {
			List<Player> curr = ctr.getPlayers(true);
			allPlayers.addAll(curr);
			playerCountForTeams.add(Integer.valueOf(curr.size())); 
		}
		Collections.shuffle(allPlayers);
		if (mUseDummys) {
			int index = 0;
			int currStartPos = 0;
			// give each team as many players as it had before, can be dummys or real players
			for (TeamSetupViewController ctr : mTeamControllers.values()) {
				assert playerCountForTeams.get(index) >= ctr.getMinPlayers();
				int currTeamNewPlayerCount = playerCountForTeams.get(index);
				ctr.setPlayers(allPlayers.subList(currStartPos, currStartPos + currTeamNewPlayerCount));
				currStartPos += currTeamNewPlayerCount;
				index++;
			}
		} else {
			SparseArray<List<Player>> playersForTeam = new SparseArray<List<Player>>(mTeamControllers.size());
			// first prefer filling teams to have the minimum required player count
			List<TeamSetupViewController> possibleControllers = new ArrayList<TeamSetupViewController>(mTeamControllers.values());
			Random rnd = new Random();
			Iterator<Player> it = allPlayers.iterator();
			while (it.hasNext() && possibleControllers.size() > 0) {
				Player p = it.next();
				TeamSetupViewController ctr = possibleControllers.get(rnd.nextInt(possibleControllers.size()));
				int index = ctr.getTeamNumber();
				List<Player> playersForTeamCurr = playersForTeam.get(index);
				if (playersForTeamCurr == null) {
					playersForTeamCurr = new ArrayList<Player>(ctr.getMaxPlayers());
					playersForTeam.put(index, playersForTeamCurr);
				}
				playersForTeamCurr.add(p);
				if (ctr.getMinPlayers() <= playersForTeamCurr.size()) {
					possibleControllers.remove(ctr);
				}
			}
			if (it.hasNext()) {
				// second fill teams that still have capacity with remaining players
				for (TeamSetupViewController ctr : mTeamControllers.values()) {
					if (playersForTeam.get(ctr.getTeamNumber()) == null || playersForTeam.get(ctr.getTeamNumber()).size() < ctr.getMaxPlayers() ) {
						possibleControllers.add(ctr);
					}
				}
				while (it.hasNext() && possibleControllers.size() > 0) {
					Player p = it.next();
					TeamSetupViewController ctr = possibleControllers.get(rnd.nextInt(possibleControllers.size()));
					int index = ctr.getTeamNumber();
					List<Player> playersForTeamCurr = playersForTeam.get(index);
					if (playersForTeamCurr == null) {
						playersForTeamCurr = new ArrayList<Player>(ctr.getMaxPlayers());
						playersForTeam.put(index, playersForTeamCurr);
					}
					playersForTeamCurr.add(p);
					if (playersForTeamCurr.size() >= ctr.getMaxPlayers()) {
						possibleControllers.remove(ctr);
					}
				}
				assert !it.hasNext();
			}
			for (TeamSetupViewController ctr : mTeamControllers.values()) {
				ctr.setPlayers(playersForTeam.get(ctr.getTeamNumber()));
			}
		}
	}

	private boolean hasAllPlayers() {
		if (mTeamControllers == null) {
			return false;
		}
		for (TeamSetupViewController ctr : mTeamControllers.values()) {
			if (!ctr.hasRequiredPlayers(true)) {
				return false;
			}
		}
		return true;
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
		refreshTeamNamesAndColors();
		outState.putStringArray(EXTRA_TEAM_NAMES, mTeamNames);
		outState.putIntArray(EXTRA_TEAM_COLORS, mTeamColor);
		outState.putBooleanArray(EXTRA_TEAM_IS_OPTIONAL, mTeamIsOptional);
		outState.putInt(GameKey.EXTRA_GAMEKEY, mGameKey);
		outState.putIntArray(EXTRA_OPTIONS_NUMBER_MAX_VALUES, mOptionsNumberMaxValues);
		outState.putIntArray(EXTRA_OPTIONS_NUMBER_MIN_VALUES, mOptionsNumberMinValues);
		outState.putIntArray(EXTRA_OPTIONS_NUMBER_VALUES, mOptionsNumbers);
		outState.putBooleanArray(EXTRA_OPTIONS_BOOLEAN_VALUES, mOptionsBoolean);
		outState.putBooleanArray(STORAGE_OPTIONS_BOOLEAN_VALUES_DEFAULT, mOptionsBooleanDefault);
		outState.putStringArray(EXTRA_OPTIONS_NUMBER_NAMES, mOptionsNumberNames);
		outState.putStringArray(EXTRA_OPTIONS_BOOLEAN_NAMES, mOptionsBooleanNames);
		outState.putIntArray(STORAGE_OPTIONS_NUMBER_VALUES_DEFAULT, mOptionsNumbersDefault);
		outState.putInt(STORAGE_CHOOSING_COLOR_CONTROLLER_INDEX, mChoosingColorControllerIndex);
		outState.putInt(STORAGE_CHOOSING_PLAYER_CONTROLLER_INDEX, mChoosingPlayerControllerIndex);
	}
	
	private void refreshTeamNamesAndColors() {
		if (mTeamControllers != null) {
			for (TeamSetupViewController ctr : mTeamControllers.values()) {
				mTeamNames[ctr.getTeamNumber()] = ctr.getTeamName();
				mTeamColor[ctr.getTeamNumber()] = ctr.getTeamColor();
			}
		}
	}
	
	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		if (savedInstanceState != null) {
			mChoosingColorControllerIndex = savedInstanceState.getInt(STORAGE_CHOOSING_COLOR_CONTROLLER_INDEX, -1);
			mChoosingPlayerControllerIndex = savedInstanceState.getInt(STORAGE_CHOOSING_PLAYER_CONTROLLER_INDEX, -1);
		}
	}
	
	private String[] allPlayersToArray() {
		List<String> allPlayerNames = new LinkedList<String>();
		for (int i = 0; i < mMinTeamSizes.length; i++) {
			if (mHiddenTeams.contains(i)) {
				// team not used, fill with null names
				for (int k = 0; k < mMaxTeamSizes[i]; k++) {
					allPlayerNames.add(null);
				}
			} else {
				TeamSetupViewController ctr = mTeamControllers.get(i);
				assert ctr != null;
				List<Player> curr = ctr.getPlayers(true);
				for (Player p : curr) {
					allPlayerNames.add(p.getName());
				}
				// fill every not taken player with a null name
				for (int k = 0; k < ctr.getMaxPlayers() - curr.size(); k++) {
					allPlayerNames.add(null);
				}
			}
		}
		String[] playerNames = new String[allPlayerNames.size()];
		for (int i = 0; i < playerNames.length; i++) {
			playerNames[i] = allPlayerNames.get(i);
		}
		return playerNames;
	}
	
	private void savePlayerToBundle(Bundle out) {
		if (out == null) {
			return;
		}
		out.putStringArray(EXTRA_PLAYER_NAMES, allPlayersToArray());
	}
	
	private void savePlayerToIntent(Intent out) {
		if (out == null) {
			return;
		}
		out.putExtra(EXTRA_PLAYER_NAMES, allPlayersToArray());
	}
	
	private boolean prepareTeamParameters(int[] minTeamSize, int[] maxTeamSize, 
			String[] teamNames, boolean[] isOptional, int[] teamColors) {
		if (minTeamSize == null || minTeamSize.length == 0) {
			return false; // there is nothing we can do when we did not even get a min single min size
		}
		// ensure minTeamSize.length == maxTeamSize.length, use the greater one
		if (maxTeamSize == null) {
			maxTeamSize = new int[minTeamSize.length];
		} else if (maxTeamSize.length < minTeamSize.length) {
			int[] temp = maxTeamSize;
			maxTeamSize = new int[minTeamSize.length];
			System.arraycopy(temp, 0, maxTeamSize, 0, temp.length);
		} else if (maxTeamSize.length > minTeamSize.length) {
			int[] temp = minTeamSize;
			minTeamSize = new int[maxTeamSize.length];
			System.arraycopy(temp, 0, minTeamSize, 0, temp.length);
		}
		// ensure maxTeamSize >= minTeamSize >= 1
		for (int i = 0; i < minTeamSize.length; i++) {
			minTeamSize[i] = Math.max(1, minTeamSize[i]);
			maxTeamSize[i] = Math.max(minTeamSize[i], maxTeamSize[i]);
		}
		mMaxTeamSizes = maxTeamSize;
		mMinTeamSizes = minTeamSize;
		// ensure teamNames.length = TeamSize.length, null values allowed
		if (teamNames == null) {
			teamNames = new String[minTeamSize.length];
		} else if (teamNames.length != minTeamSize.length) {
			String[] temp = teamNames;
			teamNames = new String[minTeamSize.length];
			System.arraycopy(temp, 0, teamNames, 0, Math.min(temp.length, minTeamSize.length));
		}
		mTeamNames = teamNames;
		// ensure isOptional.length = TeamSize.length, default to false
		if (isOptional == null) {
			isOptional = new boolean[minTeamSize.length];
		} else if (isOptional.length != minTeamSize.length) {
			boolean[] temp = isOptional;
			isOptional = new boolean[minTeamSize.length];
			System.arraycopy(temp, 0, isOptional, 0, Math.min(temp.length, minTeamSize.length));
		}
		mTeamIsOptional = isOptional;
		// ensure mTeamColor.length = TeamSize.length, use default color
		if (teamColors == null) {
			teamColors = new int[minTeamSize.length];
			Arrays.fill(teamColors, TeamSetupViewController.DEFAULT_TEAM_COLOR);
		} else if (teamColors.length != minTeamSize.length) {
			int[] temp = teamColors;
			teamColors = new int[minTeamSize.length];
			Arrays.fill(teamColors, TeamSetupViewController.DEFAULT_TEAM_COLOR);			
			System.arraycopy(temp, 0, teamColors, 0, Math.min(temp.length, minTeamSize.length));
		}
		mTeamColor = teamColors;		
		return true;
	}
	
	private void buildUIFromBundle(Bundle in) {
		int[] minTeamSize = in.getIntArray(EXTRA_TEAM_MIN_PLAYERS);
		int[] maxTeamSize = in.getIntArray(EXTRA_TEAM_MAX_PLAYERS);
		String[] teamNames = in.getStringArray(EXTRA_TEAM_NAMES);
		int[] teamColors = in.getIntArray(EXTRA_TEAM_COLORS);
		boolean[] isOptional = in.getBooleanArray(EXTRA_TEAM_IS_OPTIONAL);
		if (!prepareTeamParameters(minTeamSize, maxTeamSize, teamNames, isOptional, teamColors)) {
			setResult(RESULT_CANCELED);
			Log.e(getClass().getName(), "Could not prepare parameters to start. Need at least one team minimum size.");
			finish();
			return;
		}
		initTitle();
		initTeamsUI(in.getStringArray(EXTRA_PLAYER_NAMES));
		initOptionsUI(in);
		applyBackgroundTheme();
	}
	
	private void applyBackgroundTheme() {
		int btnResId = GameKey.getButtonResource(mGameKey);
		mStartGame.setBackgroundResource(btnResId);
		for (TeamSetupViewController ctr : mTeamControllers.values()) {
			ctr.applyBackgroundTheme(btnResId);
		}
	}
	
	private void initTitle() {
		mStartGame.setCompoundDrawablesWithIntrinsicBounds(GameKey.getGameIconId(mGameKey), 0, 0, 0);
		mStartGame.setText(getResources().getString(R.string.game_setup_start, GameKey.getGameName(mGameKey)));
	}
	
	private void initTeamsUI(String[] playerNames) {
		mTeamControllers = new TreeMap<Integer, TeamSetupViewController>();
		mTeamsContainer.removeAllViews();
		int index = 0;
		mHiddenTeams = new TreeSet<Integer>();
		for (int i = 0; i < mMinTeamSizes.length; i++) {
			boolean showController = false;
			if (!mTeamIsOptional[i]) {
				// if team is not optional show it
				showController = true;
			} else if (playerNames != null) {
				// if there is a not-null name in the range for this team, then use a controller, else keep it hidden
				boolean hasNotNull = false;
				for (int j = index; j < index + mMaxTeamSizes[i] && j < playerNames.length; j++) {
					hasNotNull |= playerNames[j] != null;
				}
				if (hasNotNull) {
					showController = true;
				}
			}
			if (showController) {
				List<Player> range = null;
				if (playerNames != null) {
					range = new ArrayList<Player>(mMaxTeamSizes[i]);
					// get the players for the names, in case the player is a dummy by name, use the dummy in this case
					PlayerPool pool = getPool();
					for (int j = index; j < index + mMaxTeamSizes[i] && j < playerNames.length; j++) {
						if (playerNames[j] == null) {
							range.add(null);
						} else {
							DummyPlayer asDummy = DUMMY_PLAYERS.obtainDummy(playerNames[j]);
							if (asDummy != null) {
								range.add(asDummy);
							} else {
								range.add(pool.populatePlayer(playerNames[j]));
							}
						}
					}
				}
				addTeam(i, range);
			} else {
				mHiddenTeams.add(i);
			}
			index += mMaxTeamSizes[i];
		}
		applyButtonsState();
	}
	
	private void addTeam(int teamIndex, List<Player> defaultPlayers) {
		if (teamIndex < 0 || teamIndex >= mMinTeamSizes.length) {
			return;
		}
		removeTeam(teamIndex);
		TeamSetupViewController ctr = new TeamSetupViewController(this, teamIndex, mMinTeamSizes[teamIndex], 
				mMaxTeamSizes[teamIndex], defaultPlayers, mUseDummys, this);
		int viewIndex = 0;
		for (int i : mTeamControllers.keySet()) {
			if (i > teamIndex) {
				break;
			} else {
				viewIndex++;
			}
		}
		mTeamControllers.put(teamIndex, ctr);
		ctr.setTeamDeletable(isTeamDeletable(teamIndex));
		ctr.setTeamName(mTeamNames[teamIndex], mAllowTeamNameEditing);
		ctr.setTeamColorChoosable(mAllowTeamColorChoosing);
		ctr.setTeamColor(mTeamColor[teamIndex]);
		ctr.applyBackgroundTheme(GameKey.getButtonResource(mGameKey));
		mTeamsContainer.addView(ctr.getView(), viewIndex);
	}
	
	private void onNewTeamClicked() {
		if (mHiddenTeams.size() > 0) {
			Integer newTeamIndex = mHiddenTeams.first();
			addTeam(newTeamIndex, null);
			mHiddenTeams.remove(newTeamIndex);
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
		List<Player> filter = new LinkedList<Player>();
		if (mTeamControllers != null) {
			for (TeamSetupViewController ctr : mTeamControllers.values()) {
				filter.addAll(ctr.toFilter());
			}
		}
		return filter;
	}

	@Override
	public void playerChosen(int playerIndex, Player chosen) {
		if (chosen == null) {
			return;
		}
		TeamSetupViewController ctr = mTeamControllers.get(mChoosingPlayerControllerIndex);
		if (ctr == null) {
			addTeam(mChoosingPlayerControllerIndex, null); 
			ctr = mTeamControllers.get(mChoosingPlayerControllerIndex);
		}
		if (ctr != null) {
			// maybe the player entered a dummy player name, then we want this dummy and not a player that mimiks the dummy but is not one
			Player reallyChosen = DUMMY_PLAYERS.obtainDummy(chosen);
			if (reallyChosen == null) {
				reallyChosen = chosen;
			}
			ctr.playerChosen(playerIndex, reallyChosen);
			applyButtonsState();
		}
	}

	@Override
	public void choosePlayer(int teamIndex, int playerIndex) {
		mChoosingPlayerControllerIndex = teamIndex;
		// do not change colors of dummys (since this is also not supported, would only change color of copy of dummy in players pool
		Player oldPlayer = mTeamControllers.get(teamIndex).getPlayer(playerIndex);
		if (oldPlayer instanceof DummyPlayer) {
			oldPlayer = null;
		}
		ChoosePlayerDialogFragment dialog = ChoosePlayerDialogFragment.newInstance(playerIndex, 
				oldPlayer, mAllowPlayerColorChoosing);
		dialog.show(getSupportFragmentManager(), "ChoosePlayerDialog");
	}

	@Override
	public void chooseTeamColor(int teamIndex) {
		mChoosingColorControllerIndex = teamIndex;
		showColorChooserDialog(mTeamControllers.get(teamIndex).getTeamColor(), "ChooseTeamColorDialog");		
	}
	
	private void showColorChooserDialog(int color, String tag) {
		ColorPickerDialog dialog = new ColorPickerDialog();
		Bundle args = new Bundle();
		args.putInt(ColorPickerDialog.EXTRA_COLOR, color);
		dialog.setArguments(args);
		dialog.show(getSupportFragmentManager(), tag);
	}

	@Override
	public void notifyPlayerCountChanged() {
		applyButtonsState();
	}

	private boolean isTeamDeletable(int teamIndex) {
		return teamIndex >= 0 && teamIndex < mTeamIsOptional.length && mTeamIsOptional[teamIndex];
	}

	@Override
	public void requestTeamDelete(int teamIndex) {
		if (isTeamDeletable(teamIndex)) {
			removeTeam(teamIndex);
		}
		applyButtonsState();
	}
	
	private boolean removeTeam(int teamIndex) {
		// if there currently is a controller for the team with the given index, remove the controller and view
		if (teamIndex >= 0 && teamIndex < mMinTeamSizes.length) {
			TeamSetupViewController ctr = mTeamControllers.remove(teamIndex);
			if (ctr != null) {
				ctr.close();
				mTeamsContainer.removeView(ctr.getView());
				mHiddenTeams.add(teamIndex);
				return true;
			}
		}
		return false;
	}

	@Override
	public void onColorChanged(int color) {
		TeamSetupViewController ctr = mTeamControllers.get(mChoosingColorControllerIndex);
		if (ctr == null) {
			addTeam(mChoosingColorControllerIndex, null); 
			ctr = mTeamControllers.get(mChoosingColorControllerIndex);
		}
		if (ctr != null) {
			ctr.setTeamColor(color);
		}
	}

	@Override
	public DummyPlayer obtainNewDummy() {
		return DUMMY_PLAYERS.obtainNewDummy();
	}

	@Override
	public void onPlayerColorChanged(int arg, Player concernedPlayer) {
		TeamSetupViewController ctr = mTeamControllers.get(mChoosingPlayerControllerIndex);
		if (ctr == null) {
			addTeam(mChoosingPlayerControllerIndex, null); 
			ctr = mTeamControllers.get(mChoosingPlayerControllerIndex);
		}
		if (ctr != null) {
			ctr.notifyDataSetChanged();
		}
	}	
}
