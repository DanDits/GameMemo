package dan.dit.gameMemo.gameData.game.binokel;

import android.support.annotation.NonNull;
import android.util.Log;

import java.util.List;

import dan.dit.gameMemo.gameData.game.GameRound;
import dan.dit.gameMemo.gameData.game.InadequateRoundInfo;
import dan.dit.gameMemo.gameData.player.Player;
import dan.dit.gameMemo.util.compaction.CompactedDataCorruptException;
import dan.dit.gameMemo.util.compaction.Compacter;

/**
 * Created by daniel on 10.01.16.
 */
public class BinokelRound extends GameRound {
    private final BinokelGame mGame;
    private int mReizenWinnerTeamIndex;
    private int mReizenWinnerPlayerIndex;
    private int mReizenValue;
    private BinokelRoundType mRoundType;
    private BinokelMeldung[] mMeldungen;
    private BinokelRoundResult[] mResults;
    private int mPlayersCount;
    private int mTeamsCount;

    

    protected BinokelRound(@NonNull BinokelGame game, Compacter data) throws InadequateRoundInfo, CompactedDataCorruptException {
        mGame = game;
        initFromGame();
        unloadData(data);
    }

    private BinokelRound(@NonNull BinokelGame game) {
        mGame = game;
        initFromGame();
    }

    private void initFromGame() {
        List<BinokelTeam> teams = mGame.getTeams();
        mTeamsCount = teams.size();
        for (BinokelTeam team : teams) {
            mPlayersCount += team.getPlayerCount();
        }
        mMeldungen = new BinokelMeldung[mPlayersCount];

        int teamIndex = 0;
        int index = 0;
        for (BinokelTeam team : teams) {
            for (Player p : team.getPlayers()) {
                mMeldungen[index] = new BinokelMeldung(teamIndex, index);
                index++;
            }
            teamIndex++;
        }
        for (int i = 0; i < mMeldungen.length; i++) {
            mMeldungen[i] = new BinokelMeldung(i / teams.get(i).getPlayerCount(),
                    teams.get(i).getPlayerCount());
        }
        mResults = new BinokelRoundResult[mTeamsCount];
        for (int i = 0; i < mResults.length; i++) {
            mResults[i] = new BinokelRoundResult();
        }
    }

    private boolean canBeValid() {
        return mReizenValue > 0 && mRoundType != null;
    }

    protected void checkAndThrowRoundState() throws InadequateRoundInfo {
        if (mPlayersCount < BinokelGame.MIN_PLAYER_COUNT || mPlayersCount > BinokelGame.MAX_PLAYER_COUNT) {
            throw new InadequateRoundInfo("No players count set! " + mPlayersCount);
        }
        if (mTeamsCount < BinokelGame.MIN_TEAMS || mTeamsCount > BinokelGame.MAX_TEAMS) {
            throw new InadequateRoundInfo("Too little or too many teams! " + mTeamsCount);
        }
        if (mPlayersCount % mTeamsCount != 0) {
            throw new InadequateRoundInfo("Unbalanced players per team: " + mTeamsCount + " " +
                    "players: " + mPlayersCount);
        }
        if (mReizenWinnerTeamIndex < 0 || mReizenWinnerTeamIndex >= mTeamsCount
                || mReizenWinnerPlayerIndex < 0 || mReizenWinnerPlayerIndex >= mPlayersCount) {
            throw new InadequateRoundInfo("Illegal reizen winner team or player: " +
                    mReizenWinnerTeamIndex + " by player " + mReizenWinnerPlayerIndex);
        }
        if (mReizenValue <= 0) {
            throw new InadequateRoundInfo("Nothing gereizt: " + mReizenValue);
        }
        if (mRoundType == null) {
            throw new InadequateRoundInfo("No round type given.");
        }
        if (mMeldungen == null || mMeldungen.length == 0) {
            throw new InadequateRoundInfo("No meldungen given.");
        }
        if (mResults == null || mResults.length == 0) {
            throw new InadequateRoundInfo("No results given.");
        }
    }

    @Override
    public String compact() {
        Compacter cmp = new Compacter();
        cmp.appendData(mReizenValue)
                .appendData(mReizenWinnerPlayerIndex)
                .appendData(mReizenWinnerTeamIndex)
                .appendData(mRoundType.getKey());
        Compacter meldungen = new Compacter();
        for (BinokelMeldung meldung : mMeldungen) {
            meldungen.appendData(meldung.compact());
        }
        cmp.appendData(meldungen.compact());
        Compacter results = new Compacter();
        for (BinokelRoundResult result : mResults) {
            results.appendData(result.compact());
        }
        cmp.appendData(results.compact());
        return cmp.compact();
    }

    @Override
    public void unloadData(Compacter compactedData) throws CompactedDataCorruptException {
        mReizenValue = compactedData.getInt(0);
        mReizenWinnerPlayerIndex = compactedData.getInt(1);
        mReizenWinnerTeamIndex = compactedData.getInt(2);
        mRoundType = BinokelRoundType.getFromKey(compactedData.getData(3));
        Compacter meldungen = new Compacter(compactedData.getData(4));
        for (int i = 0; i < mMeldungen.length; i++) {
            mMeldungen[i] = new BinokelMeldung(new Compacter(meldungen.getData(i)));
        }
        Compacter results = new Compacter(compactedData.getData(5));
        for (int i = 0; i < mResults.length; i++) {
            mResults[i] = new BinokelRoundResult(new Compacter(results.getData(i)));
        }
    }

    public int getTeamScore(int teamIndex) {
        try {
            return calculateTeamScore(teamIndex);
        } catch (InadequateRoundInfo inadequateRoundInfo) {
            Log.e("Binokel", "Attempting to get team score of team " + teamIndex + " in illegal " +
                    "round info state! " + inadequateRoundInfo);
            return 0;
        }
    }


    private boolean isReizenWinningTeam(int teamIndex) {
        return teamIndex == mReizenWinnerTeamIndex;
    }

    private int calculateTeamScore(int teamIndex) throws InadequateRoundInfo {
        checkAndThrowRoundState();
        if (mRoundType.isSpecial()) {
            // reizen value doesn't matter, own meldungen ignored for playing team,
            // stich value ignored for all players
            boolean specialRoundWon = !mResults[mReizenWinnerTeamIndex].isSpecialGameLost();
            if (isReizenWinningTeam(teamIndex)) {
                int value = mGame.getSpecialRoundValue(mRoundType);
                return specialRoundWon ? value : -2 * value;
            } else {
                return specialRoundWon ? 0 : sumMeldungenScore(teamIndex);
            }
        }
        // no special round
        int lastStichScore = sumLastStichScore(teamIndex);
        int stichScore = sumStichScore(teamIndex);
        int meldungenScore = stichScore >= 0 || lastStichScore != 0 ?
                sumMeldungenScore(teamIndex) : 0;
        stichScore = Math.max(0, stichScore);

        int totalScore = lastStichScore + stichScore + meldungenScore;
        if (isReizenWinningTeam(teamIndex)) {
            return mReizenValue <= totalScore ? totalScore : -2 * mReizenValue;
        }
        return totalScore;
    }

    private int sumMeldungenScore(int teamIndex) {
        int sum = 0;
        final int playersPerTeam = mPlayersCount / mTeamsCount;
        for (int i = teamIndex * playersPerTeam; i < (teamIndex + 1) * playersPerTeam; i++) {
            sum += mMeldungen[i].getScore();
        }
        return sum;
    }

    private int sumStichScore(int teamIndex) {
        return mResults[teamIndex].getStichScore();
    }

    private int sumLastStichScore(int teamIndex) {
        return mResults[teamIndex].getMadeLastStich() ? BinokelGame.LAST_STICH_SCORE : 0;
    }


    public static class PrototypeBuilder {
        private BinokelRound mPrototype;
        public PrototypeBuilder(BinokelGame game) {
            mPrototype = new BinokelRound(game);
        }

        public void setReizenValue(int reizValue) {
            mPrototype.mReizenValue = reizValue;
        }

        public void setRoundType(BinokelRoundType type) {
            mPrototype.mRoundType = type;
        }

        public void setMeldungValue(int playerIndex, int value) {
            mPrototype.mMeldungen[playerIndex].setScore(value);
        }

        public void setMadeLastStich(int teamIndex) {
            int index = 0;
            for (BinokelRoundResult result : mPrototype.mResults) {
                mPrototype.mResults[index].setMadeLastStich(index == teamIndex);
                index++;
            }
        }

        public void setLostSpecialGame(int teamIndex) {
            int index = 0;
            for (BinokelRoundResult result : mPrototype.mResults) {
                mPrototype.mResults[index].setLostSpecialGame(index == teamIndex);
                index++;
            }
        }

        public void setStichScore(int teamIndex, int score) {
            mPrototype.mResults[teamIndex].setStichScore(score);
        }

        public boolean isValid() {
            if (mPrototype.canBeValid()) {
                try {
                    mPrototype.checkAndThrowRoundState();
                } catch (InadequateRoundInfo e) {
                    return false;
                }
                return true;
            }
            return false;
        }
    }
}
