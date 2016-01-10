package dan.dit.gameMemo.appCore.doppelkopf;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
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
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;
import dan.dit.gameMemo.R;
import dan.dit.gameMemo.appCore.GameDetailFragment;
import dan.dit.gameMemo.gameData.game.Game;
import dan.dit.gameMemo.gameData.game.GameKey;
import dan.dit.gameMemo.gameData.game.OriginBuilder;
import dan.dit.gameMemo.gameData.game.doppelkopf.DoppelkopfBid;
import dan.dit.gameMemo.gameData.game.doppelkopf.DoppelkopfExtraScore;
import dan.dit.gameMemo.gameData.game.doppelkopf.DoppelkopfGame;
import dan.dit.gameMemo.gameData.game.doppelkopf.DoppelkopfRe;
import dan.dit.gameMemo.gameData.game.doppelkopf.DoppelkopfRound;
import dan.dit.gameMemo.gameData.game.doppelkopf.DoppelkopfRoundResult;
import dan.dit.gameMemo.gameData.game.doppelkopf.DoppelkopfRoundStyle;
import dan.dit.gameMemo.gameData.game.doppelkopf.DoppelkopfSolo;
import dan.dit.gameMemo.gameData.player.ChoosePlayerDialogFragment;
import dan.dit.gameMemo.gameData.player.Player;
import dan.dit.gameMemo.gameData.player.PlayerPool;
import dan.dit.gameMemo.storage.GameStorageHelper;
import dan.dit.gameMemo.util.compaction.CompactedDataCorruptException;

public class DoppelkopfGameDetailFragment extends GameDetailFragment {
	public static final boolean LOADED_FINISHED_GAMES_ARE_IMMUTABLE = true;
	private static final int RE_PARTY = 0;
	private static final int CONTRA_PARTY = 1;
	private static final int[] PARTIES = new int[] {RE_PARTY, CONTRA_PARTY};
	private static final int PARTIES_COUNT = PARTIES.length;
	public static final String EXTRAS_NEW_GAME_PLAYERS = "dan.dit.gameMemo.NEW_GAME_PLAYERS"; // String[]
	public static final String EXTRA_NEW_GAME_DUTY_SOLI_PER_PLAYER = "dan.dit.gameMemo.NEW_GAME_DUTY_SOLI"; // int
	public static final String EXTRA_NEW_GAME_ROUNDS_LIMIT = "dan.dit.gameMemo.NEW_GAME_ROUNDS_LIMIT"; // int
	private static final String STORAGE_IS_IMMUTABLE = "dan.dit.gameMemo.STORAGE_IS_IMMUTABLE"; // boolean
	private static final int RE_ICON_ID = R.drawable.kreuzdame;
	private static final int GIVER_ICON_ID = R.drawable.kartenstapel;
	private static final int[] FOX_IDS = new int[] {R.drawable.fuchs_0, R.drawable.fuchs_1, R.drawable.fuchs_2};
	private static final int[] CARL_IDS = new int[] {R.drawable.karl_0, R.drawable.karl_1};
	private static final int[] DK_IDS = new int[] {R.drawable.dk_0, R.drawable.dk_1, R.drawable.dk_2, R.drawable.dk_3, R.drawable.dk_4};
	//private static final String PREFERENCES_SHOW_DELTA = "dan.dit.gameMemo.PREF_SHOW_DELTA"; // feature disabled
	public static final String EXTRA_RULE_SYSTEM = "dan.dit.gameMemo.EXTRA_RULE_SYSTEM"; // String
	
	private Drawable mReIcon;
	private Drawable mGiverIcon;
	private Button mGameStyle;
	private Button[] mPlayer;
	private TextView[] mPlayerInfo;
	private Button[] mBid;
	private Button[] mResult;
	private TextView[] mResultScore;
	private TextView[] mTotalResultScore;
	private ImageButton[] mExtraFox;
	private ImageButton[] mExtraDK;
	private ImageButton[] mExtraCharly;
	private EditText[] mExtraScore;
	private TextView mInfoText;
	private TextWatcher[] mExtraScoreWatcher;
	private TextView mStatusText;
	private ViewSwitcher mSwitcher;
	private ImageButton mMainAction;
	private MenuItem mLockItem;

	private DoppelkopfRound mCurrRound;
	private DoppelkopfGame mGame;
	private DoppelkopfGameStateMachine mStateMachine;
	public int mCurrRoundIndex = -1;
	private int mFirstReIndex = -1;
	private int mSecondReIndex = -1;
	private DoppelkopfGameRoundAdapter mAdapter;
	
	public static DoppelkopfGameDetailFragment newInstance(long gameId) {
		DoppelkopfGameDetailFragment f = new DoppelkopfGameDetailFragment();

        // Supply index input as an argument.
        Bundle args = new Bundle();
        args.putLong(GameStorageHelper.getCursorItemType(GameKey.DOPPELKOPF), gameId);
        f.setArguments(args);

        return f;
	}
	
	public static DoppelkopfGameDetailFragment newInstance(Bundle extras) {
		DoppelkopfGameDetailFragment f = new DoppelkopfGameDetailFragment();
        f.setArguments(extras);
        return f;
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View baseView = inflater.inflate(R.layout.doppelkopf_detail, null);
		mGiverIcon = getResources().getDrawable(GIVER_ICON_ID);
		mReIcon = getResources().getDrawable(RE_ICON_ID);
		return baseView;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState); 
		Window window = getActivity().getWindow();
		if (window != null) {
			window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
			window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
		}
		mGameStyle = (Button) getView().findViewById(R.id.doppelkopf_game_style);
		mInfoText = (TextView) getView().findViewById(R.id.doppelkopf_game_info);
		mPlayer = new Button[DoppelkopfGame.MAX_PLAYERS];
		mPlayer[0] = (Button) getView().findViewById(R.id.game_player1);
		mPlayer[1] = (Button) getView().findViewById(R.id.game_player2);
		mPlayer[2] = (Button) getView().findViewById(R.id.game_player3);
		mPlayer[3] = (Button) getView().findViewById(R.id.game_player4);
		mPlayer[4] = (Button) getView().findViewById(R.id.game_player5);
		mPlayerInfo = new TextView[DoppelkopfGame.MAX_PLAYERS];
		mPlayerInfo[0] = (TextView) getView().findViewById(R.id.game_player1_info);
		mPlayerInfo[1] = (TextView) getView().findViewById(R.id.game_player2_info);
		mPlayerInfo[2] = (TextView) getView().findViewById(R.id.game_player3_info);
		mPlayerInfo[3] = (TextView) getView().findViewById(R.id.game_player4_info);
		mPlayerInfo[4] = (TextView) getView().findViewById(R.id.game_player5_info);
		mStatusText = (TextView) getView().findViewById(R.id.doppelkopf_game_status);
		mSwitcher = (ViewSwitcher) getView().findViewById(R.id.switcher);
		mBid = new Button[PARTIES_COUNT];
		mBid[RE_PARTY] = (Button) getView().findViewById(R.id.re_bid);
		mBid[CONTRA_PARTY] = (Button) getView().findViewById(R.id.contra_bid);
		mResult = new Button[PARTIES_COUNT];
		mResult[RE_PARTY] = (Button) getView().findViewById(R.id.re_result);
		mResult[CONTRA_PARTY] = (Button) getView().findViewById(R.id.contra_result);
		mResultScore = new TextView[PARTIES_COUNT];
		mResultScore[RE_PARTY] = (TextView) getView().findViewById(R.id.re_result_score);
		mResultScore[CONTRA_PARTY] = (TextView) getView().findViewById(R.id.contra_result_score);
		mTotalResultScore = new TextView[PARTIES_COUNT];
		mTotalResultScore[RE_PARTY] = (TextView) getView().findViewById(R.id.re_result_total_score);
		mTotalResultScore[CONTRA_PARTY] = (TextView) getView().findViewById(R.id.contra_result_total_score);
		mExtraFox = new ImageButton[PARTIES_COUNT];
		mExtraFox[RE_PARTY] = (ImageButton) getView().findViewById(R.id.re_extra_fox);
		mExtraFox[CONTRA_PARTY] = (ImageButton) getView().findViewById(R.id.contra_extra_fox);
		mExtraDK = new ImageButton[PARTIES_COUNT];
		mExtraDK[RE_PARTY] = (ImageButton) getView().findViewById(R.id.re_extra_dk);
		mExtraDK[CONTRA_PARTY] = (ImageButton) getView().findViewById(R.id.contra_extra_dk);
		mExtraCharly = new ImageButton[PARTIES_COUNT];
		mExtraCharly[RE_PARTY] = (ImageButton) getView().findViewById(R.id.re_extra_charly);
		mExtraCharly[CONTRA_PARTY] = (ImageButton) getView().findViewById(R.id.contra_extra_charly);
		mExtraScore = new EditText[PARTIES_COUNT];
		mExtraScore[RE_PARTY] = (EditText) getView().findViewById(R.id.re_extra_score);
		mExtraScore[CONTRA_PARTY] = (EditText) getView().findViewById(R.id.contra_extra_score);
		mMainAction = (ImageButton) getView().findViewById(R.id.main_action);
		mStateMachine = new DoppelkopfGameStateMachine();
		loadOrStartGame(savedInstanceState);
		registerForContextMenu(getListView());
		initListeners();
		handleRestorageOfUnsavedUserInput(savedInstanceState);
		GameKey.applySleepBehavior(GameKey.DOPPELKOPF, getActivity());
	}
	
	private void initListeners() {
		mMainAction.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				mStateMachine.onActionButtonPressed();
			}
		});
		mGameStyle.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				nextGameRoundStyle();
			}
		});
		OnLongClickListener exchangePlayerListener = new OnLongClickListener() {

			@Override
			public boolean onLongClick(View v) {
				if (isImmutable()) {
					return false;
				}
				int clickedIndex = 0;
				for (int i = 0; i < mPlayer.length; i++) {
					if (v == mPlayer[i]) {
						clickedIndex = i;
						break;
					}
				}
		        DialogFragment dialog = ChoosePlayerDialogFragment.newInstance(clickedIndex, null, false, false);
		        dialog.show(getActivity().getSupportFragmentManager(), "ChoosePlayerDialogFragment");
				return true;
			}
			
		};	
		OnClickListener selectReListener = new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				int clickedIndex = 0;
				for (int i = 0; i < mPlayer.length; i++) {
					if (v == mPlayer[i]) {
						clickedIndex = i;
						break;
					}
				}
				if (mFirstReIndex == clickedIndex) {
					setFirstReIndex(DoppelkopfRoundStyle.NO_INDEX);
				} else if (mSecondReIndex == clickedIndex) {
					setSecondReIndex(DoppelkopfRoundStyle.NO_INDEX);
				} else if (DoppelkopfRoundStyle.isValidIndex(mFirstReIndex)) {
					setSecondReIndex(clickedIndex);
				} else {
					setFirstReIndex(clickedIndex);
				}
			}
		};
		for (int playerIndex = 0; playerIndex < mPlayer.length; playerIndex++) {
			mPlayer[playerIndex].setOnLongClickListener(exchangePlayerListener);
			mPlayer[playerIndex].setOnClickListener(selectReListener);
		}
		mExtraScoreWatcher = new TextWatcher[PARTIES_COUNT];
		for (int party : PARTIES) {
			// extra score
			mExtraScoreWatcher[party] = new TextWatcher() {
				
				@Override
				public void onTextChanged(CharSequence s, int start, int before, int count) {
				}
				@Override
				public void beforeTextChanged(CharSequence s, int start, int count,
						int after) {}
				@Override
				public void afterTextChanged(Editable s) {
					if (mCurrRound == null) {
						return; //ignore
					}
					boolean isReText = this == mExtraScoreWatcher[RE_PARTY];
					int party = isReText ? RE_PARTY : CONTRA_PARTY;
					DoppelkopfExtraScore extra = mCurrRound.getExtraScore();
					Integer value = 0;
					try {
						value = Integer.parseInt(mExtraScore[party].getText().toString());
						if (value != extra.getExtraScore(isReText)) {
							setOtherExtraScoreCount(party, value - extra.getExtraScore(isReText) + extra.getOtherExtraScore(isReText));
						}
					} catch (NumberFormatException nfe) {
					}
				}
			};
			mExtraScore[party].addTextChangedListener(mExtraScoreWatcher[party]);
			
			mExtraCharly[party].setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					if (mCurrRound == null) {
						return; // ignore
					}
					boolean isRe = v == mExtraCharly[RE_PARTY];
					setCharlyCount(isRe ? RE_PARTY : CONTRA_PARTY, mCurrRound.getExtraScore().getCharlyCount(isRe) + 1);
				}
			});
			mExtraDK[party].setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					if (mCurrRound == null) {
						return; //ignore
					}
					boolean isRe = v == mExtraDK[RE_PARTY];
					setDoppelkopfCount(isRe ? RE_PARTY : CONTRA_PARTY, mCurrRound.getExtraScore().getDoppelkopfCount(isRe) + 1);
				}
			});
			mExtraFox[party].setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					if (mCurrRound == null) {
						return; //ignore 
					}
					boolean isRe = v == mExtraFox[RE_PARTY];
					setFoxCount(isRe ? RE_PARTY : CONTRA_PARTY, mCurrRound.getExtraScore().getFoxCount(isRe) + 1);
				}
			});
			mBid[party].setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					boolean isRe = v == mBid[RE_PARTY];
					nextBid(isRe);
				}
			});
			mResult[party].setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					boolean isRe = v == mResult[RE_PARTY];
					nextResult(isRe ? RE_PARTY : CONTRA_PARTY);
				}
				
			});
		}
		
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Window window = getActivity().getWindow();
		if (window != null) {
			window.clearFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
			window.clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
			window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		}
	}
	
	// Create the menu based on the XML definition
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.doppelkopf_game_detail, menu);
		//menu.findItem(R.id.game_detail_option_show_delta).setChecked(getPreferencesShowDelta()); // feature disabled
		mLockItem = menu.findItem(R.id.lock_game);
		applyLock();
	}

	
	// Reaction to the menu selection
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		/*case R.id.game_detail_option_show_delta: // features disabled (menu button removed) as I and others never used this (games are too long for a rematch, delta not interesting)
			boolean newState = !item.isChecked();
			item.setChecked(newState);
			setPreferencesShowDelta(newState);
			return true;
		case R.id.game_detail_action_rematch:
			saveCloseAndRematch();
			return true;*/
		case R.id.lock_game:
		    toggleLocked();
		    return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	private boolean getPreferencesShowDelta() {
	    return DoppelkopfGameRoundAdapter.PREFERENCE_SHOW_DELTA_DEFAULT;
		//SharedPreferences sharedPref = getActivity().getSharedPreferences(Game.PREFERENCES_FILE, Context.MODE_PRIVATE); // feature disabled
		//return sharedPref.getBoolean(PREFERENCES_SHOW_DELTA, DoppelkopfGameRoundAdapter.PREFERENCE_SHOW_DELTA_DEFAULT);
	}
	
	// feature disabled
	/*private void setPreferencesShowDelta(boolean show) {
		mAdapter.setShowDelta(show);
		SharedPreferences.Editor editor = getActivity().getSharedPreferences(Game.PREFERENCES_FILE, Context.MODE_PRIVATE).edit();
		editor.putBoolean(PREFERENCES_SHOW_DELTA, show);
		ActivityUtil.commitOrApplySharedPreferencesEditor(editor);
	}*/
	   
    private void applyLock() {
        if (mLockItem == null || mGame == null) {
            return;
        }
        if (mGame.isLocked()) {
            mLockItem.setVisible(true);
            mLockItem.setTitle(R.string.unlock_game);
            mLockItem.setIcon(R.drawable.ic_menu_locked_inverse);
            mLockItem.setEnabled(mGame.getRuleSystem().isLockStateAcceptable(mGame, false));
        } else if (mGame.getRuleSystem().isLockStateAcceptable(mGame, true)) {
            if (mLastRunningTimeUpdate == null) {
                mLastRunningTimeUpdate = new Date(); // if the game is unlocked, we start counting the run time again
            }
            mLockItem.setVisible(true);
            mLockItem.setEnabled(true);
            mLockItem.setTitle(R.string.lock_game);
            mLockItem.setIcon(R.drawable.ic_menu_unlocked_inverse); 
        } else {
            mLockItem.setVisible(false);
        }
    }
    
	private void loadOrStartGame(Bundle savedInstanceState) {
		// Check from the saved Instance
		long gameId = (savedInstanceState == null) ? Game.NO_ID : savedInstanceState
				.getLong(GameStorageHelper.getCursorItemType(GameKey.DOPPELKOPF), Game.NO_ID);

		// Or passed from the other activity
		List<Player> players = new ArrayList<Player>(DoppelkopfGame.MAX_PLAYERS);
		Bundle extras = getArguments();
		if (extras != null) {
			long extraId = extras
					.getLong(GameStorageHelper.getCursorItemType(GameKey.DOPPELKOPF), Game.NO_ID);
			if (Game.isValidId(extraId)) {
				gameId = extraId;
			} else if (!Game.isValidId(gameId)) {
				// no id given, but maybe an uri?
				Uri uri = extras.getParcelable(GameStorageHelper.getCursorItemType(GameKey.DOPPELKOPF));
				gameId = GameStorageHelper.getIdOrStarttimeFromUri(uri);
			}
			String[] playerNames = extras.getStringArray(EXTRAS_NEW_GAME_PLAYERS);
			if (playerNames != null) {
				for (String name : playerNames) {
					if (Player.isValidPlayerName(name) && players.size() < DoppelkopfGame.MAX_PLAYERS) {
						players.add(DoppelkopfGame.PLAYERS.populatePlayer(name));
					}
				}
			}
		}

		mLastRunningTimeUpdate = new Date();
		if (Game.isValidId(gameId)) {
			loadGame(gameId, savedInstanceState);
		} else if (players != null && players.size() >= DoppelkopfGame.MIN_PLAYERS) {
			int dutySoliPerPlayer = extras != null ? extras.getInt(EXTRA_NEW_GAME_DUTY_SOLI_PER_PLAYER, DoppelkopfGame.DEFAULT_DUTY_SOLI) : DoppelkopfGame.DEFAULT_DUTY_SOLI;
			int roundsLimit = extras != null ? extras.getInt(EXTRA_NEW_GAME_ROUNDS_LIMIT, DoppelkopfGame.DEFAULT_DURCHLAEUFE) : DoppelkopfGame.DEFAULT_DURCHLAEUFE;
			createNewGame(players, roundsLimit, dutySoliPerPlayer, extras.getString(EXTRA_RULE_SYSTEM));
		} else {
			mCallback.closeDetailView(true, false);
			return;
		}
		if (mGame != null && mGame.getPlayers().size() > DoppelkopfGame.MIN_PLAYERS) {
			mPlayer[4].setVisibility(View.VISIBLE);
			mPlayerInfo[4].setVisibility(View.VISIBLE);
		}
		applyGiverAndInactive();
	}
	
	@Override
	public PlayerPool getPool() {
		return DoppelkopfGame.PLAYERS;
	}

	@Override
	public List<Player> toFilter() {
		return mGame.getPlayers();
	}

	@Override
	public void playerChosen(int playerIndex, Player chosen) {
		if (chosen == null || mGame == null || playerIndex < 0 || mGame.getPlayers().contains(chosen)) {
			return;
		}
		List<Player> players = mGame.getPlayers();
		players.remove(playerIndex);
		players.add(playerIndex, chosen);
		mGame.setupPlayers(players);
		updatePlayerNames();
	}

	@Override
	public void playerRemoved(int arg, Player removed) {
	    throw new UnsupportedOperationException();
	}
	
	@Override
	public void onPlayerColorChanged(int arg, Player concernedPlayer) {
		// player color not used
	}

	@Override
	public void setInfoText(CharSequence main, CharSequence extra) {
		if (mInfoText.getVisibility() != View.VISIBLE) {
			mInfoText.setVisibility(View.VISIBLE);
		}
		mInfoText.setText(main + " " + extra);
	}
	
	protected boolean isImmutable() {
		return (mGame != null && mGame.isLocked()) || (LOADED_FINISHED_GAMES_ARE_IMMUTABLE && mIsLoadedFinishedGame);
	}
	
	private void toggleLocked() {
	    mGame.setLocked(!mGame.isLocked());
	    if (mGame.isLocked()) {
	        deselectRound();
	        makeUserInputAvailable(false);
	    } else if (!mGame.isFinished()) {
	        mIsLoadedFinishedGame = false;
	        makeUserInputAvailable(true);
	    }
	    applyLock();
	}
	
	private void makeUserInputAvailable(boolean available) {
		if (available && isImmutable()) {
			// sorry dude, you cannot edit a loaded finished game which got locked forever
			available = false;
		}
		for (int i = 0; i < mPlayer.length; i++) {
			mPlayer[i].setEnabled(available);
		}
		mGameStyle.setEnabled(available);
		for (int party : PARTIES) {
			mBid[party].setEnabled(available);
			mExtraCharly[party].setEnabled(available);
			mExtraDK[party].setEnabled(available);
			mExtraFox[party].setEnabled(available);
			mExtraScore[party].setEnabled(available);
			mResult[party].setEnabled(available);
		}
	}
	
	private void hideSoftKeyboard() {
		InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(
			      Context.INPUT_METHOD_SERVICE);
		if (imm != null) {
			imm.hideSoftInputFromWindow(mExtraScore[RE_PARTY].hasFocus() ? mExtraScore[RE_PARTY].getWindowToken() : mExtraScore[CONTRA_PARTY].getWindowToken(), 0);
		}
	}
	
	private void createNewGame(List<Player> players, int durchlauefe, int dutySoliPerPlayer, String ruleSysName) {
		mGame = new DoppelkopfGame(ruleSysName, durchlauefe, dutySoliPerPlayer);
        mGame.setOriginData(OriginBuilder.getInstance().getOriginHints());
		mGame.setupPlayers(players);
		fillData();
		mStateMachine.updateUI();
	}

	private void loadGame(long gameId, Bundle saveInstanceState) {
		assert Game.isValidId(gameId);
		List<Game> games = null;
		try {
			games = Game.loadGames(GameKey.DOPPELKOPF, getActivity().getContentResolver(), GameStorageHelper.getUriWithId(GameKey.DOPPELKOPF, gameId), true);
		} catch (CompactedDataCorruptException e) {
		    Log.e("Doppelkopf", "Compacted data corrupt: " + e.getCorruptData() + " : " + e.toString() + " : " + games);
			games = null;
		}
		if (games != null && games.size() > 0) {
			assert games.size() == 1;
			mGame = (DoppelkopfGame) games.get(0);
			// this variable must never ever be changed, consider it to be final
			mIsLoadedFinishedGame = (saveInstanceState == null) ? (LOADED_FINISHED_GAMES_ARE_IMMUTABLE ? mGame.isFinished() : false)
					: saveInstanceState.getBoolean(STORAGE_IS_IMMUTABLE);
			if (mGame.isFinished()) {
				mLastRunningTimeUpdate = null; // so we do not update the running time anymore when loading a finished game
			}
			fillData();
			mStateMachine.updateUI();
		} else {
			Toast.makeText(getActivity(), getResources().getString(R.string.game_failed_loading), Toast.LENGTH_LONG).show();
			mCallback.closeDetailView(true, false); // nothing to show here
		}
	}

	// invoked when a new game is created, the game can or cannot have data to
	// visualize, this selects a new round
	private void fillData() {
		assert mGame != null;
		updatePlayerNames();
		fillRoundData();
		deselectRound();
		applyLock();
	}
	
	private void updatePlayerNames() {
		// invoked at start from fillData and when a new player is chosen to replace an old one
		List<Player> players = mGame.getPlayers();
		int index = 0;
		for (Player p : players) {
			mPlayer[index++].setText(p.getShortenedName(Player.SHORT_NAME_LENGTH));
		}
	}
	
	private void fillRoundData() {
		if (mAdapter == null) {
			mAdapter = new DoppelkopfGameRoundAdapter(mGame, getActivity(), R.layout.doppelkopf_round, mGame.getRounds(), getPreferencesShowDelta());
			setListAdapter(mAdapter); 
			getListView().setSelection(mAdapter.getCount() - 1 ); // by default, scroll to bottom of round list
		}
		mAdapter.notifyDataSetChanged();
	}
	
	private void updateScores() {
		if (mExtraScore != null && mExtraScoreWatcher != null) {
			mExtraScore[RE_PARTY].removeTextChangedListener(mExtraScoreWatcher[RE_PARTY]);
			mExtraScore[CONTRA_PARTY].removeTextChangedListener(mExtraScoreWatcher[CONTRA_PARTY]);
		}
		if (mCurrRound != null) {
			for (int party : PARTIES) {
				// extra scores
				int count = mCurrRound.getExtraScore().getExtraScore(party == RE_PARTY);
				mExtraScore[party].setText(Integer.toString(count));
				// score by bid and result
				count = mGame.getRuleSystem().getBidAndResultScore(party == RE_PARTY, mCurrRound);
				mResultScore[party].setText(Integer.toString(count));
				// total score
				count = mGame.getRuleSystem().getTotalScore(party == RE_PARTY, mCurrRound);
				mTotalResultScore[party].setText(Integer.toString(count));
			}
		} else {
			for (int party : PARTIES) {
				mExtraScore[party].setText(Integer.toString(0));		
				mResultScore[party].setText(Integer.toString(0));	
				mTotalResultScore[party].setText(Integer.toString(0));	
			}
		}
		if (mExtraScore != null && mExtraScoreWatcher != null) {
			mExtraScore[RE_PARTY].addTextChangedListener(mExtraScoreWatcher[RE_PARTY]);
			mExtraScore[CONTRA_PARTY].addTextChangedListener(mExtraScoreWatcher[CONTRA_PARTY]);
		}
		mStateMachine.updateUI();
	}
	
	private void nextGameRoundStyle() {
		if (mCurrRound != null) {
			mCurrRound.getRoundStyle().setNextType();
			int roundIndexOfInterest = mCurrRoundIndex != -1 ? mCurrRoundIndex : mGame.getRoundCount();
			if (mCurrRound.isSolo() && !((DoppelkopfSolo) mCurrRound.getRoundStyle()).isValidDutySolo() && mGame.enforcesDutySolo(roundIndexOfInterest)) {
				mCurrRound.getRoundStyle().setNextType();
			} else if (!mCurrRound.isSolo() && mGame.enforcesDutySolo(roundIndexOfInterest)) {
				assert false; // if there are two re parties even though a solo is enforces
				setSecondReIndex(DoppelkopfRoundStyle.NO_INDEX);
				nextGameRoundStyle();
				return;
			}
			mStateMachine.updateUI();
		}
		updateGameStyle();
	}
	
	private void updateGameStyle() {
		if (mCurrRound == null) {
			mGameStyle.setText(getString(R.string.doppelkopf_game_style_normal));
		} else {
			mGameStyle.setText(mCurrRound.getRoundStyle().getNameResource());
		}
	}

	private void applyRePlayers() {
		if (DoppelkopfRoundStyle.isValidIndex(mSecondReIndex) && !DoppelkopfRoundStyle.isValidIndex(mFirstReIndex)) {
			mFirstReIndex = mSecondReIndex;
			mSecondReIndex = DoppelkopfRoundStyle.NO_INDEX;
		}
		boolean deselect = false;
		if (mCurrRound == null) {
			mCurrRound = DoppelkopfRound.makeRound(mGame.getRuleSystem(), mFirstReIndex, mSecondReIndex);
		} else {
			boolean firstValid = DoppelkopfRoundStyle.isValidIndex(mFirstReIndex);
			boolean secondValid = DoppelkopfRoundStyle.isValidIndex(mSecondReIndex);
			if (mCurrRound.isSolo() &&  firstValid && secondValid) {
				mCurrRound.setRoundStyle(new DoppelkopfRe(DoppelkopfRe.DEFAULT_RE_STYLE, mFirstReIndex, mSecondReIndex));
			} else if (mCurrRound.isSolo() && firstValid) {
				mCurrRound.getRoundStyle().setFirstReIndex(mFirstReIndex);
			} else if (!mCurrRound.isSolo() && firstValid && secondValid) {
				mCurrRound.setRoundStyle(new DoppelkopfRe(mCurrRound.getRoundStyle().getType(), mFirstReIndex, mSecondReIndex));
			} else if (!mCurrRound.isSolo() && firstValid) {
				mCurrRound.setRoundStyle(new DoppelkopfSolo(DoppelkopfSolo.DEFAULT_SOLO_TYPE, mFirstReIndex));
			} else {
				deselect = true;
			}
		}
		
		if (deselect) {
			deselectRound();
		}
	}
	
	public void setFirstReIndex(int index) {
		if ((DoppelkopfRoundStyle.isValidIndex(index) && index == mSecondReIndex) || !mGame.isPlayerActive(index, mCurrRoundIndex != -1 ? mCurrRoundIndex : mGame.getRoundCount())) {
			return; // do nothing
		}
		if (DoppelkopfRoundStyle.isValidIndex(mFirstReIndex)) {
			// clear old
			mPlayerInfo[mFirstReIndex].setCompoundDrawablesWithIntrinsicBounds(null, null, mPlayerInfo[mFirstReIndex].getCompoundDrawables()[2], null);
			mFirstReIndex = DoppelkopfRoundStyle.NO_INDEX;
		}
		if (DoppelkopfRoundStyle.isValidIndex(index)) {
			mFirstReIndex = index;
			mPlayerInfo[mFirstReIndex].setCompoundDrawablesWithIntrinsicBounds(mReIcon, null, mPlayerInfo[mFirstReIndex].getCompoundDrawables()[2], null);
		}
		applyRePlayers();
		updateGameStyle();
		updateScores();
	}
	
	public void setSecondReIndex(int index) {
		if ((DoppelkopfRoundStyle.isValidIndex(index) && index == mFirstReIndex) 
				|| !mGame.isPlayerActive(index, mCurrRoundIndex != -1 ? mCurrRoundIndex : mGame.getRoundCount())) {
			return; // do nothing
		}
		if (!DoppelkopfRoundStyle.isValidIndex(mFirstReIndex)) {
			setFirstReIndex(index);
			return;
		}
		if (DoppelkopfRoundStyle.isValidIndex(mSecondReIndex)) {
			// clear old
			mPlayerInfo[mSecondReIndex].setCompoundDrawablesWithIntrinsicBounds(null, null, mPlayerInfo[mSecondReIndex].getCompoundDrawables()[2], null);
			mSecondReIndex = DoppelkopfRoundStyle.NO_INDEX;
		}
		if (DoppelkopfRoundStyle.isValidIndex(index)) {
			if (mGame.enforcesDutySolo(mCurrRoundIndex != -1 ? mCurrRoundIndex : mGame.getRoundCount())) {
				setFirstReIndex(index);
				return;
			}
			mSecondReIndex = index;
			mPlayerInfo[mSecondReIndex].setCompoundDrawablesWithIntrinsicBounds(mReIcon, null, mPlayerInfo[mSecondReIndex].getCompoundDrawables()[2], null);
		}
		applyRePlayers();
		updateGameStyle();
		updateScores();
	}
	
	private void setOtherExtraScoreCount(int party, int count) {
		if (mCurrRound != null) {
			DoppelkopfExtraScore score = mCurrRound.getExtraScore();
			score.setOtherExtraScore(party == RE_PARTY, count);
		}
		updateExtras();
		updateScores();
	}

	private void updateExtras() {
		if (mCurrRound != null) {
			int count = 0;
			for (int party : PARTIES) {
				count = mCurrRound.getExtraScore().getDoppelkopfCount(party == RE_PARTY);
				mExtraDK[party].setImageResource(DK_IDS[count]);
				count = mCurrRound.getExtraScore().getFoxCount(party == RE_PARTY);
				mExtraFox[party].setImageResource(FOX_IDS[count]);
				count = mCurrRound.getExtraScore().getCharlyCount(party == RE_PARTY);
				mExtraCharly[party].setImageResource(CARL_IDS[count]);
			}
		} else {
			for (int party : PARTIES) {
				mExtraDK[party].setImageResource(DK_IDS[0]);
				mExtraFox[party].setImageResource(FOX_IDS[0]);
				mExtraCharly[party].setImageResource(CARL_IDS[0]);
			}			
		}
	}
	
	private void setDoppelkopfCount(int party, int pCount) {
		if (mCurrRound != null) {
			int count = pCount % (DoppelkopfExtraScore.getMaxAmount(DoppelkopfExtraScore.DOPPELKOPF) + 1);
			DoppelkopfExtraScore score = mCurrRound.getExtraScore();
			while (score.getDoppelkopfCount(party == RE_PARTY) != count) {
				score.nextDoppelkopf(party == RE_PARTY);
			}
		}
		updateExtras();
		updateScores();
	}

	private void setFoxCount(int party, int pCount) {
		if (mCurrRound != null) {
			int count = pCount % (DoppelkopfExtraScore.getMaxAmount(DoppelkopfExtraScore.FOX_CAUGHT) + 1);
			DoppelkopfExtraScore score = mCurrRound.getExtraScore();
			while (score.getFoxCount(party == RE_PARTY) != count) {
				score.nextFox(party == RE_PARTY);
			}
		}
		updateExtras();
		updateScores();
	}

	private void setCharlyCount(int party, int pCount) {
		if (mCurrRound != null) {
			int count = pCount % (DoppelkopfExtraScore.getMaxAmount(DoppelkopfExtraScore.CHARLY) + 1);
			DoppelkopfExtraScore score = mCurrRound.getExtraScore();
			while (score.getCharlyCount(party == RE_PARTY) != count) {
				score.nextCharly(party == RE_PARTY);
			}
		}
		updateExtras();
		updateScores();
	}

	private void nextBid(boolean re) {
		if (mCurrRound != null) {
			if (re) {
				mCurrRound.getReBid().nextBid();
			} else {
				mCurrRound.getContraBid().nextBid();
			}
			setBid(re ? RE_PARTY : CONTRA_PARTY, re ? mCurrRound.getReBid().getType() : mCurrRound.getContraBid().getType());
		}
	}
	
	private void setBid(int party, int type) {
		String name = "";
		switch (type) {
		case DoppelkopfBid.NONE:
			name = getString(R.string.doppelkopf_bid_no_bid); break;
		case DoppelkopfBid.RE_CONTRA:
			name = party == RE_PARTY ? getString(R.string.doppelkopf_bid_re) : getString(R.string.doppelkopf_bid_contra); break;
		case DoppelkopfBid.OHNE_90:
			name = getString(R.string.doppelkopf_bid_ohne_90); break;
		case DoppelkopfBid.OHNE_60:
			name = getString(R.string.doppelkopf_bid_ohne_60); break;
		case DoppelkopfBid.OHNE_30:
			name = getString(R.string.doppelkopf_bid_ohne_30); break;
		case DoppelkopfBid.SCHWARZ:
			name = getString(R.string.doppelkopf_bid_black); break;
		}
		mBid[party].setText(name);
		if (mCurrRound != null) {
			mCurrRound.setBid(party == RE_PARTY, type);
		}
		updateScores();
	}
	
	private void nextResult(int party) {
		if (mCurrRound != null) {
			mCurrRound.getRoundResult().improve(party == RE_PARTY);
			setResult(mCurrRound.getRoundResult());
		}
	}
	
	private void setResult( DoppelkopfRoundResult res) {
		if (mCurrRound != null) {
			mCurrRound.setResult(res);
		}
		mResult[RE_PARTY].setText(res.getNameResource(true));
		mResult[CONTRA_PARTY].setText(res.getNameResource(false));
		updateScores();
	}
	
	private void applyGiverAndInactive() {
		if (mGame != null) {
			int roundIndex = mCurrRoundIndex != -1 ? mCurrRoundIndex : mGame.getRoundCount();
			int giver = mGame.getGiver(roundIndex);
			for (int i = 0; i < mGame.getPlayerCount(); i++) {
				if (!mGame.isPlayerActive(i, roundIndex)) {
					mPlayer[i].setEnabled(false);
				}
				mPlayerInfo[i].setCompoundDrawablesWithIntrinsicBounds(mPlayerInfo[i].getCompoundDrawables()[0], null, i == giver ? mGiverIcon : null, null);
			}
		}
	}
	
	@Override
	protected void deselectRound() {
		// deselect round and make user able to input new one
		// this must also work when round is already deselected as it could get called more than once
		mCurrRound = null;
		mCurrRoundIndex = -1;
		getListView().clearChoices();
		getListView().requestLayout();
		makeUserInputAvailable(!mGame.isFinished());
		applyGiverAndInactive();
		// clear views from round specific data
		setSecondReIndex(-1);
		setFirstReIndex(-1);
		mGameStyle.setText(getString(R.string.doppelkopf_game_style_normal));
		setResult(new DoppelkopfRoundResult(DoppelkopfRoundResult.R_120));
		for (int party : PARTIES) {
			setBid(party, DoppelkopfBid.NONE);
			setCharlyCount(party, 0);
			setFoxCount(party, 0);
			setDoppelkopfCount(party, 0);
			setOtherExtraScoreCount(party, 0);
		}
		mStateMachine.updateUI();
		applyLock();
	}

	protected void selectRound(int index) {
		// existing round (that can be edited)
		makeUserInputAvailable(true);
		mCurrRoundIndex = index;
		getListView().setItemChecked(mCurrRoundIndex, true);
		mCurrRound = ((DoppelkopfRound) mGame.getRound(index)).makeCopy();
		// fill views with info
		applyGiverAndInactive();
		int second = mCurrRound.getRoundStyle().getSecondIndex();
		int oldGameType = mCurrRound.getRoundStyle().getType();
		setFirstReIndex(mCurrRound.getRoundStyle().getFirstIndex());
		setSecondReIndex(second);
		mCurrRound.getRoundStyle().setType(oldGameType);
		updateGameStyle();
		setResult(mCurrRound.getRoundResult());
		for (int party : PARTIES) {
			boolean re = party == RE_PARTY;
			setBid(party, re ? mCurrRound.getReBid().getType() : mCurrRound.getContraBid().getType());
			setCharlyCount(party, mCurrRound.getExtraScore().getCharlyCount(re));
			setFoxCount(party, mCurrRound.getExtraScore().getFoxCount(re));
			setDoppelkopfCount(party, mCurrRound.getExtraScore().getDoppelkopfCount(re));
			setOtherExtraScoreCount(party, mCurrRound.getExtraScore().getOtherExtraScore(re));
		}
		mStateMachine.updateUI();
	}
	
	@Override
	public void onPause() {
		super.onPause();
		saveState();
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		saveState();
		if (mGame == null) {
			return;
		}
		// could save user input to restore, but since most obvious case (orientation change) is 
		// not happening, we are fine
	}
	
	private void handleRestorageOfUnsavedUserInput(Bundle savedInstanceState) {
		if (savedInstanceState == null) {
			return;
		}
		// could restore user input here, see onSaveInstanceState
	}
	
	private class DoppelkopfGameStateMachine {
		private static final int STATE_DETAIL_HIDDEN = 0;
		private static final int STATE_EXPECT_DATA = 1;
		private static final int STATE_ADD_ROUND= 2;
		private static final int STATE_OLD_ROUND_EDITED = 3;
		private static final int STATE_OLD_ROUND_SELECTED = 4;
		
		private int mState;
		
		private void determineState() {
			if (mCurrRound == null && mFirstReIndex == -1) {
				mState = STATE_DETAIL_HIDDEN;
			} else if (mCurrRound == null && mFirstReIndex != -1) {
				mState = STATE_EXPECT_DATA;
			} else {
				assert mCurrRound != null;
				if (mCurrRoundIndex == -1) {
					mState = STATE_ADD_ROUND;
				} else if (mGame.getRound(mCurrRoundIndex).equals(mCurrRound)) {
					mState = STATE_OLD_ROUND_SELECTED;
				} else {
					mState = STATE_OLD_ROUND_EDITED;
				}
			}
		}

		private int getActionButtonDrawableId() {
			switch(mState) {
			case STATE_DETAIL_HIDDEN:
				return 0;
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

			switch(mState) {
			case STATE_DETAIL_HIDDEN:
				return ""; 
			case STATE_EXPECT_DATA:
				return getResources().getString(R.string.doppelkopf_game_state_expect_data);
			case STATE_ADD_ROUND:
				return getResources().getString(R.string.doppelkopf_game_state_add_round) + roundScore.toString();
			case STATE_OLD_ROUND_EDITED:
				return getResources().getString(R.string.doppelkopf_game_state_old_round_edited) + roundScore.toString();
			case STATE_OLD_ROUND_SELECTED:
				return getResources().getString(R.string.doppelkopf_game_state_new_round);
			default:
				throw new IllegalStateException();
			}
		}
		
		private boolean actionButtonEnabled() {
			return mState != STATE_DETAIL_HIDDEN && mState != STATE_EXPECT_DATA;
		}
		
		private void onActionButtonPressed() {
			switch(mState) {
			case STATE_DETAIL_HIDDEN:
				/* fall through */
			case STATE_EXPECT_DATA:
				assert false; // button should not be available 
				break;
			case STATE_OLD_ROUND_SELECTED:
				deselectRound();
				hideSoftKeyboard();
				break;
			case STATE_ADD_ROUND:
				mGame.addRound(mCurrRound);
				fillRoundData();
				deselectRound();
				hideSoftKeyboard();
				break;
			case STATE_OLD_ROUND_EDITED:
				mGame.updateRound(mCurrRoundIndex, mCurrRound);
				fillRoundData();
				deselectRound();
				hideSoftKeyboard();
				break;
			default:
				throw new IllegalStateException();
			}
		}
		
		private CharSequence getMainInfoText() {
			StringBuilder builder = new StringBuilder(20);
			builder.append(GameKey.getGameName(GameKey.DOPPELKOPF, getResources()));
			if (mGame.hasLimit()) {
				builder.append(' ');
				builder.append(Math.min((int ) mGame.getDurchlauf() + 1, mGame.getLimit()));
				builder.append('/');
				builder.append(mGame.getLimit());
			}
			return builder.toString();
		}
		
		private CharSequence getExtraInfoText() {
			StringBuilder builder = new StringBuilder();
			int remainingSoli = mGame.getRemainingSoli(mGame.getRoundCount());
			int totalSoli = mGame.getDutySoliCountPerPlayer() * mGame.getPlayerCount();
			if (remainingSoli > 0) {
				builder.append(getResources().getString(R.string.doppelkopf_remaining_soli_info, totalSoli - remainingSoli, totalSoli));
			}
			return builder.toString();
		}
		
		private void updateUI() {
			determineState();
			if (mState == STATE_DETAIL_HIDDEN) {
				if (mSwitcher.getDisplayedChild() == 0) {
					mSwitcher.showPrevious();
				}
			} else {
				if (mSwitcher.getDisplayedChild() == 1) {
					mSwitcher.showNext();	
				}
			}
			mStatusText.setText(getStateText());
			mCallback.setInfo(getMainInfoText(), getExtraInfoText());
			mMainAction.setEnabled(actionButtonEnabled());
			mMainAction.setImageResource(getActionButtonDrawableId());
		}
	}

	@Override
	protected Game getGame() {
		return mGame;
	}

    public boolean hasSelectedRound() {
        return mStateMachine == null || mStateMachine.mState != DoppelkopfGameStateMachine.STATE_DETAIL_HIDDEN;
    }

    @Override
    protected int getGameKey() {
        return GameKey.DOPPELKOPF;
    }

}
