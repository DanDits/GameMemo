package dan.dit.gameMemo.gameData.game.minigolf;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import android.content.ContentResolver;
import android.content.res.Resources;
import dan.dit.gameMemo.R;
import dan.dit.gameMemo.gameData.game.Game;
import dan.dit.gameMemo.gameData.game.GameKey;
import dan.dit.gameMemo.gameData.game.GameRound;
import dan.dit.gameMemo.gameData.game.SportGame;
import dan.dit.gameMemo.gameData.player.AbstractPlayerTeam;
import dan.dit.gameMemo.gameData.player.Player;
import dan.dit.gameMemo.gameData.player.PlayerPool;
import dan.dit.gameMemo.gameData.player.PlayerTeam;
import dan.dit.gameMemo.util.compaction.Compacter;

public class MinigolfGame extends SportGame {
    public static final int MAX_PLAYERS = 10; // be sure to have enough views for the player names in the overview and detail layout
    public static final PlayerPool PLAYERS = new PlayerPool();
    public static final String GAME_NAME = "Minigolf";
    public static final int WINNER_PLAYER_ONE = Game.WINNER_NONE + 1;
    public static final int DEFAULT_LANES_COUNT = 15;
    
    private List<Player> mPlayers;
    private int mDesiredLanes;
    
    public MinigolfGame(int desiredLanes, MinigolfGame basedOn) {
        if (basedOn != null) {
            mDesiredLanes = basedOn.getLanesCount(); // probably the amount of lanes did not change for this location
        }
        if (desiredLanes > 0 && desiredLanes != DEFAULT_LANES_COUNT) {
            mDesiredLanes = desiredLanes; // well the user explicitly wanted another number of lanes, ok ok take it
        }
        if (basedOn != null) {
            setLocation(basedOn.mLocation);
        }
        for (int i = 0; (rounds.isEmpty() || i < mDesiredLanes); i++) {
            MinigolfRound r = createLane();
            if (basedOn != null && i < basedOn.getLanesCount()) {
                r.setLaneName(((MinigolfRound) basedOn.getRound(i)).getLaneName());
                r.setLaneComment(((MinigolfRound) basedOn.getRound(i)).getLaneComment());
            }
        }
    }
    
    protected MinigolfGame() {
        // only to be used when there is a guarantee that at least one lane will be created and added to this game, so when loading a MinigolfGame
    }
    
    @Override
    public String toString() {
        return "MinigolfGame: desiredLanes: " + mDesiredLanes + " players: " + mPlayers + " lanes count: " + rounds.size();
    }
    
    @Override
    public AbstractPlayerTeam getWinner() {
        List<Integer> minimumValues = getMinScoreIndices();
        if (minimumValues.size() == 1) {
            return mPlayers.get(minimumValues.get(0));
        } else {
            PlayerTeam team = new PlayerTeam();
            for (int index : getMinScoreIndices()) {
                team.addPlayer(mPlayers.get(minimumValues.get(index)));
            }
            return team;
        }
    }

    @Override
    public void addRound(GameRound round) {
        if (round == null) {
            throw new IllegalArgumentException("Null round cannot be added.");
        }
        rounds.add(round);
        ((MinigolfRound) round).setPlayerCount(mPlayers == null ? 1 : mPlayers.size());
    }

    @Override
    public void reset() {
        rounds.clear();
    }

    @Override
    public boolean isFinished() {
        for (GameRound r : rounds) {
            if (!((MinigolfRound) r).isFinished()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void setupPlayers(List<Player> players) {
        if (players == null || players.size() < 1) {
            throw new IllegalArgumentException("A minigolf game needs a player.");
        }
        mPlayers = players;
        for (GameRound r : rounds) {
            ((MinigolfRound) r).setPlayerCount(mPlayers.size());
        }

    }

    private String getDefaultLaneName(int laneNumber) {
        return "# " + laneNumber; // could also be made user editable or specific for the sport location
    }

    @Override
    protected String getPlayerData() {
        Compacter cmp = new Compacter(mPlayers.size());
        for (Player p : mPlayers) {
            cmp.appendData(p.getName());
        }
        return cmp.compact();
    }

    @Override
    protected int getWinnerData() {
        int result = Game.WINNER_NONE;
        if (isFinished()) {
            result = 0;
            for (int index : getMinScoreIndices()) {
                result |= (1 << index);
            }
        }
        return result;
    }
    
    public static final boolean isPlayerWinner(int winner, int index) {
        return (winner & (1 << index)) != 0;
    }

    private List<Integer> getMinScoreIndices() {
        int[] summedValues = new int[mPlayers.size()];
        Arrays.fill(summedValues, MinigolfRound.DEFAULT_VALUE);
        for (GameRound r : rounds) {
            MinigolfRound round = (MinigolfRound) r;
            int[] mScores = round.getValues();
            for (int i = 0; i < Math.min(mScores.length, summedValues.length) ; i++) {
               summedValues[i] += mScores[i];
            } 
        }
        List<Integer> minIndices = new ArrayList<Integer>(mPlayers.size());
        int min = Integer.MAX_VALUE;
        for (int i = 0; i < summedValues.length; i++) {
            if (summedValues[i] < min) {
                min = summedValues[i];
                minIndices.clear();
                minIndices.add(Integer.valueOf(i));
            } else if (summedValues[i] == min) {
                minIndices.add(Integer.valueOf(i));
            }
        }
        return minIndices;
    }
    
    @Override
    protected String getMetaData() {
        return getMetaDataInCompacter().compact();
    }

    @Override
    public void synch() {
        // nothing to do
    }

    @Override
    public int getKey() {
        return GameKey.MINIGOLF;
    }

    @Override
    public String getFormattedInfo(Resources res) {
        java.text.DateFormat dateFormat = SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.SHORT, SimpleDateFormat.SHORT);
        java.text.DateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        timeFormat.setTimeZone(TimeZone.getTimeZone("GMT+0"));
        StringBuilder builder = new StringBuilder();
        builder.append(res.getString(R.string.game_starttime)).append(": ").append(dateFormat.format(new Date(getStartTime()))).append('\n')
        .append(res.getString(R.string.game_runtime)).append(": ").append(timeFormat.format(new Date(getRunningTime()))).append('\n');
        for (Player p : mPlayers) {
            builder.append(p.getName()).append('\n');
        }
        builder.append(res.getString(R.string.sport_game_location)).append(" :").append(getLocation()).append('\n')
        .append(res.getString(R.string.minigolf_lanes_count, getLanesCount())).append('\n');
        int missing = getMissingEntries();
        if (missing > 0) {
            builder.append(res.getString(R.string.minigolf_missing_entries, getMissingEntries())).append('\n');
        }
        String originFormatted = getFormattedOrigin();
        if (originFormatted.length() > 0) {
            builder.append(res.getString(R.string.game_origin)).append(": ").append(originFormatted);
        }
        return builder.toString();
    }

    public int getMissingEntries() {
        int count = 0;
        for (GameRound r : rounds) {
            MinigolfRound round = (MinigolfRound) r;
            count += round.getMissingEntries();
        }
        return count;
    }

    @Override
    public void saveGame(ContentResolver resolver) {
        // if Minigolf stores extra columns then add data to ContentValues
        super.saveGame(resolver);
    }

    public int getPlayerCount() {
        return mPlayers.size();
    }

    public List<Player> getPlayers() {
        return mPlayers;
    }

    /**
     * Returns the amount of lanes that where added to the game, can
     * include empty not (yet) played lanes.
     * @return The amount of lanes of this game.
     */
    public int getLanesCount() {
        return rounds.size() == 0 ? 1 : rounds.size();
    }

    public boolean containsPlayers(AbstractPlayerTeam team) {
        for (Player p : team) {
            if (!mPlayers.contains(p)) {
                return false;
            }
        }
        return true;
    }

    public void swapPlayers(int index1, int index2) {
        if (index1 >= 0 && index2 >= 0 && index1 < mPlayers.size() && index2 < mPlayers.size()) {
            Player p1 = mPlayers.get(index1);
            Player p2 = mPlayers.get(index2);
            mPlayers.set(index1, p2);
            mPlayers.set(index2, p1);
            for (GameRound r : rounds) {
                MinigolfRound round = (MinigolfRound) r;
                round.swapPlayers(index1, index2);
            }
        } else {
            throw new IndexOutOfBoundsException();
        }
    }

    public MinigolfRound createLane() {
        MinigolfRound r = new MinigolfRound(mPlayers != null ? mPlayers.size() : 1, getDefaultLaneName(rounds.size() + 1));
        addRound(r);
        return r;
    }

    public int getScore(int position) {
        int sum = 0;
        for (GameRound r : rounds) {
            sum += ((MinigolfRound) r).getValue(position);
        }
        return sum;
    }

    public int getFirstIncompleteRound() {
        int index = 0;
        for (GameRound r : rounds) {
            if (!((MinigolfRound) r).isFinished()) {
                return index;
            }
            index++;
        }
        return 0; // all finished
    }

    public int getFirstIncompletePlayer(int bahn) {
        MinigolfRound round = (MinigolfRound) rounds.get(bahn);
        return round.getFirstIncompletePlayer();
    }

    public void removeLane(int bahn) {
        if (rounds.size() > 1) {
            rounds.remove(bahn);
        }
    }
    
}
