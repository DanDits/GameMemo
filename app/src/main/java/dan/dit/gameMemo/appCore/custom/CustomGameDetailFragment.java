package dan.dit.gameMemo.appCore.custom;

import java.lang.ref.WeakReference;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.DialogFragment;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.TextView.BufferType;
import android.widget.Toast;
import dan.dit.gameMemo.R;
import dan.dit.gameMemo.appCore.GameDetailFragment;
import dan.dit.gameMemo.appCore.gameSetup.GameSetupActivity;
import dan.dit.gameMemo.appCore.gameSetup.TeamSetupTeamsController;
import dan.dit.gameMemo.appCore.numberInput.AbsoluteOperation;
import dan.dit.gameMemo.appCore.numberInput.ClearOperation;
import dan.dit.gameMemo.appCore.numberInput.ConstructiveNumberOperation;
import dan.dit.gameMemo.appCore.numberInput.CustomOperation;
import dan.dit.gameMemo.appCore.numberInput.MinusOperation;
import dan.dit.gameMemo.appCore.numberInput.NewOperation;
import dan.dit.gameMemo.appCore.numberInput.NumberInputController;
import dan.dit.gameMemo.appCore.numberInput.OnOperationClickedListener;
import dan.dit.gameMemo.appCore.numberInput.Operation;
import dan.dit.gameMemo.appCore.numberInput.OperationListener;
import dan.dit.gameMemo.appCore.numberInput.PlusOperation;
import dan.dit.gameMemo.appCore.numberInput.SelectListenerOperation;
import dan.dit.gameMemo.gameData.game.Game;
import dan.dit.gameMemo.gameData.game.GameKey;
import dan.dit.gameMemo.gameData.game.OriginBuilder;
import dan.dit.gameMemo.gameData.game.custom.CustomGame;
import dan.dit.gameMemo.gameData.game.custom.CustomRound;
import dan.dit.gameMemo.gameData.player.AbstractPlayerTeam;
import dan.dit.gameMemo.gameData.player.ChoosePlayerDialogFragment;
import dan.dit.gameMemo.gameData.player.Player;
import dan.dit.gameMemo.gameData.player.PlayerPool;
import dan.dit.gameMemo.gameData.player.PlayerTeam;
import dan.dit.gameMemo.storage.GameStorageHelper;
import dan.dit.gameMemo.util.ActivityUtil;
import dan.dit.gameMemo.util.compaction.CompactedDataCorruptException;

public class CustomGameDetailFragment extends GameDetailFragment {
    private static final boolean CUSTOMGAMES_LOADED_FINISHED_GAMES_ARE_IMMUTABLE = false;
    private static final String CUSTOM_OPERATION_NEXT_ROUND_KEY = "next_round";
    private static final String CUSTOM_OPERATION_PREV_ROUND_KEY = "prev_round";
    private static final String CUSTOM_OPERATION_NEW_ROUND_KEY = "new_round";
    
    private ImageButton mNextRound;
    private ImageButton mPrevRound;
    private TextView mRoundTitle;
    private ViewGroup mRoundContainer;
    private GridView mScoresView;
    private TextView mMainInfo;
    private LinearLayout mInputContainer;
    private NumberInputController mInputController;
    private ProgressBar mDelayProgress;
    
    // state information about the game
    private CustomGame mGame;
    private int mSelectedRound;
    private TeamScoreAdapter mAdapter;
    private int mSelectedTeam;
    
    public static CustomGameDetailFragment newInstance(long gameId) {
        CustomGameDetailFragment f = new CustomGameDetailFragment();

        // Supply index input as an argument.
        Bundle args = new Bundle();
        args.putLong(GameStorageHelper.getCursorItemType(GameKey.CUSTOMGAME), gameId);
        f.setArguments(args);

        return f;
    }
    
    public static CustomGameDetailFragment newInstance(Bundle extras) {
        CustomGameDetailFragment f = new CustomGameDetailFragment();
        f.setArguments(extras);
        return f;
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View baseView = inflater.inflate(R.layout.custom_detail, null);
        return baseView;
    }
    
    @Override
    public void onResume() {
        super.onResume();
        updateInterface(); // in case there was a cleanup
    }
    
    @Override
    public void onStop() {
        super.onStop();
        if (mDelayedInputTimer != null) {
            onInputTimerTimeout();
        }
    }
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState); 
        Window window = getActivity().getWindow();
        if (window != null) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
            window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        }
        mPrevRound = (ImageButton) getView().findViewById(R.id.last_round);
        mNextRound = (ImageButton) getView().findViewById(R.id.next_round);
        mRoundTitle = (TextView) getView().findViewById(R.id.round_title);
        mRoundContainer = (ViewGroup) getView().findViewById(R.id.rounds_container);
        mScoresView = (GridView) getView().findViewById(R.id.scores_view);
        mMainInfo = (TextView) getView().findViewById(R.id.mainInfo);
        mInputContainer = (LinearLayout) getView().findViewById(R.id.score_input_container);
        mDelayProgress = (ProgressBar) getView().findViewById(R.id.inputDelayProgress);
        loadOrStartGame(savedInstanceState);
        initListeners();
        initInput();
        loadPreferences();
        GameKey.applySleepBehavior(GameKey.CUSTOMGAME, getActivity());
    }
    
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.customgame_game_detail, menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.settings:
            showSettings();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    private void showSettings() {
        // in case the timeout is changed we better end the timer immediately
        onInputTimerTimeout();
        // load components
        View root = getActivity().getLayoutInflater().inflate(R.layout.customgame_settings, null);
        final EditText gameName = (EditText) root.findViewById(R.id.gameName);
        final EditText gameDescr = (EditText) root.findViewById(R.id.gameDescription);
        final CheckBox highestWins = (CheckBox) root.findViewById(R.id.gameHighestWins);
        final EditText startScore = (EditText) root.findViewById(R.id.gameStartscore);
        final ImageButton gameFinished = (ImageButton) root.findViewById(R.id.gameFinished);
        final EditText newPlayerName = (EditText) root.findViewById(R.id.newPlayerName);
        final ImageButton newPlayerNameConfirm = (ImageButton) root.findViewById(R.id.addPlayer);
        final CheckBox autoselectNext = (CheckBox) root.findViewById(R.id.autoselectNext);
        final EditText inputDelayTime = (EditText) root.findViewById(R.id.inputDelayTime);
        // fill with game's data
        gameName.setText(mGame.getName());
        gameDescr.setText(mGame.getDescription());
        highestWins.setChecked(mGame.getHighestScoreWins());
        startScore.setText(Double.toString(mGame.getStartScore()));
        gameFinished.setTag(mGame.isFinished() ? Boolean.TRUE : Boolean.FALSE);
        gameFinished.setImageResource(mGame.isFinished() ? R.drawable.ic_menu_locked : R.drawable.ic_menu_unlocked);
        gameFinished.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View v) {
                if (Boolean.TRUE.equals(gameFinished.getTag())) {
                    gameFinished.setTag(Boolean.FALSE);
                    gameFinished.setImageResource(R.drawable.ic_menu_unlocked);
                } else {
                    gameFinished.setTag(Boolean.TRUE);
                    gameFinished.setImageResource(R.drawable.ic_menu_locked);
                }
            }
        });
        if (mGame.getTeamCount() < CustomGame.MAX_TEAMS) {
            newPlayerNameConfirm.setEnabled(true);
            newPlayerName.setEnabled(true);
            newPlayerNameConfirm.setOnClickListener(new OnClickListener() {
                
                @Override
                public void onClick(View v) {
                    String name = newPlayerName.getText().toString();
                    if (Player.isValidPlayerName(name) && mGame.getTeamCount() < CustomGame.MAX_TEAMS) {
                        List<AbstractPlayerTeam> teams = mGame.getTeams();
                        newPlayerName.setText("");
                        Player newPlayer = CustomGame.PLAYERS.populatePlayer(name);
                        boolean alreadyContained = false;
                        for (AbstractPlayerTeam team : teams) {
                            if (team.contains(newPlayer)) {
                                alreadyContained = true;
                            }
                        }
                        if (!alreadyContained) {
                            PlayerTeam newTeam = new PlayerTeam();
                            newTeam.addPlayer(newPlayer);
                            teams.add(newTeam);
                            mGame.setupTeams(teams);
                            mInputController.clearHistory();
                            onTeamNumberChange();
                            if (teams.size() >= CustomGame.MAX_TEAMS) {
                                newPlayerNameConfirm.setEnabled(false);
                                newPlayerName.setEnabled(false);                                
                            }
                        }
                    }
                }
            });
        } else {
            newPlayerNameConfirm.setEnabled(false);
            newPlayerName.setEnabled(false);
        }
        autoselectNext.setChecked(mAutoselectNext);
        inputDelayTime.setText(mAdapter.DOUBLE_FORMAT.format((((double) mDelayedInputDelayStart) / 1000.0)));
        
        // init dialog
        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
        alert.setView(root);
        alert.setIcon(R.drawable.ic_menu_manage)
        .setTitle(R.string.customgame_detail_settings_menu)
        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            //@Override
            public void onClick(DialogInterface dialog, int which) {
                // apply changes
                String name = gameName.getText().toString();
                if (!TextUtils.isEmpty(name)) {
                    mGame.setGameName(name);
                }
                mGame.setDescription(gameDescr.getText().toString());
                mGame.setHighestScoreWins(highestWins.isChecked());
                double score = mGame.getStartScore();
                try {
                    score = Double.parseDouble(startScore.getText().toString());
                } catch (NumberFormatException nfe) {}
                mGame.setStartScore(score);
                mGame.setFinished(Boolean.TRUE.equals(gameFinished.getTag()));
                setAutoselectNext(autoselectNext.isChecked());
                // delay
                double delay = DEFAULT_INPUT_DELAY / 1000.0;
                try {
                    delay = Double.parseDouble(inputDelayTime.getText().toString());
                } catch (NumberFormatException nfe) {}
                setDelay((int) (delay * 1000));
                updateInterface(); // especially needed when startvalue is changed
            }
        })
        .setNeutralButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        })
        .create()
        .show();
    }
    
    private void onTeamNumberChange() {
        mAdapter.notifyDataSetChanged();
        adaptInputListeners();
    }
    
    private synchronized void onInputTimerTimeout() {
        if (mDelayedInputTimer != null) {
            mDelayedInputDelayCurrent = 0;
            mDelayedInputTimer.cancel();
            mDelayedInputTimer = null;
            mDelayProgress.setProgress(mDelayProgress.getMax());
            onInputEnd();
        }
    }
    
    private void onInputEnd() {
        if (mAutoselectNext) {
            selectNextTeam();
        }
        if (!mGame.isRoundBased()) {
            updateInterface();
        }
    }

    private static final int DEFAULT_INPUT_DELAY = 1500; // ms
    private static final boolean DEFAULT_SELECT_NEXT = true;
    private static final String PREFERENCE_INPUT_DELAY = "dan.dit.gameMemo.INPUTDELAY";
    private static final String PREFERENCE_SELECT_NEXT = "dan.dit.gameMemo.SELECT_NEXT_TEAM";
    
    private Timer mDelayedInputTimer;
    private boolean mAutoselectNext;
    private int mDelayedInputDelayStart; // ms
    private int mDelayedInputDelayCurrent; // ms
    
    private void loadPreferences() {
        if (mGame == null) {
            mDelayedInputDelayStart = DEFAULT_INPUT_DELAY;
            mAutoselectNext = DEFAULT_SELECT_NEXT;
        } else {
            SharedPreferences pref = getActivity().getPreferences(Context.MODE_PRIVATE);
            mDelayedInputDelayStart = pref.getInt(PREFERENCE_INPUT_DELAY + mGame.getName(), DEFAULT_INPUT_DELAY);
            if (mDelayedInputDelayStart < 0) {
                mDelayedInputDelayStart = 0;
            }
            mAutoselectNext = pref.getBoolean(PREFERENCE_SELECT_NEXT + mGame.getName(), DEFAULT_SELECT_NEXT);
        }
    }
    
    private void setDelay(int delay) {
        mDelayedInputDelayStart = delay;
        if (mDelayedInputDelayStart < 0) {
            mDelayedInputDelayStart = 0;
        }
        SharedPreferences.Editor editor = getActivity().getPreferences(Context.MODE_PRIVATE).edit();
        editor.putInt(PREFERENCE_INPUT_DELAY + mGame.getName(), mDelayedInputDelayStart);
        ActivityUtil.commitOrApplySharedPreferencesEditor(editor);
    }
    
    private void setAutoselectNext(boolean selectNext) {
        mAutoselectNext = selectNext;
        SharedPreferences.Editor editor = getActivity().getPreferences(Context.MODE_PRIVATE).edit();
        editor.putBoolean(PREFERENCE_SELECT_NEXT + mGame.getName(), mAutoselectNext);
        ActivityUtil.commitOrApplySharedPreferencesEditor(editor);
    }
    
    private static class DelayedInputHandler extends Handler {
        private WeakReference<CustomGameDetailFragment> mFrag;
        
        public DelayedInputHandler(CustomGameDetailFragment frag) {
            mFrag = new WeakReference<CustomGameDetailFragment>(frag);
        }
        
        public void handleMessage(Message m) {
            CustomGameDetailFragment frag = mFrag.get();
            if (frag != null) {
                if (m.what == 0) {
                    frag.mDelayProgress.setProgress(frag.mDelayedInputDelayStart - frag.mDelayedInputDelayCurrent);                                      
                } else if (m.what == 1) {
                    frag.onInputTimerTimeout();                                      
                }
            }
        }
    }
    
    private void adaptInputListeners() {
        mInputController.removeAllListener();
        mInputController.addListener(new OnOperationClickedListener() {
            @Override
            public void onOperationClickedBeforeExecute(Operation op) {
                if (mDelayedInputTimer == null && (op instanceof ConstructiveNumberOperation) && !mGame.isRoundBased()) {
                    // prepare a round for input, we will take the round directly before the first NaN round for the team, so clear the NaN here
                    int index = mGame.getFirstNaNRound(mSelectedTeam);
                    CustomRound round;
                    if (index < mGame.getRoundCount()) {
                        round = (CustomRound) mGame.getRound(index);
                    } else {
                        round = mGame.createRound();
                    }
                    round.setValue(mSelectedTeam, ((ConstructiveNumberOperation) op).getStartElement());
                }
            }
            
            @Override
            public void onOperationClickedAfterExecute(Operation op) {
                if (op instanceof ConstructiveNumberOperation) {
                    // time to start the countdown to select nextteam
                    if (mDelayedInputTimer == null) {
                        final int MIN_PERIOD = 100; // > 0 in ms
                        final int UPDATE_PERIOD = 30;
                        if (mDelayedInputDelayStart < MIN_PERIOD) {
                            onInputEnd(); // just too short to even notice, so we just make this without a break
                        } else {
                            // now we really start the timer
                            mDelayProgress.setMax(mDelayedInputDelayStart);
                            mDelayProgress.setProgress(0);
                            final int PERIOD = Math.min(UPDATE_PERIOD, mDelayedInputDelayStart); // update every UPDATE_PERIOD ms or if the input delay is smaller only that period
                            mDelayedInputDelayCurrent = mDelayedInputDelayStart; // reset current delay
                            mDelayedInputTimer = new Timer();
                            final DelayedInputHandler uiHandler = new DelayedInputHandler(CustomGameDetailFragment.this);
                            mDelayedInputTimer.scheduleAtFixedRate(new TimerTask() {
    
                                @Override
                                public void run() {
                                    mDelayedInputDelayCurrent -= PERIOD;
                                    uiHandler.obtainMessage(0).sendToTarget();
                                    if (mDelayedInputDelayCurrent <= 0) {
                                        uiHandler.obtainMessage(1).sendToTarget();
                                    } 
                                }
                                
                            }, 0, PERIOD);
                        }
                    } else {
                        // reset current timer
                        mDelayedInputDelayCurrent = mDelayedInputDelayStart;
                    }
                } else {
                    // better abort countdown as Clear or other operation has been done
                    onInputTimerTimeout();
                }
                
            }
        });
        for (int pos = 0; pos < mGame.getTeamCount(); pos++)
            mInputController.addListener(new OperationListener(){
                private int mPosition;
                private SelectListenerOperation mSelectMe = new SelectListenerOperation(mInputController, this);
                public OperationListener setPosition(int pos) {
                    mPosition = pos;
                    return this;
                }
                @Override
                public SelectListenerOperation getSelectListenerOperation() {
                    return mSelectMe;
                }
        
                @Override
                public double getNumber() {
                    if (mGame.getTeamCount() > mPosition) {
                        if (mGame.isRoundBased()) {
                            return ((CustomRound) mGame.getRound(mSelectedRound)).getValue(mPosition);
                        } else {
                            int lastValidRound = Math.max(0, mGame.getFirstNaNRound(mSelectedTeam) - 1);
                            CustomRound round = (CustomRound) mGame.getRound(lastValidRound);
                            return round.getValue(mSelectedTeam);
                        }
                    } else {
                        return Double.NaN; // could occur when a player is removed and the operation is undone/redone or maybe in single round mode
                    }
                }
        
                @Override
                public boolean operationExecuted(double result, Operation op) {
                    if (mGame.getTeamCount() <= mPosition) {
                        // can occur when a player is removed and the operation is undone/redone
                        return false;
                    }
                    if (mGame.isRoundBased()) {
                        if (op instanceof ConstructiveNumberOperation) {
                            CustomRound curr = ((CustomRound) mGame.getRound(mSelectedRound));
                            curr.setValue(mPosition, result);
                            mAdapter.notifyDataSetChanged();
                        } else if (op instanceof ClearOperation) {
                            ((CustomRound) mGame.getRound(mSelectedRound)).setValue(mPosition, Double.NaN);
                            mGame.cleanUp();
                            mAdapter.notifyDataSetChanged();
                        } else if (op instanceof SelectListenerOperation) {
                        } else if (op instanceof CustomOperation) {
                            boolean isInverse = ((CustomOperation) op).isInverse();
                            String key = ((CustomOperation) op).getKey();
                            if (key.equals(CUSTOM_OPERATION_PREV_ROUND_KEY)) {
                                if (isInverse) {
                                    if (mSelectedRound == mGame.getRoundCount() - 1) {
                                        selectRound(0);
                                    } else {
                                        selectRound(mSelectedRound + 1);
                                    }
                                } else {
                                    if (mSelectedRound > 0) {
                                        selectRound(mSelectedRound - 1);
                                    } else {
                                        selectRound(mGame.getRoundCount() - 1); // cycle through - continue at the end when it reached the first bahn
                                    }
                                }
                            } else if (key.equals(CUSTOM_OPERATION_NEXT_ROUND_KEY)) {
                                if (isInverse) {
                                    selectRound(mSelectedRound - 1);
                                } else {
                                    selectRound(mSelectedRound + 1);                                    
                                }
                            } else if (key.equals(CUSTOM_OPERATION_NEW_ROUND_KEY)) {
                                if (isInverse) {
                                    mGame.removeRound(mGame.getRoundCount() - 1);
                                    selectRound(mGame.getRoundCount() - 1);
                                } else {
                                    mGame.createRound();
                                    selectRound(mGame.getRoundCount() - 1);
                                } 
                            }
                        }
                        
                    }else {
                        // not round based                           
                        if (op instanceof ConstructiveNumberOperation) {
                            int lastValidRound = Math.max(0, mGame.getFirstNaNRound(mSelectedTeam) - 1);
                            CustomRound round = (CustomRound) mGame.getRound(lastValidRound);
                            round.setValue(mPosition, result);
                            mAdapter.notifyDataSetChanged();
                        } else if (op instanceof ClearOperation) {
                            int roundIndex = mGame.getFirstNaNRound(mSelectedTeam) - 1;
                            if (roundIndex >= 0) {
                                ((CustomRound) mGame.getRound(roundIndex)).setValue(mSelectedTeam, Double.NaN);
                            }
                            mAdapter.notifyDataSetChanged();
                        }
                    }
                    updateInterface();
                    return true;
                    
                }
        
                @Override
                public boolean isActive() {
                    return mPosition == mSelectedTeam;
                }
        
                @Override
                public void setActive(boolean active) {
                    if (active) {
                        selectTeamExecute(mPosition);
                    }
                }
            }.setPosition(pos)
            );
        
    }
    
    private void initInput() {
        if (mGame != null) {
            mInputController = new NumberInputController(GameKey.CUSTOMGAME, getActivity(), mInputContainer, true, mGame.getName());
            mInputController.setSaveHistory(mGame.isRoundBased()); // for not round based games it is way too hard to remember what happened to properly undo stuff
            if (mInputController.getOperationsCount() == 0) {
                // if we have no controls, create default ones (so at least a NewOperation is given)
                mInputController.clearOperations();
                mInputController.newOperation(new ClearOperation(), 0);
                mInputController.newOperation(new AbsoluteOperation(0), 1);
                mInputController.newOperation(new PlusOperation(1), 2);
                mInputController.newOperation(new MinusOperation(1), 3);
                mInputController.newOperation(new NewOperation(mInputController, null, -1), 4);
                
            }
            adaptInputListeners();
        }
    }
    
    private void initListeners() {
        mPrevRound.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View v) {
                onPrevRoundClick();
            }
        });
        mNextRound.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View v) {
                onNextRoundClick();
            }
        });
    }
    
    private static final CustomOperation PREV_OP = new CustomOperation(CUSTOM_OPERATION_PREV_ROUND_KEY);
    private static final CustomOperation NEXT_OP = new CustomOperation(CUSTOM_OPERATION_NEXT_ROUND_KEY);
    private static final CustomOperation NEW_OP = new CustomOperation(CUSTOM_OPERATION_NEW_ROUND_KEY);
    private void onPrevRoundClick() {
        if (mGame.getRoundCount() > 1) {
            mInputController.executeCustomOperation(PREV_OP);
            selectTeam(0);
        }
    }
    private void onNextRoundClick() {
        if (mSelectedRound < mGame.getRoundCount() - 1) {
            mInputController.executeCustomOperation(NEXT_OP);
            selectTeam(0);
        } else {
            mInputController.executeCustomOperation(NEW_OP);
            selectTeam(0);
        }
    }
    
    private void loadOrStartGame(Bundle savedInstanceState) {
        // Check from the saved Instance
        long gameId = (savedInstanceState == null) ? Game.NO_ID : savedInstanceState
                .getLong(GameStorageHelper.getCursorItemType(GameKey.CUSTOMGAME), Game.NO_ID);

        Bundle extras = getArguments();
        if (extras != null) {
            long extraId = extras
                    .getLong(GameStorageHelper.getCursorItemType(GameKey.CUSTOMGAME), Game.NO_ID);
            if (Game.isValidId(extraId)) {
                gameId = extraId;
            } else if (!Game.isValidId(gameId)) {
                // no id given, but maybe an uri?
                Uri uri = extras.getParcelable(GameStorageHelper.getCursorItemType(GameKey.CUSTOMGAME));
                gameId = GameStorageHelper.getIdOrStarttimeFromUri(uri);
            }
        }

        mLastRunningTimeUpdate = new Date();
        if (Game.isValidId(gameId)) {
            loadGame(gameId, savedInstanceState);
        } else {
            // try to create a new game if enough information is given

            Bundle options = extras.getBundle(GameSetupActivity.EXTRA_OPTIONS_PARAMETERS);
            Bundle teamParameters = extras.getBundle(GameSetupActivity.EXTRA_TEAMS_PARAMETERS);
            List<AbstractPlayerTeam> teams = new LinkedList<AbstractPlayerTeam>();
            if (teamParameters != null) {
                String[] playerNames = teamParameters.getStringArray(TeamSetupTeamsController.EXTRA_PLAYER_NAMES);
                int[] teamColors = teamParameters.getIntArray(TeamSetupTeamsController.EXTRA_TEAM_COLORS);
                String[] teamNames = teamParameters.getStringArray(TeamSetupTeamsController.EXTRA_TEAM_NAMES);
                PlayerTeam currTeam = null;
                int playerCount = 0;
                int teamIndex = -1;
                for (String playerName : playerNames) {
                    if (currTeam == null || playerCount >= CustomGame.MAX_PLAYER_PER_TEAM) {
                        currTeam = new PlayerTeam();
                        playerCount = 0;
                        teamIndex++;
                        teams.add(currTeam);
                        currTeam.setColor(teamColors[teamIndex]);
                        currTeam.setTeamName(teamNames[teamIndex]);
                    }
                    if (Player.isValidPlayerName(playerName)) {
                        currTeam.addPlayer(CustomGame.PLAYERS.populatePlayer(playerName));
                    }
                    playerCount++;
                }
            }
            if (teams.size() > 0) {
                createNewGame(teams, GameSetupOptions.extractGameName(options), 
                        GameSetupOptions.extractRoundBased(options));
            } else {
                mCallback.closeDetailView(true, false);
                return;
            }
        }
    }
    
    private void loadGame(long gameId, Bundle saveInstanceState) {
        assert Game.isValidId(gameId);
        List<Game> games = null;
        try {
            games = Game.loadGames(GameKey.CUSTOMGAME, getActivity().getContentResolver(), GameStorageHelper.getUriWithId(GameKey.CUSTOMGAME, gameId), true);
        } catch (CompactedDataCorruptException e) {
            games = null;
        }
        if (games != null && games.size() > 0) {
            assert games.size() == 1;
            mGame = (CustomGame) games.get(0);
            // this variable must never ever be changed, consider it to be final
            mIsLoadedFinishedGame = (saveInstanceState == null) ? (CUSTOMGAMES_LOADED_FINISHED_GAMES_ARE_IMMUTABLE ? mGame.isFinished() : false)
                    : saveInstanceState.getBoolean(STORAGE_IS_IMMUTABLE);
            if (mGame.isFinished()) {
                mLastRunningTimeUpdate = null; // so we do not update the running time anymore when loading a finished game
            }
            selectRound(mGame.getRoundCount() - 1);
            initUiToGameOptions();
        } else {
            Toast.makeText(getActivity(), getResources().getString(R.string.game_failed_loading), Toast.LENGTH_LONG).show();
            mCallback.closeDetailView(true, false); // nothing to show here
        }
    }
    
    private void createNewGame(List<AbstractPlayerTeam> teams, String gameName, boolean roundBased) {
        CustomGame baseOn = null;
        if (!TextUtils.isEmpty(gameName)) {
            // try to find a game with the same name that was played recently
            List<Game> sameName = null;
            try {
                sameName = CustomGame.loadGamesWithSameName(getActivity().getContentResolver(), gameName, false);
            } catch (CompactedDataCorruptException e) {
            }
            if (sameName != null && sameName.size() > 0) {
                baseOn = (CustomGame) sameName.get(0);
            }
        }

        mGame = new CustomGame(baseOn, roundBased);
        if (!TextUtils.isEmpty(gameName)) {
            mGame.setGameName(gameName); // just to ensure we really take the given game name (maybe letter cases or minor things differ)
        }
        mGame.createRound();

        mGame.setOriginData(OriginBuilder.getInstance().getOriginHints());
        mGame.setupTeams(teams);

        initUiToGameOptions();
        updateInterface();
    }
    
    private void initUiToGameOptions() {
        if (mGame.isRoundBased()) {
            mRoundContainer.setVisibility(View.VISIBLE);
        } else {
            mRoundContainer.setVisibility(View.GONE);
        }
        mCallback.setInfo(mGame.getName(), null);
    }
    
    private void selectTeam(int teamPos) {
        onInputTimerTimeout();
        mInputController.setListenerActive(teamPos);
    }
    
    private void selectNextTeam() {
        if (mSelectedTeam < mGame.getTeamCount() - 1) {
            selectTeam(mSelectedTeam + 1);
        } else {
            // last team is already selected
            if (mGame.isRoundBased()) {
                if (mSelectedRound == mGame.getRoundCount() - 1) {
                    // create a new round as last team of last round is selected
                    mInputController.executeCustomOperation(NEW_OP);
                } else {
                    mInputController.executeCustomOperation(NEXT_OP);
                }
            }
            selectTeam(0); // select first team
        }
    }
    
    private void selectTeamExecute(int teamPos) {
        mSelectedTeam = teamPos;
        updateInterface();
        mScoresView.smoothScrollToPosition(mSelectedTeam);
    }
    
    private void updateInterface() {
        if (mGame.isRoundBased()) {
            // ensure selected value is in bounds
            if (mSelectedRound < 0) {
                mSelectedRound = 0;
            } else if (mSelectedRound >= mGame.getRoundCount()) {
                mSelectedRound = mGame.getRoundCount() - 1;
            }
            if (mSelectedRound == mGame.getRoundCount() - 1) {
                // selected last bahn
                mNextRound.setImageResource(R.drawable.plus_small);
            } else {
                mNextRound.setImageResource(R.drawable.right_round_select);
            }
        } else {
            mSelectedRound = -1;
        }
        mRoundTitle.setText(getResources().getString(R.string.customgame_round_heading, mSelectedRound + 1));
        if (mAdapter == null) {
            mAdapter = new TeamScoreAdapter(getActivity());
            mScoresView.setOnItemClickListener(new OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> parent, View v,
                        int position, long id) {
                    selectTeam(position);
                }
                
            });
            mScoresView.setOnItemLongClickListener(new OnItemLongClickListener() {

                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View v,
                        int position, long id) {
                    if (isImmutable()) {
                        return false;
                    }
                    DialogFragment dialog = ChoosePlayerDialogFragment.newInstance(position, mGame.getTeams().get(position).getFirst(), true, true);
                    dialog.show(getActivity().getSupportFragmentManager(), "ChoosePlayerDialogFragment");
                    return true;
                }
            });
            mScoresView.setAdapter(mAdapter);
        } else {
            mAdapter.notifyDataSetChanged();
        }
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
    
    @Override
    public PlayerPool getPool() {
        return CustomGame.PLAYERS;
    }

    @Override
    public List<Player> toFilter() {
        return Collections.emptyList();
    }

    @Override
    public void playerChosen(int arg, Player chosen) {
        if (arg >= 0 && arg < mGame.getTeamCount() && chosen != null) {
            int index = 0;
            PlayerTeam teamWeAddedAPlayer = null;
            Player firstPlayerToSwitchToOtherTeam = null;
            for (AbstractPlayerTeam team : mGame.getTeams()) {
                if (!team.contains(chosen) && index == arg) {
                    if (team instanceof PlayerTeam) {
                        firstPlayerToSwitchToOtherTeam = team.getFirst();
                        teamWeAddedAPlayer = (PlayerTeam) team;
                        teamWeAddedAPlayer.addPlayer(chosen);
                    }
                }
                index++;
            }
            for (int i = 0; i < mGame.getTeamCount(); i++) {
                AbstractPlayerTeam team = mGame.getTeams().get(i);
                if (i != arg && team.contains(chosen)) {
                    // we already got this player somewhere else, remove him from the other team
                    ((PlayerTeam) team).removePlayer(chosen);
                    if (team.getPlayerCount() == 0 && firstPlayerToSwitchToOtherTeam != null) {
                        // ups we cleaned the team, better put a player back in it (equals switching of players in single player teams)
                        ((PlayerTeam) team).addPlayer(firstPlayerToSwitchToOtherTeam);
                        teamWeAddedAPlayer.removePlayer(firstPlayerToSwitchToOtherTeam);
                    }
                }
            }
            updateInterface();
        }
    }

    @Override
    public void playerRemoved(int arg, Player removed) {
        if (arg < 0 || arg >= mGame.getTeamCount() || removed == null) {
            return;
        }
        List<AbstractPlayerTeam> teams = mGame.getTeams();
        PlayerTeam team = (PlayerTeam) teams.get(arg);
        if (teams.size() == 1 && team.getPlayerCount() == 1) {
            return; // only player left in only team, we cannot remove it
        }
        if (team.removePlayer(removed)) {
            if (team.getPlayerCount() == 0 && mGame.removeTeam(arg)) {
                mInputController.clearHistory();
                selectTeamExecute(0);
                onTeamNumberChange();
            }
            updateInterface();
        }
    }
    
    @Override
    public void onPlayerColorChanged(int arg, Player concernedPlayer) {
        updateInterface();
    }

    @Override
    protected boolean isImmutable() {
        return mIsLoadedFinishedGame && CUSTOMGAMES_LOADED_FINISHED_GAMES_ARE_IMMUTABLE;
    }

    @Override
    protected void deselectRound() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void selectRound(int position) {
        mSelectedRound = position;
        updateInterface();
    }

    @Override
    protected Game getGame() {
        return mGame;
    }

    @Override
    protected void setInfoText(CharSequence main, CharSequence extra) {
        // extra text not used, so ignored
        mMainInfo.setText(main);
        mMainInfo.setVisibility(View.VISIBLE);
    }
    
    private class TeamScoreAdapter extends ArrayAdapter<AbstractPlayerTeam> {
        private final NumberFormat DOUBLE_FORMAT = new DecimalFormat("#.##");
        private int mUserGameColor;
        public TeamScoreAdapter(Context context) {
            super(context, R.layout.customgame_single_team, R.id.names, mGame.getTeams());
            mUserGameColor = getResources().getColor(R.color.usergame_text_color);
        }
        
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            if (v == null) {
                v = getActivity().getLayoutInflater().inflate(R.layout.customgame_single_team, parent, false);
            }
            TextView names = (TextView) v.findViewById(R.id.names);
            TextView totalScore = (TextView) v.findViewById(R.id.score);
            TextView scores = (TextView) v.findViewById(R.id.singleScores);
            if (position == mSelectedTeam) {
                v.setBackgroundResource(GameKey.getBackgroundResource(GameKey.CUSTOMGAME));
                GameKey.applyThemeTextColor(GameKey.CUSTOMGAME, getResources(), names);
                GameKey.applyThemeTextColor(GameKey.CUSTOMGAME, getResources(), totalScore);
                GameKey.applyThemeTextColor(GameKey.CUSTOMGAME, getResources(), scores);
            } else {
                v.setBackgroundResource(0);
                names.setTextColor(Color.BLACK);
                totalScore.setTextColor(Color.BLACK);
                scores.setTextColor(Color.BLACK);
            }
            // team and player names
            SpannableStringBuilder namesBuilder = new SpannableStringBuilder();
            // team name
            AbstractPlayerTeam team = getItem(position);
            if (team.getPlayerCount() > 1 && !TextUtils.isEmpty(team.getTeamName())) {
                SpannableString teamColorSpannable= new SpannableString(team.getTeamName());
                teamColorSpannable.setSpan(new ForegroundColorSpan(adaptColor(team.getColor(), position)), 0, team.getTeamName().length(), 0);
                namesBuilder.append(teamColorSpannable);
                namesBuilder.append(": ");
            }
            // player names
            Iterator<Player> it = team.iterator();
            while (it.hasNext()) {
                Player curr = it.next();
                String playerName = curr.getName();
                SpannableString playerColorSpannable= new SpannableString(playerName);
                playerColorSpannable.setSpan(new ForegroundColorSpan(adaptColor(curr.getColor(), position)), 0, playerName.length(), 0);
                namesBuilder.append(playerColorSpannable);
                if (it.hasNext()) {
                    namesBuilder.append(", ");
                }
            }
            names.setText(namesBuilder, BufferType.SPANNABLE);
            
             // scores
            double totalScoreValue = mGame.getScore(position);
            totalScore.setText(DOUBLE_FORMAT.format(totalScoreValue));
            int shownScoresLeftCount;
            int shownScoresRightCount;
            int roundIndex;
            if (mGame.isRoundBased()) {
                shownScoresLeftCount = shownScoresRightCount = 3;
                roundIndex = mSelectedRound;
            } else {
                shownScoresLeftCount = 8;
                shownScoresRightCount = 0;
                roundIndex = mGame.getFirstNaNRound(position) - 1;
                roundIndex = Math.max(0, roundIndex);
            }
            StringBuilder scoresBuilder = new StringBuilder(2 * (shownScoresLeftCount + shownScoresRightCount) + 10);
            if (roundIndex > shownScoresLeftCount) {
                scoresBuilder.append(".. ");
            }
            int count = 0;
            for (int i = roundIndex - 1; i >= roundIndex - shownScoresLeftCount && i >= 0; i--) {
                scoresBuilder.append(getRoundScore(Math.max(0, roundIndex - shownScoresLeftCount) + count, position));
                scoresBuilder.append(' ');
                count++;
            }
            scoresBuilder.append(' ');
            int boldStartIndex = scoresBuilder.length();
            scoresBuilder.append(getRoundScore(roundIndex, position));
            int boldEndIndex = scoresBuilder.length();
            scoresBuilder.append(' ');
            scoresBuilder.append(' ');
            for (int i = roundIndex + 1; i <= roundIndex + shownScoresRightCount && i < mGame.getRoundCount(); i++) {
                scoresBuilder.append(getRoundScore(i, position));
                scoresBuilder.append(' ');
            }
            if (mSelectedRound < mGame.getRoundCount() - shownScoresRightCount- 1 && mGame.isRoundBased()) {
                scoresBuilder.append("..");
            }
            if (mGame.isRoundBased() || (mSelectedTeam == position && mDelayedInputTimer != null)) {
                SpannableStringBuilder content = new SpannableStringBuilder(scoresBuilder.toString());
                final StyleSpan bss = new StyleSpan(android.graphics.Typeface.BOLD); 
                content.setSpan(bss, boldStartIndex, boldEndIndex, Spannable.SPAN_INCLUSIVE_EXCLUSIVE); 
                scores.setText(content);
            } else {
                scores.setText(scoresBuilder.toString());
            }
            return v;
        }
        
        private String getRoundScore(int round, int position) {
            double val = ((CustomRound) mGame.getRound(round)).getValue(position);
            if (Double.isNaN(val)) {
                return "X";
            } else if (Double.isInfinite(val)) {
                return "\u221E"; // well, so far this can never happen, but if it happens, it looks fabulous
            }
            return DOUBLE_FORMAT.format(val);
        }
        
        private int adaptColor(int color, int teamPos) {
            if (teamPos == mSelectedTeam) {
                return mUserGameColor;
            } else {
                return color;
            }
        }
        
    }

    @Override
    protected int getGameKey() {
        return GameKey.CUSTOMGAME;
    }

}
