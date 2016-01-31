package dan.dit.gameMemo.appCore.binokel;

import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import dan.dit.gameMemo.R;
import dan.dit.gameMemo.appCore.GameDetailFragment;
import dan.dit.gameMemo.appCore.gameSetup.GameSetupActivity;
import dan.dit.gameMemo.appCore.gameSetup.TeamSetupTeamsController;
import dan.dit.gameMemo.gameData.game.Game;
import dan.dit.gameMemo.gameData.game.GameKey;
import dan.dit.gameMemo.gameData.game.OriginBuilder;
import dan.dit.gameMemo.gameData.game.binokel.BinokelGame;
import dan.dit.gameMemo.gameData.game.binokel.BinokelRound;
import dan.dit.gameMemo.gameData.game.binokel.BinokelRoundType;
import dan.dit.gameMemo.gameData.game.binokel.BinokelTeam;
import dan.dit.gameMemo.gameData.player.ChoosePlayerDialogFragment;
import dan.dit.gameMemo.gameData.player.Player;
import dan.dit.gameMemo.gameData.player.PlayerPool;
import dan.dit.gameMemo.storage.GameStorageHelper;
import dan.dit.gameMemo.util.SlidingNumberPicker;
import dan.dit.gameMemo.util.compaction.CompactedDataCorruptException;

public class BinokelGameDetailFragment extends GameDetailFragment {


    private BinokelGame mGame;
    private TextView mInfoText;
    private BinokelGameRoundAdapter mAdapter;
    private BinokelGameStateMachine mStateMachine;
    private Button[] mPlayers;
    private ImageButton mRoundStatusButton;
    private ViewSwitcher mBinokelSwitcher;
    private int mCurrRoundIndex = -1;
    private SlidingNumberPicker[] mReizenValues;
    private HashMap<BinokelRoundType, ImageButton> mRoundTypeToView;
    private SlidingNumberPicker[] mMeldenValues;
    private Button[] mLetzerStich;
    private Button mGamePlayedStyle;
    private SlidingNumberPicker[] mStichValues;
    private TextView[] mResults;
    private TextView mStichTitle;

    public static BinokelGameDetailFragment newInstance(long gameId) {
        BinokelGameDetailFragment f = new BinokelGameDetailFragment();

        // Supply index input as an argument.
        Bundle args = new Bundle();
        args.putLong(GameStorageHelper.getCursorItemType(GameKey.BINOKEL), gameId);
        f.setArguments(args);

        return f;
    }

    public static BinokelGameDetailFragment newInstance(Bundle extras) {
        BinokelGameDetailFragment f = new BinokelGameDetailFragment();
        f.setArguments(extras);
        return f;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Window window = getActivity().getWindow();
        if (window != null) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
            window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        }
        mInfoText = (TextView) getView().findViewById(R.id.binokel_game_info);
        mPlayers = new Button[] {
                (Button) getView().findViewById(R.id.binokel_game_player1),
                (Button) getView().findViewById(R.id.binokel_game_player2),
                (Button) getView().findViewById(R.id.binokel_game_player3),
                (Button) getView().findViewById(R.id.binokel_game_player4)
        };
        mRoundStatusButton = (ImageButton) getView().findViewById(R.id.floating_status_button);
        final ViewTreeObserver observer= mRoundStatusButton.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                setRoundStatusDefaultPosition();
                ViewTreeObserver toRemoveOn = observer.isAlive() ? observer : mRoundStatusButton
                        .getViewTreeObserver();
                if (toRemoveOn.isAlive()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        toRemoveOn.removeOnGlobalLayoutListener(this);
                    } else {
                        //noinspection deprecation
                        toRemoveOn.removeGlobalOnLayoutListener(this);
                    }
                }
            }
        });
        mReizenValues = new SlidingNumberPicker[] {
                (SlidingNumberPicker) getView().findViewById(R.id.reizen1),
                (SlidingNumberPicker) getView().findViewById(R.id.reizen2),
                (SlidingNumberPicker) getView().findViewById(R.id.reizen3),
                (SlidingNumberPicker) getView().findViewById(R.id.reizen4)
        };
        mRoundTypeToView = new HashMap<>();
        mRoundTypeToView.put(BinokelRoundType.DURCH, (ImageButton) getView().findViewById(R.id.binokel_durch));
        mRoundTypeToView.put(BinokelRoundType.UNTEN_DURCH, (ImageButton) getView().findViewById(R.id
                .binokel_untendurch));
        mRoundTypeToView.put(BinokelRoundType.TRUMPF_HERZ, (ImageButton) getView().findViewById(R.id
                .binokel_herz));
        mRoundTypeToView.put(BinokelRoundType.TRUMP_EICHEL, (ImageButton) getView().findViewById(R.id
                .binokel_eichel));
        mRoundTypeToView.put(BinokelRoundType.TRUMPF_SCHIPPEN, (ImageButton) getView().findViewById(R.id
                .binokel_schippen));
        mRoundTypeToView.put(BinokelRoundType.TRUMPF_SCHELLEN, (ImageButton) getView().findViewById(R.id
                .binokel_schellen));
        mMeldenValues = new SlidingNumberPicker[] {
                (SlidingNumberPicker) getView().findViewById(R.id.melden1),
                (SlidingNumberPicker) getView().findViewById(R.id.melden2),
                (SlidingNumberPicker) getView().findViewById(R.id.melden3),
                (SlidingNumberPicker) getView().findViewById(R.id.melden4)
        };
        mLetzerStich = new Button[] {
                (Button) getView().findViewById(R.id.letzer_stich1),
                (Button) getView().findViewById(R.id.letzer_stich2),
                (Button) getView().findViewById(R.id.letzer_stich3)
        };

        mGamePlayedStyle = (Button) getView().findViewById(R.id.binokel_game_played_style);
        mStichValues = new SlidingNumberPicker[] {
                (SlidingNumberPicker) getView().findViewById(R.id.stich1),
                (SlidingNumberPicker) getView().findViewById(R.id.stich2),
                (SlidingNumberPicker) getView().findViewById(R.id.stich3)
        };
        mResults = new TextView[] {
                (TextView) getView().findViewById(R.id.result1),
                (TextView) getView().findViewById(R.id.result2),
                (TextView) getView().findViewById(R.id.result3)
        };
        mStichTitle = (TextView) getView().findViewById(R.id.stich_punkte_title);
        mBinokelSwitcher = (ViewSwitcher) getView().findViewById(R.id.binokel_switcher);
        mStateMachine = new BinokelGameStateMachine();
        loadOrStartGame(savedInstanceState);
        registerForContextMenu(getListView());
        initListeners();
        handleRestorageOfUnsavedUserInput(savedInstanceState);
        GameKey.applySleepBehavior(GameKey.BINOKEL, getActivity());
    }

    private void setRoundStatusDefaultPosition() {
        View header = getView().findViewById(R.id.binokel_header_container);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            mRoundStatusButton.setY(header.getY() + header.getHeight() - mRoundStatusButton.getHeight
                    () * 1.f / 3.f);
        }
    }

    private List<Player> getAllPlayer() {
        List<Player> all = new LinkedList<>();
        for (BinokelTeam team : mGame.getTeams()) {
            all.addAll(team.getPlayers().getPlayers());
        }
        return all;
    }

    private int getPlayerButtonIndex(View view) {
        int playerId = 0;
        for (Button mPlayer : mPlayers) {
            if (view == mPlayer) {
                break;
            }
            playerId++;
        }
        List<Player> allPlayers = getAllPlayer();
        if (playerId < 0 || playerId >= allPlayers.size()) {
            return -1;
        }
        return playerId;
    }

    private void initListeners() {
        View.OnLongClickListener exchangePlayerListener = new View.OnLongClickListener() {

            @Override
            public boolean onLongClick(View v) {
                if (isImmutable()) {
                    return false;
                }
                int playerIndex = getPlayerButtonIndex(v);
                if (playerIndex < 0) {
                    return false;
                }
                DialogFragment dialog = ChoosePlayerDialogFragment.newInstance(playerIndex,
                        getAllPlayer().get(playerIndex),
                        true,
                        false);
                dialog.show(getActivity().getSupportFragmentManager(), "ChoosePlayerDialogFragment");
                return true;
            }

        };
        for (Button mPlayer : mPlayers) {
            mPlayer.setOnLongClickListener(exchangePlayerListener);
            mPlayer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (isImmutable()) {
                        return;
                    }
                    int playerIndex = getPlayerButtonIndex(v);
                    if (playerIndex < 0) {
                        return;
                    }
                    onHotButtonClick(playerIndex);
                }
            });
        }
        mRoundStatusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mStateMachine.onStateButtonPressed();
            }
        });
        for (int i = 0; i < mReizenValues.length; i++) {
            mReizenValues[i].setVisibility(i >= mGame.getPlayersCount() ? View.GONE : View.VISIBLE);
            final int index = i;
            mReizenValues[i].setOnValueChangedListener(new SlidingNumberPicker.OnValueChangedListener() {
                @Override
                public void onValueChanged(SlidingNumberPicker view, int newValue) {
                    mStateMachine.mRoundBuiler.setReizenValue(index, newValue);
                    mStateMachine.updateUI();
                }
            });
        }
        for (final BinokelRoundType type : BinokelRoundType.values()) {
            ImageButton view = mRoundTypeToView.get(type);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mStateMachine.mRoundBuiler.setRoundType(type);
                    mStateMachine.updateUI();
                }
            });
        }
        for (int i = 0; i < mMeldenValues.length; i++) {
            mMeldenValues[i].setVisibility(i >= mGame.getPlayersCount() ? View.GONE : View.VISIBLE);
            final int index = i;
            mMeldenValues[i].setOnValueChangedListener(new SlidingNumberPicker.OnValueChangedListener() {
                @Override
                public void onValueChanged(SlidingNumberPicker view, int newValue) {
                    mStateMachine.mRoundBuiler.setMeldungValue(index, newValue);
                    mStateMachine.updateUI();
                }
            });
        }
        for (int i = 0; i < mLetzerStich.length; i++) {
            mLetzerStich[i].setVisibility(i >= mGame.getTeamsCount() ? View.GONE : View.VISIBLE);
            final int index = i;
            mLetzerStich[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View clicked) {
                    mStateMachine.mRoundBuiler.setMadeLastStich(index);
                    mStateMachine.updateUI();
                }
            });
        }
        for (int i = 0; i < mResults.length; i++) {
            mResults[i].setVisibility(i >= mGame.getTeamsCount() ? View.GONE : View.VISIBLE);
        }
        mGamePlayedStyle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mStateMachine.mRoundBuiler.nextStyle();
                mStateMachine.updateUI();
            }
        });
        for (int i = 0; i < mStichValues.length; i++) {
            mStichValues[i].setVisibility(i >= mGame.getTeamsCount() ? View.GONE : View.VISIBLE);
            final int index = i;
            mStichValues[i].setOnValueChangedListener(new SlidingNumberPicker.OnValueChangedListener() {
                @Override
                public void onValueChanged(SlidingNumberPicker view, int newValue) {
                    mStateMachine.mRoundBuiler.setStichScore(index, newValue);
                    mStateMachine.updateUI();
                }
            });
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.binokel_detail, null);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.binokel_game_detail, menu);
    }

    @Override
    protected boolean isImmutable() {
        return LOADED_FINISHED_GAMES_ARE_IMMUTABLE && mIsLoadedFinishedGame;
    }

    private void loadOrStartGame(Bundle savedInstanceState) {
        // Check from the saved Instance
        long gameId = (savedInstanceState == null) ? Game.NO_ID : savedInstanceState
                .getLong(GameStorageHelper.getCursorItemType(GameKey.BINOKEL), Game.NO_ID);

        // Or passed from the other activity

        Bundle extras = getArguments();
        List<Player> players = null;
        if (extras != null) {
            long extraId = extras
                    .getLong(GameStorageHelper.getCursorItemType(GameKey.BINOKEL), Game.NO_ID);
            if (Game.isValidId(extraId)) {
                gameId = extraId;
            } else if (!Game.isValidId(gameId)) {
                // no id given, but maybe an uri?
                Uri uri = extras.getParcelable(GameStorageHelper.getCursorItemType(GameKey.BINOKEL));
                gameId = GameStorageHelper.getIdOrStarttimeFromUri(uri);
            }
            Bundle teamParams = extras.getBundle(GameSetupActivity.EXTRA_TEAMS_PARAMETERS);
            String[] playerNames = teamParams == null ? null : teamParams.getStringArray
                    (TeamSetupTeamsController.EXTRA_PLAYER_NAMES);
            if (playerNames != null) {
                players = new ArrayList<>(BinokelGame.MAX_PLAYER_COUNT);
                for (String name : playerNames) {
                    if (Player.isValidPlayerName(name)) {
                        players.add(getPool().populatePlayer(name));
                    }
                }
            }
        }

        mLastRunningTimeUpdate = new Date();
        if (Game.isValidId(gameId)) {
            loadGame(gameId, savedInstanceState);
        } else {
            Log.d("Binokel", "Extras: " + extras);
            Bundle parameters = extras != null ? extras.getBundle
                    (GameSetupActivity.EXTRA_OPTIONS_PARAMETERS) : null;
            int scoreLimit = parameters != null ? GameSetupOptions.extractScoreLimit(parameters) :
                    BinokelGame.DEFAULT_SCORE_LIMIT;
            int untenDurchValue = parameters != null ? GameSetupOptions.extractUntenDurchValue(parameters) :
                    BinokelRoundType.UNTEN_DURCH.getSpecialDefaultScore();
            int durchValue = parameters != null ? GameSetupOptions.extractDurchValue(parameters) :
                    BinokelRoundType.DURCH.getSpecialDefaultScore();

            createNewGame(players, scoreLimit, untenDurchValue, durchValue);
            Log.d("Binokel", "Creating new game for players: " + players + " of parameters: " +
                    parameters);
        }
    }

    private void handleRestorageOfUnsavedUserInput(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            return; // nothing to restore
        }
    }

    private void createNewGame(List<Player> players, int scoreLimit, int untenDurchScore, int
            durchScore) {
        if (players == null || players.size() < BinokelGame.MIN_PLAYER_COUNT) {
            if (players != null && players.size() > 0) {
                Toast.makeText(getActivity(), getResources().getString(R.string.binokel_game_failed_loading_players_missing), Toast.LENGTH_LONG).show();
            }
            mCallback.closeDetailView(true, false);
            return;
        }
        mGame = new BinokelGame(scoreLimit,
                untenDurchScore,
                durchScore);
        mGame.setOriginData(OriginBuilder.getInstance().getOriginHints());
        mGame.addAsTeams(players);
        fillData();
        mStateMachine.updateUI();
    }

    private void loadGame(long gameId, Bundle saveInstanceState) {
        List<Game> games;
        try {
            games = Game.loadGames(GameKey.BINOKEL, getActivity().getContentResolver(),
                    GameStorageHelper.getUriWithId(GameKey.BINOKEL, gameId), true);
        } catch (CompactedDataCorruptException e) {
            games = null;
        }
        if (games != null && games.size() > 0) {
            assert games.size() == 1;
            mGame = (BinokelGame) games.get(0);
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
        updateDisplayedPlayers();
        fillRoundData();
        deselectRound();
    }

    private void fillRoundData() {
        if (mAdapter == null) {
            mAdapter = new BinokelGameRoundAdapter(mGame, getActivity(), R.layout.binokel_round,
                    mGame.getRounds());
            setListAdapter(mAdapter);
            getListView().setSelection(mAdapter.getCount() - 1); // by default, scroll to bottom of round list
        }
        mAdapter.notifyDataSetChanged();
    }

    public boolean hasSelectedRound() {
        return mStateMachine == null || mStateMachine.shouldShowRound();
    }

    @Override
    protected void deselectRound() {
        mStateMachine.mRoundBuiler = null;
        mCurrRoundIndex = -1;
        getListView().clearChoices();
        getListView().requestLayout();
        makeUserInputAvailable(!mGame.isFinished());
        mStateMachine.updateUI();
    }

    @Override
    protected void selectRound(int index) {
        makeUserInputAvailable(true);
        mCurrRoundIndex = index;
        getListView().setItemChecked(mCurrRoundIndex, true);
        mStateMachine.mRoundBuiler = new BinokelRound.PrototypeBuilder(mGame,
                (BinokelRound) mGame.getRound(index));
        mStateMachine.updateUI();
    }

    private void saveCloseAndRematch() {
        if (getDisplayedGameId() == Game.NO_ID) {
            saveState();
        }
        mCallback.closeDetailView(false, true);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        saveState();
        if (mGame == null) {
            return;
        }
        outState.putLong(GameStorageHelper.getCursorItemType(GameKey.BINOKEL),
                mGame.getId());
        outState.putBoolean(STORAGE_IS_IMMUTABLE, mIsLoadedFinishedGame);
    }

    @Override
    public void onPause() {
        super.onPause();
        saveState();
    }

    @Override
    public void onDestroyView() {
        getListView().setAdapter(null);
        super.onDestroyView();
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

    private void makeUserInputAvailable(boolean available) {
        if (available && isImmutable()) {
            // sorry dude, you cannot edit a loaded finished game which got locked forever
            available = false;
        }
        for (Button player : mPlayers) {
            player.setEnabled(available);
        }
    }

    @Override
    protected Game getGame() {
        return mGame;
    }

    @Override
    protected int getGameKey() {
        return GameKey.BINOKEL;
    }

    @Override
    protected void setInfoText(CharSequence main, CharSequence extra) {
        if (mInfoText.getVisibility() != View.VISIBLE) {
            mInfoText.setVisibility(View.VISIBLE);
        }
        mInfoText.setText(main + " " + extra);
    }

    @Override
    public PlayerPool getPool() {
        return GameKey.getPool(GameKey.BINOKEL);
    }

    @Override
    public List<Player> toFilter() {
        return getAllPlayer();
    }

    @Override
    public void playerChosen(int arg, Player chosen) {
        if (chosen == null) {
            return;
        }
        chosen.saveColor(getActivity());
        List<BinokelTeam> teams = mGame.getTeams();
        for (BinokelTeam team : teams) {
            if (team.getPlayers().contains(chosen)) {
                updateDisplayedPlayers(); // maybe color changed
                return;
            }
        }
        int playersPerTeam = BinokelGame.getPlayersPerTeam(mGame.getPlayersCount());
        int teamIndex = arg / playersPerTeam;
        int playerIndex = arg % playersPerTeam;
        if (teamIndex < 0 || teamIndex >= teams.size() || playerIndex < 0) {
            Log.e("Binokel", "Chosen player index invalid: " + teamIndex + " and " + playerIndex);
            return;
        }
        BinokelTeam team = teams.get(teamIndex);
        if (playerIndex >= team.getPlayerCount()) {
            Log.e("Binokel", "Chosen player index too big: " + teamIndex + " and " + playerIndex
                + " for players in team: " + team.getPlayerCount());
            return;
        }
        Log.d("Binokel", "Setting " + chosen + " to playerindex " + playerIndex + " for team " +
                team.getPlayers() + " arg was " + arg + " team index was " + teamIndex);
        team.setPlayer(playerIndex, chosen);
        updateDisplayedPlayers();
    }

    private void updateDisplayedPlayers() {
        List<BinokelTeam> teams = mGame.getTeams();
        List<Player> players = getAllPlayer();
        Log.d("Binokel", "Teams: " + teams + " players: " + players);
        for (int i = 0; i < mPlayers.length; i++) {
            if (i < players.size()) {
                mPlayers[i].setVisibility(View.VISIBLE);
                mPlayers[i].setText(players.get(i).getShortenedName(Player.SHORT_NAME_LENGTH));
                mPlayers[i].setTextColor(players.get(i).getColor());
            } else {
                mPlayers[i].setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onPlayerColorChanged(int arg, Player concernedPlayer) {
        updateDisplayedPlayers();
    }

    @Override
    public void playerRemoved(int arg, Player removed) {
        throw new UnsupportedOperationException();
    }

    private void onHotButtonClick(int playerIndex) {
        if (mStateMachine.ensureRoundSelected()) {
            mReizenValues[playerIndex].addDelta();
        }
    }

    private class BinokelGameStateMachine {
        private static final int STATE_NO_ROUND_SELECTED = 1;
        private static final int STATE_ADD_SELECTED_ROUND= 2;
        private static final int STATE_OLD_ROUND_EDITED = 3;
        private static final int STATE_OLD_ROUND_SELECTED = 4;
        private static final int STATE_REMATCH = 5;

        private int mState;
        private BinokelRound.PrototypeBuilder mRoundBuiler;

        private void determineState() {
            mState = mGame.isFinished() ? STATE_REMATCH : STATE_NO_ROUND_SELECTED;
            if (mRoundBuiler != null) {
                if (!isImmutable() && mRoundBuiler.isValid()) {
                    mState = mCurrRoundIndex >= 0 ? STATE_OLD_ROUND_EDITED :
                            STATE_ADD_SELECTED_ROUND;
                } else {
                    mState = STATE_OLD_ROUND_SELECTED;
                }
            }

        }

        private int getStateButtonDrawableId() {
            switch(mState) {
                case STATE_REMATCH:
                    return R.drawable.rematch_big;
                case STATE_ADD_SELECTED_ROUND:
                    return R.drawable.ic_menu_save;
                case STATE_OLD_ROUND_EDITED:
                    return R.drawable.ic_menu_edit;
                case STATE_NO_ROUND_SELECTED:
                    return R.drawable.ic_menu_add;
                case STATE_OLD_ROUND_SELECTED:
                    return R.drawable.ic_menu_close_clear_cancel;
                default:
                    throw new IllegalStateException();
            }
        }

        private void onStateButtonPressed() {
            if (mRoundStatusButton.getVisibility() != View.VISIBLE) {
                return;
            }
            switch (mState) {
                case STATE_REMATCH:
                    saveCloseAndRematch();
                    break;
                case STATE_ADD_SELECTED_ROUND:
                    if (mRoundBuiler != null) {
                        BinokelRound round = mRoundBuiler.getValid();
                        if (round != null) {
                            mGame.addRound(round);
                            fillRoundData();
                        }
                    }
                    deselectRound();
                    break;
                case STATE_OLD_ROUND_EDITED:
                    if (mRoundBuiler != null) {
                        BinokelRound round = mRoundBuiler.getValid();
                        if (round != null) {
                            mGame.updateRound(mCurrRoundIndex, round);
                            fillRoundData();
                        }
                    }
                    deselectRound();
                    break;
                case STATE_OLD_ROUND_SELECTED:
                    deselectRound();
                    break;
                case STATE_NO_ROUND_SELECTED:
                    ensureRoundSelected();
                    break;
            }
        }

        private boolean ensureRoundSelected() {
            if (!mGame.isFinished() && !shouldShowRound()) {
                mRoundBuiler = new BinokelRound.PrototypeBuilder(mGame);
                mCurrRoundIndex = -1;
                mStateMachine.updateUI();
                return true;
            }
            return shouldShowRound();
        }

        private CharSequence getMainInfoText() {
            return String.valueOf(GameKey.getGameName(GameKey.BINOKEL, getResources())) + ' ' + mGame.getScoreLimit() + '!';
        }

        private CharSequence getExtraInfoText() {
            StringBuilder builder = new StringBuilder();
            if (mGame.getUntenDurchValue() != BinokelRoundType.UNTEN_DURCH.getSpecialDefaultScore()) {
                builder.append(getResources().getString(R.string.binokel_unten_durch_score_short, mGame
                        .getUntenDurchValue()));
            }
            if (mGame.getDurchValue() != BinokelRoundType.DURCH.getSpecialDefaultScore()) {
                if (builder.length() > 0) {
                    builder.append(", ");
                }
                builder.append(getResources().getString(R.string.binokel_durch_score_short, mGame
                        .getDurchValue()));
            }
            return builder.toString();
        }

        private void updateUI() {
            determineState();
            mCallback.setInfo(getMainInfoText(), getExtraInfoText());
            applyRoundTypeUi();
            applyReizenUi();
            applyMeldenUi();
            applyStichUi();
            applyLastStichUi();
            applyGameStyleUi();
            updateResultValueUi();
            updateStateButton();
            ensureSwitcherViewIsCorrect();
        }

        private void applyStichUi() {
            if (mRoundBuiler == null) {
                return;
            }
            int sum = 0;
            for (int i = 0; i < mGame.getTeamsCount(); i++) {
                int stichValue = mRoundBuiler.getStichValue(i);
                if (stichValue < 0) {
                    mStichValues[i].setValue(mStichValues[i].getMinValue());
                } else {
                    mStichValues[i].setValue(stichValue);
                }
                sum += Math.max(0, stichValue);
            }
            mStichTitle.setTextColor(sum == BinokelRound.MAX_STICH_SCORE ? Color.BLACK : Color.RED);
        }

        private void updateResultValueUi() {
            if (mRoundBuiler == null) {
                return;
            }
            for (int i = 0; i < mGame.getTeamsCount(); i++) {
                if (mRoundBuiler.isValid()) {
                    int total = mRoundBuiler.getTotalValue(i);
                    mResults[i].setText(total >= 0 ? ("+" + total) : String.valueOf(total));
                    mResults[i].setTextColor(Color.BLACK);
                } else {
                    mResults[i].setText(R.string.binokel_result_unknown);
                    mResults[i].setTextColor(Color.RED);
                }
            }
        }

        private void applyGameStyleUi() {
            if (mRoundBuiler == null) {
                return;
            }
            int specialGameResId = mRoundBuiler.getRoundType() == BinokelRoundType.UNTEN_DURCH ?
                        R.string.binokel_unten_durch_lost :
                    mRoundBuiler.getRoundType() == BinokelRoundType.DURCH ?
                            R.string.binokel_durch_lost : R.string.binokel_special_game_lost;
            mGamePlayedStyle.setText(mRoundBuiler.getStyleAbgegangen() ? R.string
                    .binokel_abgegangen :
                mRoundBuiler.getStyleLostSpecialGame() ? specialGameResId :
                R.string.binokel_normal_game);
        }

        private void applyLastStichUi() {
            if (mRoundBuiler == null) {
                return;
            }
            for (int i = 0; i < mGame.getTeamsCount(); i++) {
                mLetzerStich[i].setEnabled(i != mRoundBuiler.getLastStichTeamIndex());
            }
        }

        private void applyReizenUi() {
            if (mRoundBuiler == null) {
                return;
            }
            for (int i = 0; i < mGame.getPlayersCount(); i++) {
                mReizenValues[i].setValue(mRoundBuiler.getReizenValue(i));
            }
            int reizenWinner = mRoundBuiler.getReizenWinnerPlayerIndex();
            for (int i = 0; i < mGame.getPlayersCount(); i++) {
                mReizenValues[i].setTextColor(i == reizenWinner ? 0xFFa40909 : Color.BLACK);
            }
        }

        private void applyMeldenUi() {
            if (mRoundBuiler == null) {
                return;
            }
            for (int i = 0; i < mGame.getPlayersCount(); i++) {
                mMeldenValues[i].setValue(mRoundBuiler.getMeldenValue(i));
            }
        }

        private void updateStateButton() {
            if (mGame.isFinished() && mState == STATE_NO_ROUND_SELECTED) {
                mRoundStatusButton.setVisibility(View.INVISIBLE);
            } else {
                mRoundStatusButton.setVisibility(View.VISIBLE);
            }
            mRoundStatusButton.setImageResource(getStateButtonDrawableId());
        }
        private void ensureSwitcherViewIsCorrect() {
            boolean isShowingRound = mBinokelSwitcher.getCurrentView() == mBinokelSwitcher
                    .getChildAt(0);
            boolean shouldShowRound = shouldShowRound();
            if (isShowingRound && !shouldShowRound) {
                mBinokelSwitcher.showNext();
            } else if (!isShowingRound && shouldShowRound) {
                mBinokelSwitcher.showPrevious();
            }
        }

        public boolean shouldShowRound() {
            return (mState != STATE_REMATCH && mState != STATE_NO_ROUND_SELECTED);
        }

        public void applyRoundTypeUi() {
            if (mRoundBuiler == null) {
                return;
            }
            for (BinokelRoundType type : BinokelRoundType.values()) {
                ImageButton view = mRoundTypeToView.get(type);
                view.setEnabled(type != mRoundBuiler.getRoundType());
            }
        }
    }
}
