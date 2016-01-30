package dan.dit.gameMemo.gameData.game.binokel;

import dan.dit.gameMemo.util.compaction.Compactable;
import dan.dit.gameMemo.util.compaction.CompactedDataCorruptException;
import dan.dit.gameMemo.util.compaction.Compacter;

/**
 * Created by daniel on 30.01.16.
 */
public class BinokelReizen implements Compactable {
    private int[] mReizenValues;

    public BinokelReizen(int playerCount) {
        if (playerCount < BinokelGame.MIN_PLAYER_COUNT) {
            throw new IllegalArgumentException("Illegal player count: " + playerCount);
        }
        mReizenValues = new int[playerCount];
    }

    public BinokelReizen(Compacter data) throws CompactedDataCorruptException {
        unloadData(data);
    }

    public int getReizenWinnerPlayerIndex() {
        int index = 0;
        int max = 0;
        for (int i = 0; i < mReizenValues.length; i++) {
            if (mReizenValues[i] > max) {
                index = i;
                max = mReizenValues[i];
            } else if (mReizenValues[i] == max) {
                index = -1; // more than one with same max value
            }
        }
        return index;
    }

    public void setReizValue(int playerIndex, int value) {
        mReizenValues[playerIndex] = Math.max(value, 0);
    }

    public int getMaxReizenValue() {
        int max = Integer.MIN_VALUE;
        for (int i = 0; i < mReizenValues.length; i++) {
            max = Math.max(max, mReizenValues[i]);
        }
        return max;
    }

    public boolean hasReizenWinner() {
        return getReizenWinnerPlayerIndex() != -1;
    }

    @Override
    public String compact() {
        Compacter cmp = new Compacter();
        cmp.appendData(mReizenValues.length);
        for (int i = 0; i < mReizenValues.length; i++) {
            cmp.appendData(mReizenValues[i]);
        }
        return cmp.compact();
    }

    @Override
    public void unloadData(Compacter compactedData) throws CompactedDataCorruptException {
        if (compactedData == null || compactedData.getSize() < BinokelGame.MIN_PLAYER_COUNT + 1) {
            throw new CompactedDataCorruptException("Too little data.").setCorruptData
                    (compactedData);
        }
        int count = compactedData.getInt(0);
        mReizenValues = new int[count];
        for (int i = 0; i < count; i++) {
            mReizenValues[i] = compactedData.getInt(i + 1);
        }
    }

    public int getReizenValue(int playerIndex) {
        return mReizenValues[playerIndex];
    }
}
