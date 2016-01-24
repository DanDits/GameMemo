package dan.dit.gameMemo.gameData.game.binokel;

import dan.dit.gameMemo.util.compaction.Compactable;
import dan.dit.gameMemo.util.compaction.CompactedDataCorruptException;
import dan.dit.gameMemo.util.compaction.Compacter;

/**
 * Created by daniel on 24.01.16.
 */
public class BinokelRoundResult implements Compactable {
    private boolean mMadeLastStich;
    private boolean mLostSpecialGame;
    private int mStichScore;

    public BinokelRoundResult() {
    }

    public BinokelRoundResult(Compacter data) throws CompactedDataCorruptException {
        unloadData(data);
    }

    public void setMadeLastStich(boolean madeLastStich) {
        mMadeLastStich = madeLastStich;
    }

    public void setLostSpecialGame(boolean lostSpecialGame) {
        mLostSpecialGame = lostSpecialGame;
    }

    public void setStichScore(int stichScore) {
        mStichScore = stichScore;
    }

    public boolean isSpecialGameLost() {
        return mLostSpecialGame;
    }

    public int getStichScore() {
        return mStichScore;
    }

    public boolean getMadeLastStich() {
        return mMadeLastStich;
    }

    @Override
    public String compact() {
        Compacter cmp = new Compacter();
        cmp.appendData(mMadeLastStich)
                .appendData(mStichScore)
                .appendData(mLostSpecialGame);
        return cmp.compact();
    }

    @Override
    public void unloadData(Compacter compactedData) throws CompactedDataCorruptException {
        if (compactedData == null || compactedData.getSize() < 3) {
            throw new CompactedDataCorruptException("Too little data given!")
                    .setCorruptData(compactedData);
        }
        mMadeLastStich = compactedData.getBoolean(0);
        mStichScore = compactedData.getInt(1);
        mLostSpecialGame = compactedData.getBoolean(2);
    }
}
