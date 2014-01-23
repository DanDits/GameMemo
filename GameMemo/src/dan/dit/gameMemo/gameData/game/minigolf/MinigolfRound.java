package dan.dit.gameMemo.gameData.game.minigolf;

import java.util.Arrays;

import dan.dit.gameMemo.gameData.game.GameRound;
import dan.dit.gameMemo.util.compaction.CompactedDataCorruptException;
import dan.dit.gameMemo.util.compaction.Compacter;

/**
 * Models a minigolf lane that stores points for all players
 * @author Daniel
 *
 */
public class MinigolfRound extends GameRound {
    public static final int DEFAULT_VALUE = 0;
    
    private String mLaneName = "";
    private String mLaneComment = "";
    private int[] mPoints;
   
    protected MinigolfRound(Compacter data) throws CompactedDataCorruptException {
        unloadData(data);
    }
    
    public MinigolfRound(int playerCount, String laneName) {
        setPlayerCount(playerCount);
        setLaneName(laneName);
    }
    
    public void setLaneName(String laneName) {
        mLaneName = laneName == null ? "" : laneName;
    }
    
    public void setLaneComment(String laneComment) {
        mLaneComment = laneComment == null ? "" : laneComment;
    }
    
    public String getLaneName() {
        return mLaneName;
    }
    
    public String getLaneComment() {
        return mLaneComment;
    }
    
    public boolean isDefaultLaneName() {
        return mLaneName.matches("[0-9]+");
    }
    
    public void setPlayerCount(int playerCount) {
        if (playerCount <= 0) {
            throw new IllegalArgumentException("At least one player required for a MinigolfRound.");
        }
        if (mPoints == null || playerCount != mPoints.length) {
            if (mPoints == null) {
                // create a new point array filled with DEFAULT_VALUE
                mPoints = new int[playerCount];
                Arrays.fill(mPoints, DEFAULT_VALUE);
            } else {
                // first copy the old point array
                int oldSize = mPoints.length;
                mPoints = Arrays.copyOf(mPoints, playerCount);
                // fill rest with DEFAULT_VALUE
                for (int i = oldSize; i < mPoints.length; i++) {
                    mPoints[i] = DEFAULT_VALUE;
                }
            }
        }
    }
    
    public int getMissingEntries() {
        int count = 0;
        for (int i = 0; i < mPoints.length; i++) {
            if (mPoints[i] == DEFAULT_VALUE) {
                count++;
            }
        }
        return count;
    }
    
    public void setValue(int index, int value) {
        mPoints[index] = value;
    }
    
    public int getValue(int index) {
        return mPoints[index];
    }
    
    @Override
    public String compact() {
        Compacter cmp = new Compacter(mPoints.length + 1);
        cmp.appendData(mPoints.length);
        for (int point : mPoints) {
            cmp.appendData(point);
        }
        cmp.appendData(mLaneName);
        cmp.appendData(mLaneComment);
        return cmp.compact();
    }

    @Override
    public void unloadData(Compacter compactedData)
            throws CompactedDataCorruptException {
        if (compactedData.getSize() <= 1) {
            throw new CompactedDataCorruptException("Too little data for MinigolfRound: ").setCorruptData(compactedData);
        }
        int pointsCount = compactedData.getInt(0);
        if (pointsCount <= 0 || compactedData.getSize() < pointsCount + 1) {
            throw new CompactedDataCorruptException("PointsCount too low: " + pointsCount).setCorruptData(compactedData);
        }
        mPoints = new int[pointsCount];
        for (int i = 0; i < mPoints.length; i++) {
            mPoints[i] = compactedData.getInt(i + 1);
        }
        int offset = pointsCount + 1;
        mLaneName = compactedData.getData(offset);
        mLaneComment = compactedData.getData(offset + 1);
    }
    
    @Override
    public int hashCode() {
        return Arrays.hashCode(mPoints);
    }
    
    @Override
    public boolean equals(Object other) {
        if (other instanceof MinigolfRound) {
            MinigolfRound o = (MinigolfRound) other;
            return Arrays.equals(mPoints, o.mPoints);
        } else {
            return super.equals(other);
        }
    }
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("MinigolfRound: Scores(")
        .append(Arrays.toString(mPoints))
        .append(")");
        return builder.toString();
    }

    public int[] getValues() {
        return mPoints;
    }

    public boolean isFinished() {
        for (int point : mPoints) {
            if (point == DEFAULT_VALUE) {
                return false;
            }
        }
        return true;
    }

    protected void swapPlayers(int index1, int index2) {
        int p1 = mPoints[index1];
        int p2= mPoints[index2];
        mPoints[index1] = p2;
        mPoints[index2] = p1;
    }

    public int getFirstIncompletePlayer() {
        for (int i = 0; i < mPoints.length; i++) {
            if (mPoints[i] == DEFAULT_VALUE) {
                return i;
            }
        }
        return mPoints.length - 1; // all complete
    }
}
