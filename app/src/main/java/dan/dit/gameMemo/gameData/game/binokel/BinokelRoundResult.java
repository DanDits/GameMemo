package dan.dit.gameMemo.gameData.game.binokel;

import dan.dit.gameMemo.util.compaction.Compactable;
import dan.dit.gameMemo.util.compaction.CompactedDataCorruptException;
import dan.dit.gameMemo.util.compaction.Compacter;

public class BinokelRoundResult implements Compactable {
    private boolean mMadeLastStich;
    private int mStichScore;

    public BinokelRoundResult() {
        mStichScore = -1;
    }

    public BinokelRoundResult(Compacter data) throws CompactedDataCorruptException {
        unloadData(data);
    }

    public void setMadeLastStich(boolean madeLastStich) {
        mMadeLastStich = madeLastStich;
    }

    public void setStichScore(int stichScore) {
        mStichScore = stichScore;
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
                .appendData(mStichScore);
        return cmp.compact();
    }

    @Override
    public void unloadData(Compacter compactedData) throws CompactedDataCorruptException {
        if (compactedData == null || compactedData.getSize() < 2) {
            throw new CompactedDataCorruptException("Too little data given!")
                    .setCorruptData(compactedData);
        }
        mMadeLastStich = compactedData.getBoolean(0);
        mStichScore = compactedData.getInt(1);
    }
}
