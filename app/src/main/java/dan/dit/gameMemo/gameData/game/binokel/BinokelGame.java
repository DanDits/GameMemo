package dan.dit.gameMemo.gameData.game.binokel;

import android.content.ContentResolver;
import android.content.res.Resources;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import dan.dit.gameMemo.R;
import dan.dit.gameMemo.gameData.game.Game;
import dan.dit.gameMemo.gameData.game.GameKey;
import dan.dit.gameMemo.gameData.game.GameRound;
import dan.dit.gameMemo.gameData.game.InadequateRoundInfo;
import dan.dit.gameMemo.gameData.game.tichu.TichuRound;
import dan.dit.gameMemo.gameData.player.AbstractPlayerTeam;
import dan.dit.gameMemo.gameData.player.Player;
import dan.dit.gameMemo.gameData.player.PlayerPool;
import dan.dit.gameMemo.gameData.player.PlayerTeam;
import dan.dit.gameMemo.util.compaction.Compacter;

/**
 * Created by daniel on 10.01.16.
 */
public class BinokelGame extends Game {
    public static final String GAME_NAME = "Binokel";
    public static final PlayerPool PLAYERS = new PlayerPool();
    public static final int DEFAULT_SCORE_LIMIT = 1000;
    public static final int MAX_PLAYER_COUNT = 4;
    public static final int MIN_PLAYER_COUNT = 3;
    public static final int MIN_TEAMS = 2;
    public static final int MAX_TEAMS = 3;
    public static final int LAST_STICH_SCORE = 10;
    public static final int MIN_SCORE_LIMIT = 100;
    public static final int MAX_SCORE_LIMIT = 5000;
    public static final int MAX_PLAYERS_PER_TEAM = 2;
    public static final int MIN_PLAYERS_PER_TEAM = 1;

    private int mDurchScore = BinokelRoundType.DURCH.getSpecialDefaultScore();
    private int mUntenDurchScore = BinokelRoundType.UNTEN_DURCH.getSpecialDefaultScore();

    private List<BinokelTeam> mTeams = new ArrayList<>(MAX_TEAMS);
    private int mScoreLimit = DEFAULT_SCORE_LIMIT;

    protected BinokelGame() {
    }

    public BinokelGame(int scoreLimit, int untenDurchScore, int durchScore) {
        mScoreLimit = scoreLimit < MIN_SCORE_LIMIT ? DEFAULT_SCORE_LIMIT :
                            scoreLimit > MAX_SCORE_LIMIT ? DEFAULT_SCORE_LIMIT : scoreLimit;
        mUntenDurchScore = untenDurchScore < 0 ? 0 : untenDurchScore;
        mDurchScore = durchScore < 0 ? 0 : durchScore;
    }

    private BinokelTeam getWinnerTeam() {
        if (isFinished()) {
            int maxScore = Integer.MIN_VALUE;
            BinokelTeam highestTeam = null;
            for (BinokelTeam team : mTeams) {
                if (team.getTotalScore() > maxScore) {
                    highestTeam = team;
                    maxScore = team.getTotalScore();
                }
            }
            return highestTeam == null ? null : highestTeam;
        }
        return null;
    }

    @Override
    public AbstractPlayerTeam getWinner() {
        BinokelTeam winner = getWinnerTeam();
        return winner == null ? null : winner.mTeam;
    }

    @Override
    public void addRound(GameRound round) {
        if (isFinished()) {
            throw new IllegalStateException("Game is finished, no more rounds can be added.");
        } else if (round == null) {
            throw new IllegalArgumentException("Cannot add null round.");
        } else if (!(round instanceof BinokelRound)) {
            throw new IllegalArgumentException("Given round is no Binokelround.");
        } else {
            try {
                ((BinokelRound) round).checkAndThrowRoundState();
            } catch (InadequateRoundInfo e) {
                throw new IllegalArgumentException(e);
            }
            rounds.add(round);
            // (re)calculateScore here
            for (BinokelTeam team : mTeams) {
                team.addTotalScore(((BinokelRound) round).getTeamScore(team.getTeamIndex()));
            }
        }
    }

    @Override
    public void reset() {
        for (BinokelTeam team : mTeams) {
            team.setTotalScore(0);
        }
        rounds.clear();
    }

    @Override
    public boolean isFinished() {
        int highestAboveLimit = Integer.MIN_VALUE;
        boolean highestIsUnique = false;
        for (BinokelTeam team : mTeams) {
            if (team.getTotalScore() >= mScoreLimit) {
                if (team.getTotalScore() > highestAboveLimit) {
                    highestAboveLimit = team.getTotalScore();
                    highestIsUnique = true;
                } else if (team.getTotalScore() == highestAboveLimit) {
                    highestIsUnique = false;
                }
            }
        }
        return highestAboveLimit > 0 && highestIsUnique;
    }

    @Override
    public void setupPlayers(List<Player> players) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected String getPlayerData() {
        Compacter cmp = new Compacter(MAX_TEAMS * MAX_PLAYERS_PER_TEAM);
        for (BinokelTeam team : mTeams) {
            for (Player p : team.mTeam) {
                cmp.appendData(p.getName());
            }
        }
        return cmp.compact();
    }

    @Override
    protected int getWinnerData() {
        int result = Game.WINNER_NONE;
        BinokelTeam winner = getWinnerTeam();
        if (winner != null) {
            result = winner.getTeamIndex() + 1;
        }
        return result;
    }

    @Override
    protected String getMetaData() {
        Compacter metaData = new Compacter();
        metaData.appendData(mScoreLimit);
        metaData.appendData(mDurchScore);
        metaData.appendData(mUntenDurchScore);
        return metaData.compact();
    }

    private void calculateScore() {
        for (BinokelTeam team : mTeams) {
            team.setTotalScore(0);
        }
        for (GameRound round : rounds) {
            for (BinokelTeam team : mTeams) {
                team.addTotalScore(((BinokelRound) round).getTeamScore(team.getTeamIndex()));
            }
        }
    }

    @Override
    public void synch() {
        calculateScore();
    }

    @Override
    public int getKey() {
        return GameKey.BINOKEL;
    }

    @Override
    public String getFormattedInfo(Resources res) {
        java.text.DateFormat dateFormat = SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.SHORT, SimpleDateFormat.SHORT);
        java.text.DateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        timeFormat.setTimeZone(TimeZone.getTimeZone("GMT+0"));
        StringBuilder builder = new StringBuilder();
        builder.append(res.getString(R.string.game_starttime)).append(": ").append(dateFormat.format(new Date(getStartTime()))).append('\n')
                .append(res.getString(R.string.game_runtime)).append(": ").append(timeFormat
                .format(new Date(getRunningTime()))).append('\n');
        for (BinokelTeam team : mTeams) {
            boolean plus = false;
            for (Player p: team.mTeam.getPlayers()) {
                if (plus) {
                    builder.append(" + ");
                }
                plus = true;
                builder.append(p.getName());
            }
            builder.append(": ").append(team.getTotalScore()).append("\n");
        }
        builder.append("\n");
        if (mScoreLimit != DEFAULT_SCORE_LIMIT) {
            builder.append(res.getString(R.string.binokel_score_limit, mScoreLimit)).append('\n');
        }
        builder.append(res.getString(R.string.binokel_unten_durch_score, mUntenDurchScore))
                .append('\n')
                .append(res.getString(R.string.binokel_durch_score, mDurchScore))
                .append('\n');
        String originFormatted = getFormattedOrigin();
        if (originFormatted.length() > 0) {
            builder.append(res.getString(R.string.game_origin)).append(": ").append(originFormatted);
        }
        return builder.toString();
    }

    @Override
    public void saveGame(ContentResolver resolver) {
        super.saveGame(null, resolver);
    }

    public void addTeam(PlayerTeam team) {
        if (team == null || team.getPlayerCount() < MIN_PLAYERS_PER_TEAM || mTeams.size() >= MAX_TEAMS) {
            throw new IllegalArgumentException("Too many teams! " + mTeams.size() + " adding: " +
                    team);
        }
        mTeams.add(new BinokelTeam(mTeams.size(), team));
    }

    public void setScoreLimit(int scoreLimit) {
        mScoreLimit = scoreLimit;
    }

    public int getTeamsCount() {
        return mTeams.size();
    }

    public List<BinokelTeam> getTeams() {
        return new ArrayList<>(mTeams);
    }

    public int getSpecialRoundValue(BinokelRoundType type) {
        switch (type) {
            case UNTEN_DURCH:
                return mUntenDurchScore;
            case DURCH:
                return mDurchScore;
            default:
                return type.getSpecialDefaultScore();
        }
    }

    public void setSpecialGamesScores(int durchScore, int untenDurchScore) {
        mDurchScore = durchScore;
        mUntenDurchScore = untenDurchScore;
    }

    public boolean containsPlayer(AbstractPlayerTeam team) {
        for (BinokelTeam t : mTeams) {
            if (t.mTeam.equals(team)) {
                return true;
            } else if (t.mTeam.containsTeam(team)) {
                return true;
            }
        }
        return false;
    }

    public int getScoreLimit() {
        return mScoreLimit;
    }

    public int getUntenDurchValue() {
        return mUntenDurchScore;
    }

    public int getDurchValue() {
        return mDurchScore;
    }

    public int getScoreUpToRound(int roundIndex, int teamIndex) {
        int sum = 0;
        for (int i = 0; i <= roundIndex && i < rounds.size(); i++) {
            sum += ((BinokelRound) rounds.get(i)).getTeamScore(teamIndex);
        }
        return sum;
    }

    public static int getPlayersPerTeam(int playersCount) {
        return playersCount == 3 ? 1 : 2;
    }

    public void addAsTeams(List<Player> allPlayers) {
        PlayerTeam currTeam = null;
        int playersPerTeam = BinokelGame.getPlayersPerTeam(allPlayers.size());
        for (Player p : allPlayers) {
            if (currTeam == null) {
                currTeam = new PlayerTeam();
            }
            currTeam.addPlayer(p);
            if (currTeam.getPlayerCount() == playersPerTeam) {
                addTeam(currTeam);
                currTeam = null;
            }
        }
    }
}
