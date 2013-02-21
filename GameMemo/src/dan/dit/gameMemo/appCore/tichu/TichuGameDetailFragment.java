package dan.dit.gameMemo.appCore.tichu;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import android.graphics.PorterDuff;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.ListFragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import dan.dit.gameMemo.R;
import dan.dit.gameMemo.gameData.game.Game;
import dan.dit.gameMemo.gameData.game.GameKey;
import dan.dit.gameMemo.gameData.game.tichu.TichuBidType;
import dan.dit.gameMemo.gameData.game.tichu.TichuGame;
import dan.dit.gameMemo.gameData.game.tichu.TichuRound;
import dan.dit.gameMemo.gameData.player.ChoosePlayerDialogFragment.ChoosePlayerDialogListener;
import dan.dit.gameMemo.gameData.player.ChoosePlayerDialogFragment;
import dan.dit.gameMemo.gameData.player.Player;
import dan.dit.gameMemo.gameData.player.PlayerDuo;
import dan.dit.gameMemo.gameData.player.PlayerPool;
import dan.dit.gameMemo.storage.GameStorageHelper;
import dan.dit.gameMemo.util.compression.CompressedDataCorruptException;
import dan.dit.gameMemo.util.compression.Compressor;
/**
 * A fragment that displays a TichuGame with all of its rounds in a list,
 * allowing adding new rounds and editing existing rounds only for unfinished games.<br>
 * Takes a long game id extra (preferred) or a parcelable Uri extra {@link Uri} to the TichuGame to load
 * referenced by the key GameStorageHelper.getCursorItemType(gameKey) as an argument.<br>
 * To start with a new game, do not give a id or uri extra but instead 4 valid player names {@link Player} String extras, 2 for each team.<br>
 * <b>Requires the hosting activity to implement the {@link CloseDetailViewRequestListener} interface to listen to
 * requests of the user to close this detail fragment.</b>
 * @author Daniel
 *
 */
public class TichuGameDetailFragment extends ListFragment implements ChoosePlayerDialogListener {
	public static final String EXTRA_NEW_GAME_USE_MERCY_RULE = "dan.dit.gameMemo.USE_MERCY_RULE";
	public static final String EXTRA_NEW_GAME_SCORE_LIMIT = "dan.dit.gameMemo.SCORE_LIMIT";
	
	/**
	 * Indicates the minimum amount of required finishers, so that a new game round is saveable. Value between 1 and TichuGame.TOTAL_PLAYERS.
	 */
	public static final int MIN_GIVEN_FINISHER_COUNT_TO_SAVE_ROUND = 1; // must between 1 and TichuGame.TOTAL_PLAYERS (=4)
	
	/**
	 * Indicates if games that are loaded and are already finished are immutable and game rounds cannot be added or changed.
	 */
	public static final boolean LOADED_FINISHED_GAMES_ARE_IMMUTABLE = true;
	
	/**
	 * String extra for the name of the first player of the first team.
	 */
	public static final String EXTRAS_TEAM1_PLAYER_1 = "dan.dit.gameMemo.game_team1_player1";
	
	/**
	 * String extra for the name of the second player of the first team.
	 */
	public static final String EXTRAS_TEAM1_PLAYER_2 = "dan.dit.gameMemo.game_team1_player2";
	
	/**
	 * String extra for the name of the first player of the second team.
	 */
	public static final String EXTRAS_TEAM2_PLAYER_1 = "dan.dit.gameMemo.game_team2_player1";
	
	/**
	 * String extra for the name of the second player of the second team.
	 */
	public static final String EXTRAS_TEAM2_PLAYER_2 = "dan.dit.gameMemo.game_team2_player2";

	/**
	 * The drawable id of the drawable used to symbol a small tichu bid.
	 */
	public static final int TICHU_BID_SMALL_DRAWABLE_ID = R.drawable.tichu_small_bid;
	
	/**
	 * The drawable id of the drawable used to symbol a big tichu bid.
	 */
	public static final int TICHU_BID_BIG_DRAWABLE_ID = R.drawable.tichu_big_bid;
	
	/**
	 * The drawable id of the drawable used to symbol no tichu bid.
	 */
	public static final int TICHU_BID_NONE_DRAWABLE_ID = R.drawable.tichu_main;
	
	/**
	 * The drawable id of the drawable used to symbol a small lost tichu bid.
	 */
	public static final int TICHU_BID_SMALL_LOST_DRAWABLE_ID = R.drawable.tichu_small_bid_lost;
	
	/**
	 * The drawable id of the drawable used to symbol a big lost tichu bid.
	 */
	public static final int TICHU_BID_BIG_LOST_DRAWABLE_ID = R.drawable.tichu_big_bid_lost;
	
	private static final String PREFERENCES_SHOW_DELTA = "dan.dit.gameMemo.PREFERENCE_SHOW_DELTA";
	private static final String PREFERENCES_SHOW_TICHUS = "dan.dit.gameMemo.PREFERENCE_SHOW_TICHUS";
	
	private static final String STORAGE_IS_IMMUTABLE = "STORAGE_IS_IMMUTABLE";
	private static final String STORAGE_SELECTED_ROUND = "STORAGE_SELECTED_ROUND";
	private static final String STORAGE_TEXT_SCORE_TEAM1 = "STORAGE_TEXT_SCORE_TEAM1";
	private static final String STORAGE_TEXT_SCORE_TEAM2 = "STORAGE_TEXT_SCORE_TEAM2";
	private static final String STORAGE_FINISHERS_LIST = "STORAGE_FINISHERS_LIST";
	private static final String STORAGE_BIDS_LIST = "STORAGE_BIDS_LIST";

	private static final int[] PLAYER_FINISHER_POS_COLOR = new int[] {0xFF58FC1C, 0xFFB2FC1C,0xFFE2F41C, 0xFFF4B71C};

	
	// references the UI elements or listeners 
	private Button mPlayer[];
	private ImageButton[] mPlayerTichu;
	private TextView mInfoText;
	private TextView mStatusText;
	private EditText mScoreTeam1;
	private TextWatcher mScoreTeam1Watcher;
	private EditText mScoreTeam2;
	private TextWatcher mScoreTeam2Watcher;
	private ImageButton mLockIn;
	private TichuGameRoundAdapter mAdapter;
	private CloseDetailViewRequestListener mCloseRequestListener;
	
	/**
	 * A listener interface that is required
	 * @author Daniel
	 *
	 */
	public interface CloseDetailViewRequestListener {
		void closeDetailView(boolean error);
	}
	
	// member vars concerning building up a new game round, visualizing and editing rounds
	private Date mLastRunningTimeUpdate;
	private boolean mIsImmutable;
	private TichuGame mGame;
	private TichuRound mCurrRound;
	private int mCurrRoundIndex;
	private List<Integer> mFinisher;
	private TichuBidType[] mBids;
	private TichuGameStateMachine mStateMachine;
	private int mWantsToExchangePlayerId;
	
	public static TichuGameDetailFragment newInstance(long gameId) {
		TichuGameDetailFragment f = new TichuGameDetailFragment();

        // Supply index input as an argument.
        Bundle args = new Bundle();
        args.putLong(GameStorageHelper.getCursorItemType(GameKey.TICHU), gameId);
        f.setArguments(args);

        return f;
	}
	
	public static TichuGameDetailFragment newInstance(Bundle extras) {
		TichuGameDetailFragment f = new TichuGameDetailFragment();
        f.setArguments(extras);
        return f;
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		mCloseRequestListener = (CloseDetailViewRequestListener) activity; // throws if given activity does not listen to close requests
		if (!(activity instanceof ChoosePlayerDialogListener)) {
			throw new ClassCastException("Hosting activity must implement ChoosePlayerDialogListener interface.");
		}
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}
	

	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View baseView = inflater.inflate(R.layout.tichu_detail, null);
		return baseView;
	}
		
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState); 
		getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
		getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
		this.getListView().setDividerHeight(2);
		mFinisher = new ArrayList<Integer>(TichuGame.TOTAL_PLAYERS);
		mBids = new TichuBidType[TichuGame.TOTAL_PLAYERS];
		mPlayer = new Button[TichuGame.TOTAL_PLAYERS];
		mPlayer[0] = (Button) getView().findViewById(R.id.tichu_game_player1);
		mPlayer[1] = (Button) getView().findViewById(R.id.tichu_game_player2);
		mPlayer[2] = (Button) getView().findViewById(R.id.tichu_game_player3);
		mPlayer[3] = (Button) getView().findViewById(R.id.tichu_game_player4);
		mPlayerTichu = new ImageButton[TichuGame.TOTAL_PLAYERS];
		mPlayerTichu[0] = (ImageButton) getView().findViewById(R.id.tichu1);
		mPlayerTichu[1] = (ImageButton) getView().findViewById(R.id.tichu2);
		mPlayerTichu[2] = (ImageButton) getView().findViewById(R.id.tichu3);
		mPlayerTichu[3] = (ImageButton) getView().findViewById(R.id.tichu4);
		mScoreTeam1 = (EditText) getView().findViewById(R.id.scoreTeam1);
		mScoreTeam2 = (EditText) getView().findViewById(R.id.scoreTeam2);
		mLockIn = (ImageButton) getView().findViewById(R.id.tichu_game_lockin);
		mStatusText = (TextView) getView().findViewById(R.id.tichu_game_status);
		mInfoText = (TextView) getView().findViewById(R.id.tichu_game_info);
		mStateMachine = new TichuGameStateMachine();
		loadOrStartGame(savedInstanceState);
		registerForContextMenu(getListView());
		initListeners();
		handleRestorageOfUnsavedUserInput(savedInstanceState);
	}
	
	private void synchPlayerNames() {
		// invoked at start from fillData and
		mPlayer[0].setText(mGame.getTeam1().getFirst().getName());
		mPlayer[1].setText(mGame.getTeam1().getSecond().getName());
		mPlayer[2].setText(mGame.getTeam2().getFirst().getName());
		mPlayer[3].setText(mGame.getTeam2().getSecond().getName());
	}

	public static void copyExtrasToIntent(Intent i, Bundle extras) {
		String currKey;
		currKey = GameStorageHelper.getCursorItemType(GameKey.TICHU);
		if (extras.containsKey(currKey)) i.putExtra(currKey, extras.getLong(currKey));
		currKey = GameStorageHelper.getCursorItemType(GameKey.TICHU);
		if (extras.containsKey(currKey)) i.putExtra(currKey, extras.getParcelable(currKey));
		currKey = EXTRAS_TEAM1_PLAYER_1;
		if (extras.containsKey(currKey)) i.putExtra(currKey, extras.getString(currKey));
		currKey = EXTRAS_TEAM1_PLAYER_2;
		if (extras.containsKey(currKey)) i.putExtra(currKey, extras.getString(currKey));
		currKey = EXTRAS_TEAM2_PLAYER_1;
		if (extras.containsKey(currKey)) i.putExtra(currKey, extras.getString(currKey));
		currKey = EXTRAS_TEAM2_PLAYER_2;
		if (extras.containsKey(currKey)) i.putExtra(currKey, extras.getString(currKey));
		currKey = EXTRA_NEW_GAME_SCORE_LIMIT;
		if (extras.containsKey(currKey)) i.putExtra(currKey, extras.getInt(currKey));
		currKey = EXTRA_NEW_GAME_USE_MERCY_RULE;
		if (extras.containsKey(currKey)) i.putExtra(currKey, extras.getBoolean(currKey));
	}	
	
	private void loadOrStartGame(Bundle savedInstanceState) {
		// Check from the saved Instance
		long gameId = (savedInstanceState == null) ? -1 : savedInstanceState
				.getLong(GameStorageHelper.getCursorItemType(GameKey.TICHU), -1);

		// Or passed from the other activity
		String player1 = null;
		String player2 = null;
		String player3 = null;
		String player4 = null;
		Bundle extras = getArguments();
		if (extras != null) {
			long extraId = extras
					.getLong(GameStorageHelper.getCursorItemType(GameKey.TICHU), -1);
			if (Game.isValidId(extraId)) {
				gameId = extraId;
			} else if (!Game.isValidId(gameId)) {
				// no id given, but maybe an uri?
				Uri uri = extras.getParcelable(GameStorageHelper.getCursorItemType(GameKey.TICHU));
				gameId = GameStorageHelper.getIdFromUri(uri);
			}
			player1 = extras.getString(EXTRAS_TEAM1_PLAYER_1);
			player2 = extras.getString(EXTRAS_TEAM1_PLAYER_2);
			player3 = extras.getString(EXTRAS_TEAM2_PLAYER_1);
			player4 = extras.getString(EXTRAS_TEAM2_PLAYER_2);
		}

		mLastRunningTimeUpdate = new Date();
		if (Game.isValidId(gameId)) {
			loadGame(gameId, savedInstanceState);
		} else if (TichuGame.areValidPlayers(player1, player2, player3, player4)) {
			boolean useMercyRule = extras != null ? extras.getBoolean(EXTRA_NEW_GAME_USE_MERCY_RULE, TichuGame.DEFAULT_USE_MERCY_RULE) : TichuGame.DEFAULT_USE_MERCY_RULE;
			int scoreLimit = extras != null ? extras.getInt(EXTRA_NEW_GAME_SCORE_LIMIT, TichuGame.DEFAULT_SCORE_LIMIT) : TichuGame.DEFAULT_SCORE_LIMIT;
			createNewGame(new PlayerDuo(player1, player2, TichuGame.PLAYERS), new PlayerDuo(player3, player4, TichuGame.PLAYERS),
					useMercyRule, scoreLimit);
		} else {
			Log.d("Tichu", "Failed loading or creating game for id " + gameId);
			mCloseRequestListener.closeDetailView(true);
			return;
		}
	}
	
	private void handleRestorageOfUnsavedUserInput(Bundle savedInstanceState) {
		if (savedInstanceState == null) {
			return; // nothing to restore
		}
		// if activity gets restored, then reselect the last selected round or restore user input
		int selectedRound = savedInstanceState.getInt(STORAGE_SELECTED_ROUND, -1);
		selectRound(selectedRound);
		if (mCurrRoundIndex == -1) {
			// no round selected, lets restore fields from bundle
			setScoreSilent(true, savedInstanceState.getString(STORAGE_TEXT_SCORE_TEAM1));
			setScoreSilent(false, savedInstanceState.getString(STORAGE_TEXT_SCORE_TEAM2));
			String storedBids = savedInstanceState.getString(STORAGE_BIDS_LIST);
			if (storedBids != null) {
				int id = TichuGame.PLAYER_ONE_ID;
				for (String bidKey : new Compressor(storedBids)) {
					setTichuBid(id, TichuBidType.getFromKey(bidKey), true);
					id++;
				}
			}
			String storedFinishers = savedInstanceState.getString(STORAGE_FINISHERS_LIST);
			if (storedFinishers != null) {
				for (String finisher : new Compressor(storedFinishers)) {
					int nextFinisherId = -1;
					try {
						nextFinisherId = Integer.parseInt(finisher);
					} catch (NumberFormatException nfe) {}
					if (nextFinisherId >= TichuGame.PLAYER_ONE_ID && nextFinisherId <= TichuGame.PLAYER_FOUR_ID) {
						setNextFinisher(nextFinisherId);
					}
				}
			}
		}
	}
	
	private void initListeners() {
		mLockIn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				mStateMachine.onLockinButtonPressed();
			}

		});
		mScoreTeam1Watcher = new TextWatcher() {

			@Override
			public void afterTextChanged(Editable s) {
				if (TichuRound.isValidScore(getScore(true))) {
					setScoreSilent(false, "");
					onRoundDataChange();
				} 
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {}

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {}
			
		};
		mScoreTeam1.addTextChangedListener(mScoreTeam1Watcher);
		mScoreTeam2Watcher = new TextWatcher() {

			@Override
			public void afterTextChanged(Editable s) {
				if (TichuRound.isValidScore(getScore(false))) {
					setScoreSilent(true, "");
					onRoundDataChange();
				}
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {}

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {}
			
		};
		mScoreTeam2.addTextChangedListener(mScoreTeam2Watcher);
		OnLongClickListener exchangePlayerListener = new OnLongClickListener() {

			@Override
			public boolean onLongClick(View v) {
				if (mIsImmutable) {
					return false;
				}
				int playerId = TichuGame.PLAYER_ONE_ID;
				for (View player : mPlayer) {
					if (v == player) {
						break;
					}
					playerId++;
				}
				mWantsToExchangePlayerId = playerId;
		        DialogFragment dialog = new ChoosePlayerDialogFragment();
		        dialog.show(getActivity().getSupportFragmentManager(), "ChoosePlayerDialogFragment");
				return true;
			}
			
		};		
		for (View playerView : mPlayer) {
			playerView.setOnLongClickListener(exchangePlayerListener);
		}
		OnClickListener nextFinisherListener = new OnClickListener() {

			@Override
			public void onClick(View v) {
				int playerId = TichuGame.PLAYER_ONE_ID;
				for (View player : mPlayer) {
					if (v == player) {
						break;
					}
					playerId++;
				}
				TichuGameDetailFragment.this.setNextFinisher(playerId);
				TichuGameDetailFragment.this.onRoundDataChange();
			}
			
		};
		for (View playerView : mPlayer) {
			playerView.setOnClickListener(nextFinisherListener);
		}
		OnClickListener tichuBidChangeListener = new OnClickListener() {

			@Override
			public void onClick(View v) {
				int playerId = TichuGame.PLAYER_ONE_ID;
				for (View playerBid : mPlayerTichu) {
					if (v == playerBid) {
						break;
					}
					playerId++;
				}
				TichuBidType type = (mBids[playerId - TichuGame.PLAYER_ONE_ID] == TichuBidType.NONE) ? TichuBidType.SMALL : 
					((mBids[playerId - TichuGame.PLAYER_ONE_ID] == TichuBidType.SMALL) ? TichuBidType.BIG : TichuBidType.NONE);
				TichuGameDetailFragment.this.setTichuBid(playerId, type, true);
				TichuGameDetailFragment.this.onRoundDataChange();
			}
			
		};
		for (View playerBid : mPlayerTichu) {
			playerBid.setOnClickListener(tichuBidChangeListener);
		}
		getView().findViewById(R.id.roundsList).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				selectRound(-1);
			}
			
		});
	}
	
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		selectRound(position);
	}
	
	// Create the menu based on the XML definition
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.tichu_game_detail, menu);
		menu.findItem(R.id.game_detail_option_show_delta).setChecked(getPreferencesShowDelta());
		menu.findItem(R.id.game_detail_option_show_tichus).setChecked(getPreferencesShowTichus());
	}
	
	// Reaction to the menu selection
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.game_detail_option_show_delta:
			boolean newState = !item.isChecked();
			item.setChecked(newState);
			setPreferencesShowDelta(newState);
			return true;
		case R.id.game_detail_option_show_tichus:
			newState = !item.isChecked();
			item.setChecked(newState);
			setPreferencesShowTichus(newState);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	private boolean getPreferencesShowDelta() {
		SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
		return sharedPref.getBoolean(PREFERENCES_SHOW_DELTA, TichuGameRoundAdapter.PREFERENCE_SHOW_DELTA_DEFAULT);
	}
	
	private void setPreferencesShowDelta(boolean show) {
		mAdapter.setShowDelta(show);
		SharedPreferences.Editor editor = getActivity().getPreferences(Context.MODE_PRIVATE).edit();
		editor.putBoolean(PREFERENCES_SHOW_DELTA, show);
		editor.commit();
	}
	
	private void setPreferencesShowTichus(boolean show) {
		mAdapter.setShowTichus(show);
		SharedPreferences.Editor editor = getActivity().getPreferences(Context.MODE_PRIVATE).edit();
		editor.putBoolean(PREFERENCES_SHOW_TICHUS, show);
		editor.commit();
	}
	
	private boolean getPreferencesShowTichus() {
		SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
		return sharedPref.getBoolean(PREFERENCES_SHOW_TICHUS, TichuGameRoundAdapter.PREFERENCE_SHOW_TICHUS_DEFAULT);
	}
	
	private String getBluetoothDeviceName() {
		if (BluetoothAdapter.getDefaultAdapter() != null) {
			return BluetoothAdapter.getDefaultAdapter().getName();
		} else {
			return "";
		}
	}
	private void createNewGame(PlayerDuo first, PlayerDuo second, boolean useMercyRule, int scoreLimit) {
		if (scoreLimit < TichuGame.MIN_SCORE_LIMIT || scoreLimit > TichuGame.MAX_SCORE_LIMIT) {
			mGame = new TichuGame(TichuGame.DEFAULT_SCORE_LIMIT, useMercyRule);
		} else {
			mGame = new TichuGame(scoreLimit, useMercyRule);
		}
		mGame.setOriginData(getBluetoothDeviceName(), Build.MODEL);
		mGame.setupPlayers(first.getFirst(), first.getSecond(), second.getFirst(),
				second.getSecond());

		fillData();
		mStateMachine.updateUI();
	}

	private void loadGame(long gameId, Bundle saveInstanceState) {
		assert Game.isValidId(gameId);
		List<Game> games = null;
		try {
			games = TichuGame.loadGames(getActivity().getContentResolver(), GameStorageHelper.getUri(GameKey.TICHU, gameId), true);
		} catch (CompressedDataCorruptException e) {
			games = null;
		}
		if (games != null && games.size() > 0) {
			assert games.size() == 1;
			mGame = (TichuGame) games.get(0);
			// this variable must never ever be changed, consider it to be final
			mIsImmutable = (saveInstanceState == null) ? (LOADED_FINISHED_GAMES_ARE_IMMUTABLE ? mGame.isFinished() : false)
					: saveInstanceState.getBoolean(STORAGE_IS_IMMUTABLE);
			if (mGame.isFinished()) {
				mLastRunningTimeUpdate = null; // so we do not update the running time anymore when loading a finished game
			}
			fillData();
			mStateMachine.updateUI();
		} else {
			Toast.makeText(getActivity(), getResources().getString(R.string.tichu_game_failed_loading), Toast.LENGTH_LONG).show();
			mCloseRequestListener.closeDetailView(true); // nothing to show here
		}
	}

	// invoked when a new game is created, the game can or cannot have data to
	// visualize, this selects a new round
	private void fillData() {
		assert mGame != null;
		synchPlayerNames();
		fillRoundData();
		selectRound(-1);
	}
	
	private void fillRoundData() {
		if (mAdapter == null) {
			mAdapter = new TichuGameRoundAdapter(mGame, getActivity(), R.layout.tichu_round, mGame.getRounds(), 
					getPreferencesShowDelta(), getPreferencesShowTichus());
			setListAdapter(mAdapter); 
			getListView().setSelection(mAdapter.getCount() - 1 ); // by default, scroll to bottom of round list
		}
		mAdapter.notifyDataSetChanged();
	}

	private void selectRound(int index) {
		if (index == -1) {
			// deselect round and make user able to input new one
			// this must also work when round is already deselected as it could get called more than once
			mCurrRound = null;
			mCurrRoundIndex = -1;
			mAdapter.setMarkedRow(mCurrRoundIndex);
			makeUserInputAvailable(!mGame.isFinished());
			setScoreSilent(true, "");
			setScoreSilent(false, "");
			for (int i = TichuGame.PLAYER_ONE_ID; i < TichuGame.PLAYER_ONE_ID
					+ TichuGame.TOTAL_PLAYERS; i++) {
				setTichuBid(i, TichuBidType.NONE, true);
			}
			clearFinishers();
			mStateMachine.updateUI();
		} else {
			// existing round that can be edited
			makeUserInputAvailable(true);
			mCurrRoundIndex = index;
			mAdapter.setMarkedRow(mCurrRoundIndex);
			mCurrRound = (TichuRound) mGame.getRound(index);
			setScoreSilent(true, String.valueOf(mCurrRound.getRawScoreTeam1()));
			setScoreSilent(false, String.valueOf(mCurrRound.getRawScoreTeam2()));
			int[] tempFin = new int[4];
			for (int i = 0; i < tempFin.length; i++) {
				tempFin[i] = TichuRound.FINISHER_POS_UNKNOWN;
			}
			for (int i = TichuGame.PLAYER_ONE_ID; i < TichuGame.PLAYER_ONE_ID
					+ TichuGame.TOTAL_PLAYERS; i++) {
				setTichuBid(i, mCurrRound.getTichuBid(i).getType(), true);
				int pos = mCurrRound.getFinisherPos(i);
				if (pos != TichuRound.FINISHER_POS_UNKNOWN) {
					tempFin[pos - 1] = i;
				}
			}
			clearFinishers();
			for (int i = 0; i < tempFin.length; i++) {
				if (tempFin[i] != TichuRound.FINISHER_POS_UNKNOWN) {
					setNextFinisher(tempFin[i]);
				}
			}
			mStateMachine.updateUI();
		}
	}

	private void setScoreSilent(boolean setTeam1Text, CharSequence text) {
		if (text == null) {
			return;
		}
		EditText view = setTeam1Text ? mScoreTeam1 : mScoreTeam2;
		TextWatcher watcher = setTeam1Text ? mScoreTeam1Watcher : mScoreTeam2Watcher;
		if (watcher != null) {
			view.removeTextChangedListener(watcher);
		}
		view.setText(text);
		if (view.hasFocus()) {
			view.setSelection(text.length());
		}
		if (watcher != null) {
			view.addTextChangedListener(watcher);
		}
	}
	
	public int getScore(boolean ofTeam1) {
		String text = ofTeam1 ? mScoreTeam1.getText().toString() : mScoreTeam2.getText().toString();
		if (text.length() == 0) {
			return TichuRound.UNKNOWN_SCORE;
		}
		try {
			return Integer.parseInt(text);
		} catch (NumberFormatException nfe) {
			return TichuRound.UNKNOWN_SCORE;
		}
	}

	private void setTichuBid(int playerId, TichuBidType type, boolean silent) {
		if (type == null) {
			return;
		}
		ImageButton view = mPlayerTichu[playerId - TichuGame.PLAYER_ONE_ID];
		mBids[playerId - TichuGame.PLAYER_ONE_ID] = type;
		view.setImageResource(type == TichuBidType.NONE ? TICHU_BID_NONE_DRAWABLE_ID :
			(type == TichuBidType.SMALL ? TICHU_BID_SMALL_DRAWABLE_ID : TICHU_BID_BIG_DRAWABLE_ID));
	}
	
	private void clearFinishers() {
		// clear from end, not important but so we unroll the process
		for (int i = TichuGame.PLAYER_FOUR_ID; i >= TichuGame.PLAYER_ONE_ID; i--) {
			visualizeFinisherPos(i, TichuRound.FINISHER_POS_UNKNOWN);
		}
		mFinisher.clear();
	}
	
	private void setNextFinisher(int playerId) {
		assert playerId >= TichuGame.PLAYER_ONE_ID && playerId < TichuGame.PLAYER_ONE_ID + TichuGame.TOTAL_PLAYERS;
		if (mFinisher.contains(Integer.valueOf(playerId))) {
			// clear finishers and let user newly select
			clearFinishers();
		} else {
			// check if currently is a 200:0 situation, then one of the other players got clicked (since finishers did not contain given id)
			if (mFinisher.size() == 2) {
				if ((TichuRound.isInTeam(mFinisher.get(0), true) && TichuRound.isInTeam(mFinisher.get(1), true))
					|| (TichuRound.isInTeam(mFinisher.get(0), false) && TichuRound.isInTeam(mFinisher.get(1), false))) {
					clearFinishers();
				}
			}
			mFinisher.add(Integer.valueOf(playerId));
			visualizeFinisherPos(playerId, mFinisher.size());
		}
	}

	private void visualizeFinisherPos(int playerId, int pos) {
		assert playerId >= TichuGame.PLAYER_ONE_ID && playerId < TichuGame.PLAYER_ONE_ID + TichuGame.TOTAL_PLAYERS;
		assert pos == TichuRound.FINISHER_POS_UNKNOWN || (pos > 0 && pos <= TichuRound.FINISHER_POS_LAST);
		boolean clear = pos == TichuRound.FINISHER_POS_UNKNOWN;
		String posString = clear ? "" : String.valueOf(pos) + ".";
		Button button = mPlayer[playerId - TichuGame.PLAYER_ONE_ID];
		String buttonText;
		switch(playerId) {
		case TichuGame.PLAYER_ONE_ID:
			buttonText = posString+ mGame.getTeam1().getFirst();
			break;
		case TichuGame.PLAYER_TWO_ID:
			buttonText = posString+ mGame.getTeam1().getSecond();
			break;
		case TichuGame.PLAYER_THREE_ID:
			buttonText = posString+ mGame.getTeam2().getFirst();
			break;
		case TichuGame.PLAYER_FOUR_ID:
			buttonText = posString+ mGame.getTeam2().getSecond();
			break;
		default:
			throw new IllegalArgumentException("Invalid player id " + playerId);
		}
		button.setText(buttonText);
		if (clear) {
			button.getBackground().clearColorFilter();
		} else if (button.getBackground() != null) {
			button.getBackground().setColorFilter(PLAYER_FINISHER_POS_COLOR[pos - 1], PorterDuff.Mode.LIGHTEN);
		}
	}

	private void makeUserInputAvailable(boolean available) {
		if (available && mIsImmutable && LOADED_FINISHED_GAMES_ARE_IMMUTABLE) {
			// sorry dude, you cannot edit a loaded finished game which got locked forever
			available = false;
		}
		mScoreTeam1.setEnabled(available);
		mScoreTeam2.setEnabled(available);
		for (int i = 0; i < mPlayer.length; i++) {
			mPlayer[i].setEnabled(available);
			mPlayerTichu[i].setEnabled(available);
		}
		// unavailable input must guarantee that applyRoundData cannot get invoked (which is the only place a TichuRound gets created newly)
	}
	
	private void saveAndClose() {
		if (getDisplayedGameId() == Game.NO_ID) {
			saveState();
		}
		mCloseRequestListener.closeDetailView(false);
	}
	
	private void onRoundDataChange() {
		// if score was changed then the other score will be cleared and we will get one UNKNOWN_SCORE
		int scoreTeam1 = getScore(true);
		int scoreTeam2 = getScore(false);
		// try to guess the other team score only on the knowledge of the entered score
		if (scoreTeam1 == TichuRound.UNKNOWN_SCORE) {
			scoreTeam1 = TichuRound.getEstimatedOtherTeamScore(scoreTeam2);	
			if (TichuRound.isValidScore(scoreTeam1)) {
				setScoreSilent(true, String.valueOf(scoreTeam1));
			}
		} else if (scoreTeam2 == TichuRound.UNKNOWN_SCORE) {
			scoreTeam2 = TichuRound.getEstimatedOtherTeamScore(scoreTeam1);
			if (TichuRound.isValidScore(scoreTeam2)) {
				setScoreSilent(false, String.valueOf(scoreTeam2));
			}
		}
		// if there is a finisher we can even try to build a TichuRound (though it might still fail if for example team1score=0, and only finisher is player 3
		if (mFinisher != null && mFinisher.size() >= MIN_GIVEN_FINISHER_COUNT_TO_SAVE_ROUND) {
			int[] finishersAr = new int[mFinisher.size()];
			for (int i = 0; i < mFinisher.size(); i++) {
				finishersAr[i] = mFinisher.get(i);
			}
			TichuRound newRound = TichuRound.buildRound(scoreTeam1, scoreTeam2, finishersAr, mBids);
			if (newRound != null) {
				// round successfully created, now fill up the fields as a created round may change fields 
				setScoreSilent(true, String.valueOf(newRound.getRawScoreTeam1()));
				setScoreSilent(false, String.valueOf(newRound.getRawScoreTeam2()));
				for (int i = TichuGame.PLAYER_ONE_ID;  i < TichuGame.PLAYER_ONE_ID + TichuGame.TOTAL_PLAYERS; i++) {
					visualizeFinisherPos(i, newRound.getFinisherPos(i));
				}
				mCurrRound = newRound;
				// no need to revisualize tichus as they do not get changed when a round is created
			}
		} else {
			mCurrRound = null;
		}
		mStateMachine.updateUI();
	}

	private class TichuGameStateMachine {
		private static final int STATE_GAME_CLOSABLE = 0;
		private static final int STATE_EXPECT_DATA = 1;
		private static final int STATE_ADD_ROUND= 2;
		private static final int STATE_OLD_ROUND_EDITED = 3;
		private static final int STATE_OLD_ROUND_SELECTED= 4;
		
		private int mState;
		private void determineState() {
			if (mCurrRoundIndex == -1) {
				// no round selected
				if (mCurrRound != null) {
					mState = STATE_ADD_ROUND;
				} else if (!mGame.isFinished()) {
					mState = STATE_EXPECT_DATA;
				} else {
					mState = STATE_GAME_CLOSABLE;
				}
			} else {
				// round selected
				if (mCurrRound == null || (mCurrRound != null && mCurrRound.equals(mGame.getRound(mCurrRoundIndex)))) {
					// no changes made or not enough to be able to save the changes
					if (!mGame.isFinished()) {
						mState = STATE_OLD_ROUND_SELECTED;
					} else {
						mState = STATE_GAME_CLOSABLE;
					}
				} else {
					mState = STATE_OLD_ROUND_EDITED;
				}
			}
		}
		
		private int getLockInButtonDrawableId() {
			switch(mState) {
			case STATE_GAME_CLOSABLE:
				return R.drawable.ic_menu_close_clear_cancel;
			case STATE_EXPECT_DATA:
				return R.drawable.ic_menu_view;
			case STATE_ADD_ROUND:
				return R.drawable.ic_menu_add;
			case STATE_OLD_ROUND_EDITED:
				return R.drawable.ic_menu_edit;
			case STATE_OLD_ROUND_SELECTED:
				return R.drawable.ic_menu_close_clear_cancel;
			default:
				throw new IllegalStateException();
			}
		}
		
		private String getStateText() {
			StringBuilder roundScore = new StringBuilder(20);	
			if (mCurrRound != null) {
				roundScore.append(' ');
				roundScore.append(mCurrRound.getScoreTeam1(mGame.usesMercyRule()));
				roundScore.append(':');
				roundScore.append(mCurrRound.getScoreTeam2(mGame.usesMercyRule()));
			}
			StringBuilder gameScore = new StringBuilder(20);
			gameScore.append(' ');
			gameScore.append(mGame.getScoreTeam1());
			gameScore.append(':');
			gameScore.append(mGame.getScoreTeam2());
			switch(mState) {
			case STATE_GAME_CLOSABLE:
				return getResources().getString(R.string.tichu_game_state_game_closable) + gameScore.toString();
			case STATE_EXPECT_DATA:
				return getResources().getString(R.string.tichu_game_state_expect_data);
			case STATE_ADD_ROUND:
				return getResources().getString(R.string.tichu_game_state_add_round) + roundScore.toString();
			case STATE_OLD_ROUND_EDITED:
				return getResources().getString(R.string.tichu_game_state_old_round_edited) + roundScore.toString();
			case STATE_OLD_ROUND_SELECTED:
				return getResources().getString(R.string.tichu_game_state_new_round);
			default:
				throw new IllegalStateException();
			}
		}
		
		private boolean lockinButtonEnabled() {
			return mState != STATE_EXPECT_DATA;
		}
		
		private void onLockinButtonPressed() {
			switch(mState) {
			case STATE_GAME_CLOSABLE:
				saveAndClose();
				break;
			case STATE_EXPECT_DATA:
				assert false; // button should not be available when game not finished and no valid round given
				break;
			case STATE_ADD_ROUND:
				mGame.addRound(mCurrRound);
				fillRoundData();
				selectRound(-1);
				break;
			case STATE_OLD_ROUND_EDITED:
				mGame.updateRound(mCurrRoundIndex, mCurrRound);
				fillRoundData();
				selectRound(-1);
				break;
			case STATE_OLD_ROUND_SELECTED:
				selectRound(-1);
				break;
			default:
				throw new IllegalStateException();
			}
		}
		
		private CharSequence getInfoText() {
			StringBuilder builder = new StringBuilder();
			builder.append(GameKey.getGameName(GameKey.TICHU));
			if (mGame.usesMercyRule()) {
				builder.append(' ');
				builder.append(getResources().getString(R.string.tichu_game_mery_rule_short));
			}
			if (mGame.isFinished()) {
				List<Player> winners = mGame.getWinner().getPlayers();
				builder.append(' ');
				builder.append(getResources().getString(R.string.game_winner));
				builder.append(": ");
				builder.append(winners.get(0));
				builder.append('+');
				builder.append(winners.get(1));
			} else {
				builder.append(' ');
				builder.append(mGame.getScoreTeam1());
				builder.append(':');
				builder.append(mGame.getScoreTeam2());
			}
			if (mGame.getScoreLimit() != TichuGame.DEFAULT_SCORE_LIMIT) {
				builder.append(' ');
				builder.append(getResources().getString(R.string.tichu_game_score_limit_short));
				builder.append(' ');
				builder.append(mGame.getScoreLimit());
			}
			return builder.toString();
		}
		
		private void updateUI() {
			determineState();
			mStatusText.setText(getStateText());
			mInfoText.setText(getInfoText());
			mLockIn.setEnabled(lockinButtonEnabled());
			mLockIn.setImageResource(getLockInButtonDrawableId());
		}
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		saveState();
		if (mGame == null) {
			return;
		}
		outState.putLong(GameStorageHelper.getCursorItemType(GameKey.TICHU),
				mGame.getId());
		outState.putBoolean(STORAGE_IS_IMMUTABLE, mIsImmutable);
		outState.putInt(STORAGE_SELECTED_ROUND, mCurrRoundIndex);
		if (mCurrRoundIndex == -1) {
			// if there is no round selected, lets store the data so that it does not get lost for example at screen orientation change
			outState.putString(STORAGE_TEXT_SCORE_TEAM1, mScoreTeam1.getText().toString());
			outState.putString(STORAGE_TEXT_SCORE_TEAM2, mScoreTeam2.getText().toString());
			if (mFinisher.size() > 0) {
				Compressor cmp = new Compressor(mFinisher.size());
				for (int i : mFinisher) {
					cmp.appendData(i);
				}
				outState.putString(STORAGE_FINISHERS_LIST, cmp.compress());
			}
			Compressor cmp = new Compressor(mBids.length);
			for (TichuBidType t : mBids) {
				cmp.appendData(t.getKey());
			}
			outState.putString(STORAGE_BIDS_LIST, cmp.compress());
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		saveState();
	}
	
	void saveState() {
		if (mGame != null) {
			if (mLastRunningTimeUpdate != null) {
				Date currTime = new Date();
				mGame.addRunningTime(currTime.getTime() - mLastRunningTimeUpdate.getTime());
				mLastRunningTimeUpdate = currTime;
			}
			if (mGame.isFinished()) {
				mLastRunningTimeUpdate = null;
			}
			mGame.saveGame(getActivity().getContentResolver());
		}
	}
	
	@Override
	public void onDestroyView() {
        getListView().setAdapter(null);
		super.onDestroyView();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
		getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
	}

	public long getDisplayedGameId() {
		return mGame == null ? Game.NO_ID : mGame.getId();
	}

	public void onRenameSuccess(Player newPlayer, String oldName) {
		mGame.onRenameSuccess(newPlayer, oldName);
	}

	@Override
	public PlayerPool getPool() {
		return GameKey.getPool(GameKey.TICHU);
	}

	@Override
	public List<Player> toFilter() {
		List<Player> filter = new LinkedList<Player>();
		filter.addAll(mGame.getTeam1().getPlayers());
		filter.addAll(mGame.getTeam2().getPlayers());
		return filter;
	}

	@Override
	public void playerChosen(Player chosen) {
		if (chosen == null || mGame.getTeam1().contains(chosen) || mGame.getTeam2().contains(chosen)) {
			return;
		}
		switch (mWantsToExchangePlayerId) {
		case TichuGame.PLAYER_ONE_ID:
			mGame.setupPlayers(chosen, mGame.getTeam1().getSecond(), mGame.getTeam2().getFirst(), mGame.getTeam2().getSecond());
			break;
		case TichuGame.PLAYER_TWO_ID:
			mGame.setupPlayers(mGame.getTeam1().getFirst(), chosen, mGame.getTeam2().getFirst(), mGame.getTeam2().getSecond());
			break;
		case TichuGame.PLAYER_THREE_ID:
			mGame.setupPlayers(mGame.getTeam1().getFirst(), mGame.getTeam1().getSecond(), chosen, mGame.getTeam2().getSecond());
			break;
		case TichuGame.PLAYER_FOUR_ID:
			mGame.setupPlayers(mGame.getTeam1().getFirst(), mGame.getTeam1().getSecond(), mGame.getTeam2().getFirst(), chosen);
			break;
		}
		synchPlayerNames();
	}

}