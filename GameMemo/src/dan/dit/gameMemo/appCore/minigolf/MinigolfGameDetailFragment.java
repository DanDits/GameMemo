package dan.dit.gameMemo.appCore.minigolf;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import dan.dit.gameMemo.R;
import dan.dit.gameMemo.appCore.GameDetailFragment;
import dan.dit.gameMemo.appCore.gameSetup.GameSetupActivity;
import dan.dit.gameMemo.appCore.gameSetup.TeamSetupTeamsController;
import dan.dit.gameMemo.appCore.numberInput.AbsoluteOperation;
import dan.dit.gameMemo.appCore.numberInput.AlgebraicOperation;
import dan.dit.gameMemo.appCore.numberInput.ClearOperation;
import dan.dit.gameMemo.appCore.numberInput.CustomOperation;
import dan.dit.gameMemo.appCore.numberInput.NumberInputController;
import dan.dit.gameMemo.appCore.numberInput.Operation;
import dan.dit.gameMemo.appCore.numberInput.OperationListener;
import dan.dit.gameMemo.appCore.numberInput.SelectListenerOperation;
import dan.dit.gameMemo.appCore.numberInput.SignOperation;
import dan.dit.gameMemo.gameData.game.Game;
import dan.dit.gameMemo.gameData.game.GameKey;
import dan.dit.gameMemo.gameData.game.OriginBuilder;
import dan.dit.gameMemo.gameData.game.SportGame;
import dan.dit.gameMemo.gameData.game.minigolf.MinigolfGame;
import dan.dit.gameMemo.gameData.game.minigolf.MinigolfRound;
import dan.dit.gameMemo.gameData.player.ChoosePlayerDialogFragment;
import dan.dit.gameMemo.gameData.player.Player;
import dan.dit.gameMemo.gameData.player.PlayerPool;
import dan.dit.gameMemo.storage.GameStorageHelper;
import dan.dit.gameMemo.util.compaction.CompactedDataCorruptException;

public class MinigolfGameDetailFragment extends GameDetailFragment {
    private static final boolean MINIGOLF_LOADED_FINISHED_GAMES_ARE_IMMUTABLE = false;
    private static final String CUSTOM_OPERATION_NEXT_BAHN_KEY = "next_bahn";
    private static final String CUSTOM_OPERATION_PREV_BAHN_KEY = "prev_bahn";
    private static final String CUSTOM_OPERATION_NEW_BAHN_KEY = "new_bahn";
    
    private ImageButton mNextBahn;
    private ImageButton mPrevBahn;
    private TextView mBahnTitle;
    private TextView mMissingEntries;
    private GridView mScoresView;
    private LinearLayout mInputContainer;
    private NumberInputController mInputController;
    
    // state information about the game
    private MinigolfGame mGame;
    private int mSelectedBahn;
    private PlayerScoreAdapter mAdapter;
    private int mSelectedPlayer;
    
    public static MinigolfGameDetailFragment newInstance(long gameId) {
        MinigolfGameDetailFragment f = new MinigolfGameDetailFragment();

        // Supply index input as an argument.
        Bundle args = new Bundle();
        args.putLong(GameStorageHelper.getCursorItemType(GameKey.MINIGOLF), gameId);
        f.setArguments(args);

        return f;
    }
    
    public static MinigolfGameDetailFragment newInstance(Bundle extras) {
        MinigolfGameDetailFragment f = new MinigolfGameDetailFragment();
        f.setArguments(extras);
        return f;
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View baseView = inflater.inflate(R.layout.minigolf_detail, null);
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
        mPrevBahn = (ImageButton) getView().findViewById(R.id.last_bahn);
        mNextBahn = (ImageButton) getView().findViewById(R.id.next_bahn);
        mBahnTitle = (TextView) getView().findViewById(R.id.bahn_title);
        mMissingEntries = (TextView) getView().findViewById(R.id.missing_entries);
        mScoresView = (GridView) getView().findViewById(R.id.scores_view);
        mInputContainer = (LinearLayout) getView().findViewById(R.id.score_input_container);
        loadOrStartGame(savedInstanceState);
        initListeners();
        initInput();
        GameKey.applySleepBehavior(GameKey.MINIGOLF, getActivity());
    }
    
    private void onPlayerNumberChange() {
        mAdapter.notifyDataSetChanged();
        adaptInputListeners();
    }
    
    private void adaptInputListeners() {
        mInputController.removeAllListener();
        for (int pos = 0; pos < mGame.getPlayerCount(); pos++)
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
                    if (mGame.getPlayerCount() > mPosition) {
                        return ((MinigolfRound) mGame.getRound(mSelectedBahn)).getValue(mPosition);
                    } else {
                        return Double.NaN; // can occur when a player is removed and the operation is undone/redone
                    }
                }
        
                @Override
                public boolean operationExecuted(double result, Operation op) {
                    if (mGame.getPlayerCount() > mPosition) {
                        if (op instanceof AlgebraicOperation || op instanceof AbsoluteOperation || op instanceof SignOperation) {
                            MinigolfRound curr = ((MinigolfRound) mGame.getRound(mSelectedBahn));
                            curr.setValue(mPosition, (int) result);
                            mAdapter.notifyDataSetChanged();
                            selectPlayer(curr.getFirstIncompletePlayer()); 
                        } else if (op instanceof ClearOperation) {
                            ((MinigolfRound) mGame.getRound(mSelectedBahn)).setValue(mPosition, MinigolfRound.DEFAULT_VALUE);
                            mAdapter.notifyDataSetChanged();
                        } else if (op instanceof SelectListenerOperation) {
                        } else if (op instanceof CustomOperation) {
                            boolean isInverse = ((CustomOperation) op).isInverse();
                            String key = ((CustomOperation) op).getKey();
                            if (key.equals(CUSTOM_OPERATION_PREV_BAHN_KEY)) {
                                if (isInverse) {
                                    if (mSelectedBahn == mGame.getRoundCount() - 1) {
                                        selectRound(0);
                                    } else {
                                        selectRound(mSelectedBahn + 1);
                                    }
                                } else {
                                    if (mSelectedBahn > 0) {
                                        selectRound(mSelectedBahn - 1);
                                    } else {
                                        selectRound(mGame.getRoundCount() - 1); // cycle through - continue at the end when it reached the first bahn
                                    }
                                }
                            } else if (key.equals(CUSTOM_OPERATION_NEXT_BAHN_KEY)) {
                                if (isInverse) {
                                    selectRound(mSelectedBahn - 1);
                                } else {
                                    selectRound(mSelectedBahn + 1);                                    
                                }
                            } else if (key.equals(CUSTOM_OPERATION_NEW_BAHN_KEY)) {
                                if (isInverse) {
                                    mGame.removeLane(mGame.getRoundCount() - 1);
                                } else {
                                    mGame.createLane();
                                }
                                selectRound(mGame.getRoundCount() - 1);
                            }
                        }
                        fillData();
                        return true;
                    }
                    return false; // can occur when a player is removed and the operation is undone/redone
                    
                }
        
                @Override
                public boolean isActive() {
                    return mPosition == mSelectedPlayer;
                }
        
                @Override
                public void setActive(boolean active) {
                    if (active) {
                        selectPlayerExecute(mPosition);
                    }
                }
            }.setPosition(pos)
            );
        
    }
    
    private void initInput() {
        if (mGame != null) {
            mInputController = new NumberInputController(GameKey.MINIGOLF, getActivity(), mInputContainer, false);
            if (mInputController.getOperationsCount() != 8) {
                mInputController.clearOperations();
            }
            if (mInputController.getOperationsCount() == 0) {
                mInputController.newOperation(new AbsoluteOperation(1), 0);
                mInputController.newOperation(new AbsoluteOperation(2), 1);
                mInputController.newOperation(new AbsoluteOperation(3), 2);
                mInputController.newOperation(new AbsoluteOperation(4), 3);
                mInputController.newOperation(new AbsoluteOperation(5), 4);
                mInputController.newOperation(new AbsoluteOperation(6), 5);
                mInputController.newOperation(new AbsoluteOperation(7), 6);
                mInputController.newOperation(new ClearOperation(), 7);
                
            }
            adaptInputListeners();
        }
    }
    
    private void initListeners() {
        mBahnTitle.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View v) {
                createLaneEditDialog();
            }
        });
        mPrevBahn.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View v) {
                onPrevBahnClick();
            }
        });
        mNextBahn.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View v) {
                onNextBahnClick();
            }
        });
    }
    
    private static final CustomOperation PREV_OP = new CustomOperation(CUSTOM_OPERATION_PREV_BAHN_KEY);
    private static final CustomOperation NEXT_OP = new CustomOperation(CUSTOM_OPERATION_NEXT_BAHN_KEY);
    private static final CustomOperation NEW_OP = new CustomOperation(CUSTOM_OPERATION_NEW_BAHN_KEY);
    private void onPrevBahnClick() {
        if (mGame.getRoundCount() > 1) {
            mInputController.executeCustomOperation(PREV_OP);
        }
    }
    private void onNextBahnClick() {
        if (mSelectedBahn < mGame.getRoundCount() - 1) {
            mInputController.executeCustomOperation(NEXT_OP);
        } else {
            mInputController.executeCustomOperation(NEW_OP);
        }
    }
    
    private void loadOrStartGame(Bundle savedInstanceState) {
        // Check from the saved Instance
        long gameId = (savedInstanceState == null) ? Game.NO_ID : savedInstanceState
                .getLong(GameStorageHelper.getCursorItemType(GameKey.MINIGOLF), Game.NO_ID);

        Bundle extras = getArguments();
        if (extras != null) {
            long extraId = extras
                    .getLong(GameStorageHelper.getCursorItemType(GameKey.MINIGOLF), Game.NO_ID);
            if (Game.isValidId(extraId)) {
                gameId = extraId;
            } else if (!Game.isValidId(gameId)) {
                // no id given, but maybe an uri?
                Uri uri = extras.getParcelable(GameStorageHelper.getCursorItemType(GameKey.MINIGOLF));
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
            List<Player> players = new LinkedList<Player>();
            if (teamParameters != null) {
                String[] playerNames = teamParameters.getStringArray(TeamSetupTeamsController.EXTRA_PLAYER_NAMES);
                for (String playerName : playerNames) {
                    if (Player.isValidPlayerName(playerName)) {
                        players.add(MinigolfGame.PLAYERS.populatePlayer(playerName));
                    }
                }
            }
            if (players.size() > 0) {
                createNewGame(players, GameSetupOptions.extractLocation(options), GameSetupOptions.extractLanesCount(options));
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
            games = Game.loadGames(GameKey.MINIGOLF, getActivity().getContentResolver(), GameStorageHelper.getUriWithId(GameKey.MINIGOLF, gameId), true);
        } catch (CompactedDataCorruptException e) {
            games = null;
        }
        if (games != null && games.size() > 0) {
            assert games.size() == 1;
            mGame = (MinigolfGame) games.get(0);
            // this variable must never ever be changed, consider it to be final
            mIsLoadedFinishedGame = (saveInstanceState == null) ? (MINIGOLF_LOADED_FINISHED_GAMES_ARE_IMMUTABLE ? mGame.isFinished() : false)
                    : saveInstanceState.getBoolean(STORAGE_IS_IMMUTABLE);
            if (mGame.isFinished()) {
                mLastRunningTimeUpdate = null; // so we do not update the running time anymore when loading a finished game
            }
            selectRound(mGame.getFirstIncompleteRound());
        } else {
            Toast.makeText(getActivity(), getResources().getString(R.string.game_failed_loading), Toast.LENGTH_LONG).show();
            mCallback.closeDetailView(true, false); // nothing to show here
        }
    }
    
    private void createNewGame(List<Player> players, String location, int desiredLanesCount) {
        if (desiredLanesCount < 1) {
            desiredLanesCount = MinigolfGame.DEFAULT_LANES_COUNT;
        }
        MinigolfGame baseOn = null;
        if (!TextUtils.isEmpty(location)) {
            // try to find a game that was previously on the same location to copy some info of it
            List<Game> sameLocation = null;
            try {
                sameLocation = SportGame.loadGamesAtSameLocation(GameKey.MINIGOLF, getActivity().getContentResolver(), location, false);
                Log.d("Minigolf", "Found games at same location: " + sameLocation.size() + " : " + sameLocation);
            } catch (CompactedDataCorruptException e) {
            }
            if (sameLocation != null && sameLocation.size() > 0) {
                baseOn = (MinigolfGame) sameLocation.get(0);
            }
        }
        mGame = new MinigolfGame(desiredLanesCount, baseOn);
        mGame.setLocation(location);
        mGame.setOriginData(OriginBuilder.getInstance().getOriginHints());
        mGame.setupPlayers(players);

        fillData();
    }
    
    private void selectPlayer(int playerPos) {
        if (mSelectedPlayer != playerPos) {
            mInputController.setListenerActive(playerPos);
        }
    }
    
    private void selectPlayerExecute(int playerPos) {
        mSelectedPlayer = playerPos;
        fillData();
    }
    
    private void fillData() {
        // ensure selected value is in bounds
        if (mSelectedBahn < 0) {
            mSelectedBahn = 0;
        } else if (mSelectedBahn >= mGame.getRoundCount()) {
            mSelectedBahn = mGame.getRoundCount() - 1;
        }
        if (mSelectedBahn == mGame.getRoundCount() - 1) {
            // selected last bahn
            mNextBahn.setImageResource(R.drawable.plus);
        } else {
            mNextBahn.setImageResource(R.drawable.right_bahn_select);
        }
        MinigolfRound round = (MinigolfRound) mGame.getRound(mSelectedBahn);
        mBahnTitle.setText(round.getLaneName()); // will be overwritten by setInfoText if this is under honeycomb (and does not have text in the actionbar)
        mCallback.setInfo(getResources().getString(R.string.minigolf_heading, mSelectedBahn + 1, mGame.getRoundCount()), 
                getResources().getString(R.string.minigolf_missing_entries, mGame.getMissingEntries()));
        if (mAdapter == null) {
            mAdapter = new PlayerScoreAdapter(getActivity());
            mScoresView.setOnItemClickListener(new OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> parent, View v,
                        int position, long id) {
                    selectPlayer(position);
                }
                
            });
            mScoresView.setOnItemLongClickListener(new OnItemLongClickListener() {

                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View v,
                        int position, long id) {
                    if (isImmutable()) {
                        return false;
                    }
                    DialogFragment dialog = ChoosePlayerDialogFragment.newInstance(position, null, false);
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
        return MinigolfGame.PLAYERS;
    }

    @Override
    public List<Player> toFilter() {
        return mGame.getPlayers();
    }

    @Override
    public void playerChosen(int arg, Player chosen) {
        List<Player> players = mGame.getPlayers();
        if (arg >= 0 && arg < players.size() && chosen != null) {
            if (!players.contains(chosen)) {
                players.set(arg, chosen);
            } else {
                // we already got this player somewhere
                int index = players.indexOf(chosen);
                if (index != -1) {
                    // should always be the case since players contains the chosen one (hoho)
                    mGame.swapPlayers(index, arg);
                }
            }
            fillData();
        }
    }

    @Override
    public void onPlayerColorChanged(int arg, Player concernedPlayer) {
        fillData();
    }

    @Override
    protected boolean isImmutable() {
        return mIsLoadedFinishedGame && MINIGOLF_LOADED_FINISHED_GAMES_ARE_IMMUTABLE;
    }

    @Override
    protected void deselectRound() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void selectRound(int position) {
        mSelectedBahn = position;
        mSelectedPlayer = mGame.getFirstIncompletePlayer(mSelectedBahn);
        fillData();
    }

    @Override
    protected Game getGame() {
        return mGame;
    }

    @Override
    protected void setInfoText(CharSequence main, CharSequence extra) {
        mBahnTitle.setText(main);
        mMissingEntries.setText(extra);
    }
    
    private class PlayerScoreAdapter extends ArrayAdapter<Player> {

        public PlayerScoreAdapter(Context context) {
            super(context, R.layout.minigolf_single_player, R.id.playerName, mGame.getPlayers());
        }
        
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            if (v == null) {
                v = getActivity().getLayoutInflater().inflate(R.layout.minigolf_single_player, parent, false);
            }
            if (position == mSelectedPlayer) {
                v.setBackgroundResource(GameKey.getBackgroundResource(GameKey.MINIGOLF));
            } else {
                v.setBackgroundResource(0);
            }
            TextView name = (TextView) v.findViewById(R.id.playerName);
            TextView totalScore = (TextView) v.findViewById(R.id.playerScore);
            TextView scores = (TextView) v.findViewById(R.id.laneScores);
            name.setText(getItem(position).getName());
            int totalScoreValue = mGame.getScore(position);
            totalScore.setText((totalScoreValue == MinigolfRound.DEFAULT_VALUE) ? "?" : String.valueOf(totalScoreValue));
            final int SHOWN_SCORES_LEFT_COUNT = 2;
            final int SHOWN_SCORES_RIGHT_COUNT = 2;
            StringBuilder scoresBuilder = new StringBuilder(2 * (SHOWN_SCORES_LEFT_COUNT + SHOWN_SCORES_RIGHT_COUNT) + 10);
            if (mSelectedBahn > SHOWN_SCORES_LEFT_COUNT) {
                scoresBuilder.append(".. ");
            }
            int count = 0;
            for (int i = mSelectedBahn - 1; i >= mSelectedBahn - SHOWN_SCORES_LEFT_COUNT && i >= 0; i--) {
                scoresBuilder.append(getRoundScore(Math.max(0, mSelectedBahn - SHOWN_SCORES_LEFT_COUNT) + count, position));
                scoresBuilder.append(' ');
                count++;
            }
            scoresBuilder.append(' ');
            int boldStartIndex = scoresBuilder.length();
            scoresBuilder.append(getRoundScore(mSelectedBahn, position));
            int boldEndIndex = scoresBuilder.length();
            scoresBuilder.append(' ');
            scoresBuilder.append(' ');
            for (int i = mSelectedBahn + 1; i <= mSelectedBahn + SHOWN_SCORES_LEFT_COUNT && i < mGame.getRoundCount(); i++) {
                scoresBuilder.append(getRoundScore(i, position));
                scoresBuilder.append(' ');
            }
            if (mSelectedBahn < mGame.getRoundCount() - SHOWN_SCORES_RIGHT_COUNT - 1) {
                scoresBuilder.append("..");
            }
            SpannableStringBuilder content = new SpannableStringBuilder(scoresBuilder.toString());
            final StyleSpan bss = new StyleSpan(android.graphics.Typeface.BOLD); 
            content.setSpan(bss, boldStartIndex, boldEndIndex, Spannable.SPAN_INCLUSIVE_EXCLUSIVE); 
            scores.setText(content);
            return v;
        }
        
        private String getRoundScore(int round, int position) {
            int val = ((MinigolfRound) mGame.getRound(round)).getValue(position);
            if (val == MinigolfRound.DEFAULT_VALUE) {
                return "?";
            } else {
                return String.valueOf(val);
            }
        }
        
    }
    
    public void createLaneEditDialog() {

       View baseView = getActivity().getLayoutInflater().inflate(R.layout.minigolf_lane_edit, null);
        final EditText laneName = (EditText) baseView.findViewById(R.id.laneName);
        final EditText laneComment= (EditText) baseView.findViewById(R.id.laneDescr);
        final Button laneDelete = (Button) baseView.findViewById(R.id.delete);
        final MinigolfRound round = (MinigolfRound) mGame.getRound(mSelectedBahn);
        laneName.setText(round.getLaneName());
        laneComment.setText(round.getLaneComment());
        laneDelete.setEnabled(mGame.getLanesCount() > 1);

        
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setIcon(R.drawable.ic_menu_edit)
        .setTitle(getResources().getString(R.string.minigolf_heading, mSelectedBahn + 1, mGame.getLanesCount()))
                .setView(baseView)
               .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                       round.setLaneName(laneName.getText().toString());
                       round.setLaneComment(laneComment.getText().toString());
                       fillData();
                   }
               })
               .setNegativeButton(android.R.string.no, null);
        final AlertDialog dialog = builder.create();
        laneDelete.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View v) {
                if (mGame.removeLane(mSelectedBahn)) {
                    mInputController.clearHistory();
                    fillData();
                    dialog.dismiss();
                }
            }
        });
        dialog.show();
    
    }

}
