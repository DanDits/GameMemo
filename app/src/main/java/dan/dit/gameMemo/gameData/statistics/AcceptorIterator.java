package dan.dit.gameMemo.gameData.statistics;

import java.util.Iterator;
import java.util.NoSuchElementException;

import dan.dit.gameMemo.gameData.game.Game;
import dan.dit.gameMemo.gameData.game.GameRound;

public class AcceptorIterator {
    private GameStatistic mStat;
    private Iterator<Game> mGamesIt;
    private Game mCurrGame; // the latest game returned by nextGame()
    private int mCurrGameIndex;
    private Game mNextGame; // the game that will be returned next by nextGame()
    private int mNextGameIndex;
    private Iterator<GameRound> mRoundIt; // always an iterator of the rounds of mCurrGame
    private GameRound mNextRound;
    
    private int mAcceptedGamesCount;
    private int mAcceptedRoundsCount;
    
    protected AcceptorIterator(GameStatistic stat) {
        mStat = stat;
        mNextGameIndex = -1;
        mGamesIt = mStat.mAllGames.iterator();
        skipNotAcceptedGames();
    }
    
    public boolean hasNextGame() {
        return mNextGame != null;
    }
    
    public Game nextGame() {
        if (!hasNextGame()) {
            throw new NoSuchElementException("No more games.");
        }
        mCurrGame = mNextGame;
        mCurrGameIndex = mNextGameIndex;
        mNextGame = null;
        skipNotAcceptedGames(); // select next game or null if no game is accepted anymore
        mRoundIt = mCurrGame.getRounds().iterator();
        mNextRound = null;
        skipNotAcceptedRounds();
        mAcceptedGamesCount++;
        return mCurrGame;
    }
    
    /**
     * This can return <code>false</code> even if hasNextGame returns true for games
     * with no rounds. When this returns false make sure to invoke nextGame() before invoking nextRound().
     * @return <code>true</code> if nextRound() will return a valid GameRound of the current game.
     */
    public boolean hasNextRound() {
        return mNextRound != null;
    }
    
    public GameRound nextRound() {
        if (!hasNextRound()) {
            throw new NoSuchElementException("No more rounds.");
        }
        GameRound currRound = mNextRound;
        mNextRound = null;
        skipNotAcceptedRounds();
        mAcceptedRoundsCount++;
        return currRound;
    }
    
    public int getAcceptedGamesCount() {
        return mAcceptedGamesCount;
    }
    
    public int getAcceptedRoundsCount() {
        return mAcceptedRoundsCount;
    }
    
    private void skipNotAcceptedGames() {
        // ignores all games that the statistic does not accept
        while (mGamesIt.hasNext() && mNextGame == null) {
            Game candidate = mGamesIt.next();
            mNextGameIndex++;
            if (mStat.acceptGame(candidate, mStat.getData())) {
                mNextGame = candidate;
            }
        }
    }
    
    private void skipNotAcceptedRounds() {
        // ignores all rounds of the current game that the statistic does not accept
        while (mRoundIt != null && mRoundIt.hasNext() && mNextRound == null) {
            GameRound candidate = mRoundIt.next();
            if (mStat.acceptRound(mCurrGame, candidate, mStat.getData())) {
                mNextRound = candidate;
            }
        }
    }

    public int getNextGameIndex() {
        return mNextGameIndex;
    }

    public int getCurrentGameIndex() {
        return mCurrGameIndex;
    }
}
