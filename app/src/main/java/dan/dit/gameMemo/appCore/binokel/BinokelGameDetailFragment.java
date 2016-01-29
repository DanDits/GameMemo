package dan.dit.gameMemo.appCore.binokel;

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
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Date;
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
import dan.dit.gameMemo.gameData.game.binokel.BinokelRoundType;
import dan.dit.gameMemo.gameData.game.binokel.BinokelTeam;
import dan.dit.gameMemo.gameData.game.tichu.TichuGame;
import dan.dit.gameMemo.gameData.player.ChoosePlayerDialogFragment;
import dan.dit.gameMemo.gameData.player.Player;
import dan.dit.gameMemo.gameData.player.PlayerPool;
import dan.dit.gameMemo.storage.GameStorageHelper;
import dan.dit.gameMemo.util.compaction.CompactedDataCorruptException;

public class BinokelGameDetailFragment extends GameDetailFragment {


    private BinokelGame mGame;
    private TextView mInfoText;
    private BinokelGameRoundAdapter mAdapter;
    private BinokelGameStateMachine mStateMachine;
    private Button[] mPlayers;
    private ImageButton mRoundStatusButton;

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
                if (observer.isAlive()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        observer.removeOnGlobalLayoutListener(this);
                    } else {
                        observer.removeGlobalOnLayoutListener(this);
                    }
                }
            }
        });
        //TODO set refs to ui elements
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
                    () / 2);
            Log.d("Binokel", "Setting to y: " + (header.getY() + header.getHeight() - mRoundStatusButton.getHeight
                    () / 2));
            Log.d("Binokel", "Own height: " + mRoundStatusButton.getHeight() + " other height: "
                    + header.getHeight() + " other y " + header.getY());
        }
    }

    private List<Player> getAllPlayer() {
        List<Player> all = new LinkedList<>();
        for (BinokelTeam team : mGame.getTeams()) {
            all.addAll(team.getPlayers().getPlayers());
        }
        return all;
    }

    private void initListeners() {
        //TODO init listeners for ui elements
        View.OnLongClickListener exchangePlayerListener = new View.OnLongClickListener() {

            @Override
            public boolean onLongClick(View v) {
                if (isImmutable()) {
                    return false;
                }
                int playerId = 0;
                for (Button mPlayer : mPlayers) {
                    if (v == mPlayer) {
                        break;
                    }
                    playerId++;
                }
                List<Player> allPlayers = getAllPlayer();
                if (playerId < 0 || playerId >= allPlayers.size()) {
                    return false;
                }
                DialogFragment dialog = ChoosePlayerDialogFragment.newInstance(playerId,
                        allPlayers.get(playerId),
                        true,
                        false);
                dialog.show(getActivity().getSupportFragmentManager(), "ChoosePlayerDialogFragment");
                return true;
            }

        };
        for (int i = 0; i < mPlayers.length; i++) {
            mPlayers[i].setOnLongClickListener(exchangePlayerListener);
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
            int scoreLimit = extras != null ? GameSetupOptions.extractScoreLimit(extras) :
                    BinokelGame.DEFAULT_SCORE_LIMIT;
            int untenDurchValue = extras != null ? GameSetupOptions.extractUntenDurchValue(extras) :
                    BinokelRoundType.UNTEN_DURCH.getSpecialDefaultScore();
            int durchValue = extras != null ? GameSetupOptions.extractDurchValue(extras) :
                    BinokelRoundType.DURCH.getSpecialDefaultScore();

            createNewGame(players, scoreLimit, untenDurchValue, durchValue);
            Log.d("Binokel", "Creating new game for players: " + players + " of extras: " + extras);
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
        assert Game.isValidId(gameId);
        List<Game> games = null;
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
        return false;
    }

    @Override
    protected void deselectRound() {

    }

    @Override
    protected void selectRound(int position) {

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

    //TODO invoke when selecting/deselecting round
    private void makeUserInputAvailable(boolean available) {
        if (available && isImmutable()) {
            // sorry dude, you cannot edit a loaded finished game which got locked forever
            available = false;
        }
        for (Button player : mPlayers) {
            player.setEnabled(available);
        }
        mRoundStatusButton.setEnabled(available);
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
        int teamIndex = arg / BinokelGame.MAX_PLAYERS_PER_TEAM;
        int playerIndex = arg % BinokelGame.MAX_PLAYERS_PER_TEAM;
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

    private class BinokelGameStateMachine {
        private static final int STATE_EXPECT_DATA = 1;
        private static final int STATE_ADD_ROUND= 2;
        private static final int STATE_OLD_ROUND_EDITED = 3;
        private static final int STATE_OLD_ROUND_SELECTED = 4;
        private static final int STATE_REMATCH = 5;

        private int mState;
        //TODO implement methods
        private void determineState() {
            mState = STATE_EXPECT_DATA;
        }

        private int getLockInButtonDrawableId() {
            switch(mState) {
                case STATE_REMATCH:
                    return R.drawable.rematch;
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
            return "";
        }

        private boolean lockinButtonEnabled() {
            return mState != STATE_EXPECT_DATA;
        }

        private void onLockinButtonPressed() {

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

        }
    }
}
