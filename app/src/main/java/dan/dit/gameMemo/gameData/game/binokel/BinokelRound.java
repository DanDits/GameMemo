package dan.dit.gameMemo.gameData.game.binokel;

import android.support.annotation.NonNull;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import dan.dit.gameMemo.gameData.game.GameRound;
import dan.dit.gameMemo.gameData.game.InadequateRoundInfo;
import dan.dit.gameMemo.gameData.player.Player;
import dan.dit.gameMemo.util.compaction.CompactedDataCorruptException;
import dan.dit.gameMemo.util.compaction.Compacter;

public class BinokelRound extends GameRound {
    public static final int MAX_STICH_SCORE = 240;
    private final BinokelGame mGame;
    private BinokelReizen mReizen;
    private BinokelRoundType mRoundType;
    private BinokelMeldung[] mMeldungen;
    private BinokelRoundResult[] mResults;
    private int mPlayersCount;
    private int mTeamsCount;
    private boolean mStyleAbgegangen;
    private boolean mStyleLostSpecialGame;


    protected BinokelRound(@NonNull BinokelGame game, Compacter data) throws InadequateRoundInfo, CompactedDataCorruptException {
        mGame = game;
        initFromGame();
        unloadData(data);
    }

    private BinokelRound(@NonNull BinokelGame game) {
        mGame = game;
        initFromGame();
    }

    private BinokelRound copy(@NonNull BinokelGame game) {
        try {
            return new BinokelRound(game, new Compacter(compact()));
        } catch (CompactedDataCorruptException e) {
            Log.e("Binokel", "Error copying round: " + e);
            return null;
        } catch (InadequateRoundInfo i) {
            Log.e("Binokel", "Error copying round (inadequate): " + i);
            return null;
        }
    }

    private void initFromGame() {
        List<BinokelTeam> teams = mGame.getTeams();
        mTeamsCount = teams.size();
        mPlayersCount = mGame.getPlayersCount();
        mMeldungen = new BinokelMeldung[mPlayersCount];

        int teamIndex = 0;
        int index = 0;
        for (BinokelTeam team : teams) {
            for (Player ignored : team.getPlayers()) {
                mMeldungen[index] = new BinokelMeldung(teamIndex, index);
                index++;
            }
            teamIndex++;
        }
        mReizen = new BinokelReizen(mPlayersCount);
        int playersPerTeam = mPlayersCount / mTeamsCount;
        for (int i = 0; i < mMeldungen.length; i++) {
            mMeldungen[i] = new BinokelMeldung(i / playersPerTeam, i);
        }
        mResults = new BinokelRoundResult[mTeamsCount];
        for (int i = 0; i < mResults.length; i++) {
            mResults[i] = new BinokelRoundResult();
        }
    }

    private boolean isReizenAndRoundValid() {
        return mReizen.hasReizenWinner() && mRoundType != null;
    }

    private boolean canBeValid() {
        return isReizenAndRoundValid();
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
        if (!mReizen.hasReizenWinner()) {
            throw new InadequateRoundInfo("No reizen winner");
        }
        int reizenValue = mReizen.getMaxReizenValue();
        int reizenWinnerIndex = mReizen.getReizenWinnerPlayerIndex();
        if (reizenValue <= 0 || reizenWinnerIndex < 0 || reizenWinnerIndex >= mPlayersCount) {
            throw new InadequateRoundInfo("Nothing gereizt (" + reizenValue + ") or no valid " +
                    "winner: " + reizenWinnerIndex);
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
        cmp.appendData(mRoundType.getKey());
        cmp.appendData(mReizen.compact());
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
        cmp.appendData(mStyleAbgegangen);
        cmp.appendData(mStyleLostSpecialGame);
        return cmp.compact();
    }

    @Override
    public void unloadData(Compacter compactedData) throws CompactedDataCorruptException {
        mRoundType = BinokelRoundType.getFromKey(compactedData.getData(0));
        mReizen = new BinokelReizen(new Compacter(compactedData.getData(1)));
        Compacter meldungen = new Compacter(compactedData.getData(2));
        for (int i = 0; i < mMeldungen.length; i++) {
            mMeldungen[i] = new BinokelMeldung(new Compacter(meldungen.getData(i)));
        }
        Compacter results = new Compacter(compactedData.getData(3));
        for (int i = 0; i < mResults.length; i++) {
            mResults[i] = new BinokelRoundResult(new Compacter(results.getData(i)));
        }
        mStyleAbgegangen = compactedData.getBoolean(4);
        mStyleLostSpecialGame = compactedData.getBoolean(5);
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

    private int getTeamIndexOfPlayer(int playerIndex) {
        return playerIndex / BinokelGame.getPlayersPerTeam(mPlayersCount);
    }

    public int getReizenWinningTeam() {
        return getTeamIndexOfPlayer(mReizen.getReizenWinnerPlayerIndex());
    }

    private boolean isReizenWinningTeam(int teamIndex) {
        return teamIndex >= 0 && teamIndex == getReizenWinningTeam();
    }

    private int calculateTeamScore(int teamIndex) throws InadequateRoundInfo {
        checkAndThrowRoundState();
        if (mStyleAbgegangen) {
            if (isReizenWinningTeam(teamIndex)) {
                if (mRoundType.isSpecial()) {
                    return -mRoundType.getSpecialDefaultScore();
                } else {
                    return -mReizen.getMaxReizenValue();
                }
            } else {
                return sumMeldungenScore(teamIndex);
            }
        }
        if (mRoundType.isSpecial()) {
            // reizen value doesn't matter, own meldungen ignored for playing team,
            // stich value ignored for all players
            boolean specialRoundWon = !mStyleLostSpecialGame;
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
            int reizenValue = mReizen.getMaxReizenValue();
            return reizenValue <= totalScore ? totalScore : -2 * reizenValue;
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

    public BinokelRoundType getRoundType() {
        return mRoundType;
    }

    public int getLastStichTeamIndex() {
        for (int i = 0; i < mResults.length; i++) {
            if (mResults[i].getMadeLastStich()) {
                return i;
            }
        }
        return -1;
    }

    public static class PrototypeBuilder {
        private BinokelRound mPrototype;
        public PrototypeBuilder(BinokelGame game) {
            mPrototype = new BinokelRound(game);
        }

        public PrototypeBuilder(BinokelGame game, BinokelRound round) {
            mPrototype = round.copy(game);
            if (mPrototype == null) {
                throw new IllegalArgumentException("Cannot copy game and round: " + round);
            }
        }

        public void setReizenValue(int playerIndex, int reizValue) {
            mPrototype.mReizen.setReizValue(playerIndex, reizValue);
        }

        public void setRoundType(BinokelRoundType type) {
            mPrototype.mRoundType = type;
            if (!isSpecialRoundType() && mPrototype.mStyleLostSpecialGame) {
                nextStyle();
            }
        }

        public void setMeldungValue(int playerIndex, int value) {
            mPrototype.mMeldungen[playerIndex].setScore(value);
        }

        public void setMadeLastStich(int teamIndex) {
            int index = 0;
            for (BinokelRoundResult ignored : mPrototype.mResults) {
                mPrototype.mResults[index].setMadeLastStich(index == teamIndex);
                index++;
            }
        }

        public void setStichScore(int teamIndex, int score) {
            mPrototype.mResults[teamIndex].setStichScore(score);
            List<Integer> setScores = new ArrayList<>(mPrototype.mResults.length);
            for (int i = 0; i < mPrototype.mResults.length; i++) {
                if (mPrototype.mResults[i].getStichScore() >= 0) {
                    setScores.add(i);
                }
            }
            if (setScores.size() == 2 || mPrototype.mResults.length == 2) {
                // either only 2 set or only 2 total teams, we can calculate other team score
                int otherIndex;
                if (setScores.size() == 2) {
                    otherIndex = setScores.get(0) == teamIndex ? setScores.get(1) :
                            setScores.get(0);
                } else {
                    otherIndex = (teamIndex + 1) % 2;
                }
                int otherScore = BinokelRound.MAX_STICH_SCORE - Math.max(0, score);
                mPrototype.mResults[otherIndex].setStichScore(otherScore);
            }
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

        public BinokelRound getValid() {
            return isValid() ? mPrototype : null;
        }

        public int getReizenValue(int playerIndex) {
            return mPrototype.getReizenValue(playerIndex);
        }

        public BinokelRoundType getRoundType() {
            return mPrototype.getRoundType();
        }

        public int getMeldenValue(int playerIndex) {
            return mPrototype.mMeldungen[playerIndex].getScore();
        }

        public int getReizenWinnerPlayerIndex() {
            return mPrototype.mReizen.getReizenWinnerPlayerIndex();
        }

        public int getLastStichTeamIndex() {
            return mPrototype.getLastStichTeamIndex();
        }

        public void nextStyle() {
            if (mPrototype.mStyleAbgegangen) {
                // lost special game (or normal if not special round type)
                mPrototype.mStyleAbgegangen = false;
                mPrototype.mStyleLostSpecialGame = isSpecialRoundType();
            } else if (mPrototype.mStyleLostSpecialGame) {
                // normal game
                mPrototype.mStyleLostSpecialGame = false;
                mPrototype.mStyleAbgegangen = false;
            } else {
                // abgegangen
                mPrototype.mStyleLostSpecialGame = false;
                mPrototype.mStyleAbgegangen = true;
            }
            Log.d("Binokel", "After next style now is: " + getStyleAbgegangen() + " - " +
                    getStyleLostSpecialGame());
        }

        public boolean getStyleAbgegangen() {
            return mPrototype.mStyleAbgegangen;
        }

        public boolean getStyleLostSpecialGame() {
            return mPrototype.mStyleLostSpecialGame;
        }

        public int getTotalValue(int teamIndex) {
            return mPrototype.getTeamScore(teamIndex);
        }

        public int getStichValue(int teamIndex) {
            return mPrototype.mResults[teamIndex].getStichScore();
        }

        public boolean isSpecialRoundType() {
            return mPrototype.getRoundType() != null && mPrototype.getRoundType().isSpecial();
        }

        public boolean isNormalRoundStyle() {
            return !mPrototype.mStyleAbgegangen && !mPrototype.mStyleLostSpecialGame;
        }
    }

    private int getReizenValue(int playerIndex) {
        return mReizen.getReizenValue(playerIndex);
    }
}
