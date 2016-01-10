package dan.dit.gameMemo.gameData.game.custom;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

import android.content.ContentResolver;
import android.content.res.Resources;
import android.database.Cursor;
import android.text.TextUtils;
import dan.dit.gameMemo.R;
import dan.dit.gameMemo.gameData.game.Game;
import dan.dit.gameMemo.gameData.game.GameKey;
import dan.dit.gameMemo.gameData.game.GameRound;
import dan.dit.gameMemo.gameData.player.AbstractPlayerTeam;
import dan.dit.gameMemo.gameData.player.Player;
import dan.dit.gameMemo.gameData.player.PlayerPool;
import dan.dit.gameMemo.gameData.player.PlayerTeam;
import dan.dit.gameMemo.storage.GameStorageHelper;
import dan.dit.gameMemo.util.compaction.CompactedDataCorruptException;
import dan.dit.gameMemo.util.compaction.Compacter;

public class CustomGame extends Game {
    public static final int MAX_TEAMS = 32; // since we save the winner information in the bits of an int :P
    public static final int MAX_PLAYER_PER_TEAM = 15;
    public static final String NAME = "Custom";
    public static final PlayerPool PLAYERS = new PlayerPool();
    public static final double DEFAULT_START_SCORE = 0;
    public static final String DEFAULT_NAME = "Custom";
    public static final boolean DEFAULT_HIGHEST_SCORE_WINS = true;
    public static final boolean DEFAULT_ROUND_BASED = true;
    public static Set<String> ALL_NAMES = null;
    
    private List<AbstractPlayerTeam> mTeams; 
    
    // metadata
    private boolean mHighestScoreWins; // If true then the player(s) with the highest score sum will be treated as the winner, else the lowest sum counts.
    private double mStartScore; // The starting score that the round scores are added upon
    private String mName; // The game name where the game is identified with, not null
    private String mDescription; // Game description which can be added by the user
    private boolean mRoundBased; // if false then only one round is used for keeping the current score for each player
    private boolean mIsFinished; // if the game is finished, this mainly shows some winner results in the overview and signals not to overwrite this by writing in winner column
    
    public CustomGame(CustomGame baseOn, boolean roundBased) {
        if (baseOn != null) {
            mName = baseOn.mName;
            mDescription = ""; // do not copy description, this is for specific notes
            mRoundBased = roundBased;
            mHighestScoreWins = baseOn.mHighestScoreWins;
            mStartScore = baseOn.mStartScore;
        } else {
            mName = DEFAULT_NAME;
            mStartScore = DEFAULT_START_SCORE;
            mHighestScoreWins = DEFAULT_HIGHEST_SCORE_WINS;
            mRoundBased = roundBased;
            mDescription = "";
        }
    }
    
    public CustomGame(String gameName, boolean highestScoreWins,
            boolean roundBased, double startScore) {
        if (TextUtils.isEmpty(gameName)) {
            mName = DEFAULT_NAME;
        } else {
            setGameName(gameName);
        }
        mRoundBased = roundBased;
        mHighestScoreWins = highestScoreWins;
        mStartScore = startScore;
        mDescription = "";
    }

    @Override
    public AbstractPlayerTeam getWinner() {
        List<Integer> winnerIndicies = mHighestScoreWins ? getMaxScoreIndices() : getMinScoreIndices();
        if (winnerIndicies.size() == 1) {
            return mTeams.get(winnerIndicies.get(0));
        } else {
            PlayerTeam team = new PlayerTeam();
            for (int index : winnerIndicies) {
                team.addPlayers(mTeams.get(index));
            }
            return team;
        }
    }
    
    public void setGameName(String gameName) {
        if (TextUtils.isEmpty(gameName)) {
            throw new IllegalArgumentException("Empty game name");
        }
        mName = gameName;
        if (ALL_NAMES != null) {
            ALL_NAMES.add(mName);
        }
    }

    @Override
    public void addRound(GameRound round) {
        if (round == null) {
            throw new IllegalArgumentException("Null round cannot be added.");
        }
        rounds.add(round);
        ((CustomRound) round).setTeamCount(mTeams == null ? 1 : mTeams.size(), mRoundBased);
    }

    @Override
    public void reset() {
        rounds.clear();
        mIsFinished = false; // probably
    }

    @Override
    public boolean isFinished() {
        return mIsFinished;
    }

    @Override
    public void setupPlayers(List<Player> players) {
        throw new UnsupportedOperationException();
    }
    
    public void setupTeams(List<AbstractPlayerTeam> teams) {
        if (teams == null) {
            throw new IllegalArgumentException("Null teams given.");
        }
        mTeams = teams;
        // just ensure that there is no empty team given
        Iterator<AbstractPlayerTeam> it = mTeams.iterator();
        while (it.hasNext()) {
            AbstractPlayerTeam curr = it.next();
            if (curr.getPlayerCount() == 0) {
                it.remove();
            }
        }
        if (mTeams.size() == 0) {
            throw new IllegalArgumentException("A custom game needs a player/team.");
        }
        for (GameRound r : rounds) {
            ((CustomRound) r).setTeamCount(mTeams.size(), mRoundBased);
        }

    }

    @Override
    protected String getPlayerData() {
        Compacter cmp = new Compacter(mTeams.size());
        for (AbstractPlayerTeam team : mTeams) {
            for (Player player : team) {
                cmp.appendData(player.getName());
            }
        }
        return cmp.compact();
    }

    @Override
    protected int getWinnerData() {
        int result = Game.WINNER_NONE;
        if (isFinished()) {
            result = 0;
            for (int index : mHighestScoreWins ? getMaxScoreIndices() : getMinScoreIndices()) {
                result |= (1 << index);
            }
        }
        return result;
    }

    private List<Integer> getMinScoreIndices() {
        double[] summedValues = new double[mTeams.size()];
        for (int i = 0; i < mTeams.size(); i++) {
            summedValues[i] = getScore(i);
        }
        List<Integer> minIndices = new ArrayList<Integer>(mTeams.size());
        double min = Double.MAX_VALUE;
        for (int i = 0; i < summedValues.length; i++) {
            if (Math.abs(summedValues[i] - min) < 1e-30) {
                minIndices.add(Integer.valueOf(i));
            } else if (summedValues[i] < min) {
                min = summedValues[i];
                minIndices.clear();
                minIndices.add(Integer.valueOf(i));
            } 
        }
        return minIndices;
    }
    
    private List<Integer> getMaxScoreIndices() {
        double[] summedValues = new double[mTeams.size()];
        for (int i = 0; i < mTeams.size(); i++) {
            summedValues[i] = getScore(i);
        }
        List<Integer> maxIndices = new ArrayList<Integer>(mTeams.size());
        double max = -Double.MAX_VALUE;
        for (int i = 0; i < summedValues.length; i++) {
            if (Math.abs(summedValues[i] - max) < 1e-30) {
                maxIndices.add(Integer.valueOf(i));
            } else if (summedValues[i] > max) {
                max = summedValues[i];
                maxIndices.clear();
                maxIndices.add(Integer.valueOf(i));
            } 
        }
        return maxIndices;
    }
    
    public static final boolean isTeamWinner(int winner, int index) {
        return (winner & (1 << index)) != 0;
    }

    @Override
    protected String getMetaData() {
        Compacter cmp = new Compacter(7);
        cmp.appendData(mHighestScoreWins ? "1" : "0");
        cmp.appendData(mStartScore);
        cmp.appendData(mName);
        cmp.appendData(mDescription);
        cmp.appendData(!mRoundBased ? "1" : "0"); // negation wanted to be keep data from previous versions over refactoring
        cmp.appendData(mIsFinished ? "1" : "0");
        // teams' metadata
        Compacter teamSizeData = new Compacter();
        Compacter teamNameData = new Compacter();
        Compacter teamColorData = new Compacter();
        for (AbstractPlayerTeam team : mTeams) {
            teamSizeData.appendData(team.getPlayerCount());
            teamNameData.appendData(team.getTeamName());
            teamColorData.appendData(team.getColor());
        }
        cmp.appendData(teamSizeData.compact()); // index 6
        cmp.appendData(teamNameData.compact()); // index 7
        cmp.appendData(teamColorData.compact()); // index 8
        return cmp.compact();
    }

    public static List<Integer> getTeamSizeDataOfMetadata(Compacter metaData) throws CompactedDataCorruptException {
        Compacter teamSizeData = new Compacter(metaData.getData(6));
        List<Integer> teamSizes = new ArrayList<Integer>(teamSizeData.getSize());
        for (int i = 0; i < teamSizeData.getSize(); i++) {
            teamSizes.add(teamSizeData.getInt(i));
        }
        return teamSizes;
    }
    
    public static List<String> getTeamNameDataOfMetadata(Compacter metaData) throws CompactedDataCorruptException {
        Compacter teamNameData = new Compacter(metaData.getData(7));
        List<String> teamNames = new ArrayList<String>(teamNameData.getSize());
        for (int i = 0; i < teamNameData.getSize(); i++) {
            teamNames.add(teamNameData.getData(i));
        }
        return teamNames;
    }
    
    protected static List<Integer> getTeamColorDataOfMetadata(Compacter metaData) throws CompactedDataCorruptException {
        Compacter teamColorData = new Compacter(metaData.getData(8));
        List<Integer> teamColors = new ArrayList<Integer>(teamColorData.getSize());
        for (int i = 0; i < teamColorData.getSize(); i++) {
            teamColors.add(teamColorData.getInt(i));
        }
        return teamColors;
    }
    
    protected void unloadMetadata(Compacter metaData) {
        mHighestScoreWins = metaData.getData(0).equals("1");
        try {
            mStartScore = metaData.getDouble(1);
        } catch (CompactedDataCorruptException e) {
            mStartScore = DEFAULT_START_SCORE;
        }
        mName = getGameNameOfMetadata(metaData);
        mDescription = metaData.getData(3);
        mRoundBased = !metaData.getData(4).equals("1"); // negation wanted, see getMetaData()
        mIsFinished = metaData.getData(5).equals("1");
    }
    
    public static final String getGameNameOfMetadata(Compacter metaData) {
        return metaData.getData(2);
    }
    
    @Override
    public void synch() {
        // nothing to do
    }

    @Override
    public int getKey() {
        return GameKey.CUSTOMGAME;
    }

    @Override
    public String getFormattedInfo(Resources res) {
        java.text.DateFormat dateFormat = SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.SHORT, SimpleDateFormat.SHORT);
        java.text.DateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        timeFormat.setTimeZone(TimeZone.getTimeZone("GMT+0"));
        StringBuilder builder = new StringBuilder();
        builder.append(res.getString(R.string.game_starttime)).append(": ").append(dateFormat.format(new Date(getStartTime()))).append('\n')
        .append(res.getString(R.string.game_runtime)).append(": ").append(timeFormat.format(new Date(getRunningTime()))).append('\n');
        builder.append(res.getString(R.string.customgame_game_name)).append(": ").append(getName()).append('\n');
        if (!TextUtils.isEmpty(mDescription)) {
            builder.append(mDescription).append('\n');
        }
        int teamIndex = 0;
        for (AbstractPlayerTeam team : mTeams) {
            int i = 0;
            if (!TextUtils.isEmpty(team.getTeamName())) {
                builder.append(team.getTeamName()).append(": ");                
            }
            for (Player p : team) {
                builder.append(p.getName());
                if (i < team.getPlayerCount() - 1) {
                    builder.append(", ");
                }
                i++;
            }
            builder.append(" (").append(getScore(teamIndex)).append(')').append('\n');
            teamIndex++;
        }
        builder.append(res.getString(R.string.customgame_startscore)).append(": ").append(mStartScore).append('\n');
        builder.append(res.getString(mHighestScoreWins ? R.string.customgame_highest_score_wins : R.string.customgame_lowest_score_wins)).append('\n');
        String originFormatted = getFormattedOrigin();
        if (originFormatted.length() > 0) {
            builder.append(res.getString(R.string.game_origin)).append(": ").append(originFormatted);
        }
        return builder.toString();
    }

    public void cleanUp() {
        // remove rounds at the end that only contain NaN values, so values that are unnecessary for
        // single round games or were explictly cleared for round based games
        for (int i = rounds.size() - 1; i >= 1; i--) { // do never clean the first round
            CustomRound r = (CustomRound) rounds.get(i);
            double[] values = r.getValues();
            boolean allNaNs = true;
            for (int j = 0; j < values.length; j++) {
                if (!Double.isNaN(values[j])) {
                    allNaNs = false;
                }
            }
            if (allNaNs) {
                rounds.remove(i);
            } else {
                break; // stop at first not all NaN round
            }
        }
    }
    
    @Override
    public void saveGame(ContentResolver resolver) {
        cleanUp();
        super.saveGame(null, resolver);
    }

    public boolean containsTeam(AbstractPlayerTeam team) {
        for (AbstractPlayerTeam t : mTeams) {
            if (t.containsTeam(team)) {
                return true;
            }
        }
        return false;
    }

    public String getName() {
        return mName;
    }

    public static void initGameNames(ContentResolver resolver) {
        ALL_NAMES = new HashSet<String>();
        String[] proj = new String[] {GameStorageHelper.COLUMN_METADATA, GameStorageHelper.COLUMN_ID};
        Cursor cursor = null;
        try {
            cursor = resolver.query(GameStorageHelper.getUriAllItems(GameKey.CUSTOMGAME), proj, null, null,null);
            int colIndex = cursor.getColumnIndexOrThrow(GameStorageHelper.COLUMN_METADATA);
            if (cursor != null) {
                cursor.moveToFirst();
                while (!cursor.isAfterLast()) {
                    ALL_NAMES.add(getGameNameOfMetadata(new Compacter(cursor.getString(colIndex))));
                    cursor.moveToNext();
                }
            }
        } finally {
            // Always close the cursor
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public int getTeamCount() {
        return mTeams.size();
    }

    public List<AbstractPlayerTeam> getTeams() {
        return mTeams;
    }

    public static List<Game> loadGamesWithSameName(
            ContentResolver resolver, String gameName, boolean throwOnFailure) throws CompactedDataCorruptException {
        if (TextUtils.isEmpty(gameName)) {
            throw new IllegalArgumentException("Cannot search for empty game name.");
        }
        String[] projection = GameKey.getStorageAvailableColumnsProj(GameKey.CUSTOMGAME);;
        Cursor cursor = null;
        try {
            //load all games that have the game name in their metadata (but make sure later that its really the game name)
            cursor = resolver.query(GameStorageHelper.getUriAllItems(GameKey.CUSTOMGAME), projection, GameStorageHelper.COLUMN_METADATA + " like ?", new String[] {'%' + gameName + '%'},
                    GameStorageHelper.COLUMN_STARTTIME + " DESC");
            List<Game> games = null;
            if (cursor != null) {
                games = new LinkedList<Game>();
                cursor.moveToFirst();
                while (!cursor.isAfterLast()) {
                    Game g = GameKey.getBuilder(GameKey.CUSTOMGAME).loadCursor(cursor).build();
                    
                    if (g != null && ((CustomGame) g).getName().equalsIgnoreCase(gameName)) {
                        games.add(g);
                    }
                    cursor.moveToNext();
                }
            }
            return games;
        } finally {
            // Always close the cursor
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public boolean getHighestScoreWins() {
        return mHighestScoreWins;
    }

    public boolean isRoundBased() {
        return mRoundBased;
    }

    public double getStartScore() {
        return mStartScore;
    }

    public boolean removeRound(int index) {
        if (rounds.size() > 1) {
            return rounds.remove(index) != null;
        }
        return false;
    }
    
    public CustomRound createRound() {
        CustomRound r = new CustomRound(mTeams != null ? mTeams.size() : 1, mRoundBased);
        addRound(r);
        return r;
    }

    public double getScore(int index) {
        double value = mStartScore;
        double currValue;
        for (GameRound r : rounds) {
            currValue = ((CustomRound) r).getValue(index);
            if (!Double.isNaN(currValue)) {
                value += currValue;
            }
        }
        return value;
    }

    public boolean removeTeam(int teamIndex) {
        if (mTeams.size() > 1 && teamIndex >= 0 && teamIndex < mTeams.size()) {
            mTeams.remove(teamIndex);
            for (GameRound r : rounds) {
                CustomRound round = (CustomRound) r;
                round.removeTeam(teamIndex);
            }
            return true;
        }
        return false;
    }

    public int getFirstNaNRound(int teamIndex) {
        int index = 0;
        for (GameRound r : rounds) {
            CustomRound round = (CustomRound) r;
            if (Double.isNaN(round.getValue(teamIndex))) {
                return index;
            }
            index++;
        }
        return index;
    }
    
    @Override
    public String toString() {
        String scores = "";
        for (GameRound r : rounds) {
            scores += Arrays.toString(((CustomRound) r).getValues());
        }
        return mName + ": " + scores;
    }

    public String getDescription() {
        return mDescription;
    }

    public void setDescription(String descr) {
        if (descr == null) {
            mDescription = "";
        } else {
            mDescription = descr;
        }
    }

    public void setHighestScoreWins(boolean highest) {
        mHighestScoreWins = highest;
    }

    public void setStartScore(double score) {
        mStartScore = score;
    }

    public void setFinished(boolean finished) {
        mIsFinished = finished;
    }
}
