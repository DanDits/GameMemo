package dan.dit.gameMemo.gameData.game.custom;

import java.util.Arrays;

import dan.dit.gameMemo.gameData.game.GameRound;
import dan.dit.gameMemo.util.compaction.CompactedDataCorruptException;
import dan.dit.gameMemo.util.compaction.Compacter;

public class CustomRound extends GameRound {
    public static final double DEFAULT_VALUE_SINGLE_ROUND = Double.NaN;
    public static final double DEFAULT_VALUE_ROUND_BASED = 0.0;
    private double[] mPoints;
    
    protected CustomRound(Compacter data) throws CompactedDataCorruptException {
        unloadData(data);
    }
    
    public CustomRound(int teamCount, boolean isRoundBased) {
        setTeamCount(teamCount, isRoundBased);
    }
    
    public void setTeamCount(int teamCount, boolean isRoundBased) {
        if (teamCount <= 0) {
            throw new IllegalArgumentException("At least one team required for a CustomRound.");
        }
        if (mPoints == null || teamCount != mPoints.length) {
            if (mPoints == null) {
                // create a new point array filled with DEFAULT_VALUE
                mPoints = new double[teamCount];
                Arrays.fill(mPoints, isRoundBased ? DEFAULT_VALUE_ROUND_BASED : DEFAULT_VALUE_SINGLE_ROUND);
            } else {
                // first copy the old point array
                int oldSize = mPoints.length;
                double[] temp = new double[teamCount];
                for (int i = 0; i < Math.min(teamCount, oldSize); i++) {
                    temp[i] = mPoints[i];
                }
                mPoints = temp;
                // fill rest with DEFAULT_VALUE
                for (int i = oldSize; i < mPoints.length; i++) {
                    mPoints[i] = isRoundBased ? DEFAULT_VALUE_ROUND_BASED : DEFAULT_VALUE_SINGLE_ROUND;
                }
            }
        }
    }
    
    public void setValue(int index, double value) {
        mPoints[index] = value;
    }
    
    public double getValue(int index) {
        return mPoints[index];
    }
    
    @Override
    public String compact() {
        Compacter cmp = new Compacter(mPoints.length + 1);
        cmp.appendData(mPoints.length);
        for (double point : mPoints) {
            cmp.appendData(point);
        }
        return cmp.compact();
    }

    @Override
    public void unloadData(Compacter compactedData)
            throws CompactedDataCorruptException {
        if (compactedData.getSize() <= 1) {
            throw new CompactedDataCorruptException("Too little data for CustomRound: ").setCorruptData(compactedData);
        }
        int pointsCount = compactedData.getInt(0);
        if (pointsCount <= 0 || compactedData.getSize() < pointsCount + 1) {
            throw new CompactedDataCorruptException("PointsCount too low: " + pointsCount).setCorruptData(compactedData);
        }
        mPoints = new double[pointsCount];
        for (int i = 0; i < mPoints.length; i++) {
            mPoints[i] = compactedData.getDouble(i + 1);
        }
        //int offset = pointsCount + 1; // starting from offset additional metadata (if any) can be read
    }
    
    @Override
    public int hashCode() {
        return Arrays.hashCode(mPoints);
    }
    
    @Override
    public boolean equals(Object other) {
        if (other instanceof CustomRound) {
            CustomRound o = (CustomRound) other;
            return Arrays.equals(mPoints, o.mPoints);
        } else {
            return super.equals(other);
        }
    }
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("CustomRound: Scores(")
        .append(Arrays.toString(mPoints))
        .append(")");
        return builder.toString();
    }

    public double[] getValues() {
        return mPoints;
    }

    protected void swapTeams(int index1, int index2) {
        double p1 = mPoints[index1];
        double p2= mPoints[index2];
        mPoints[index1] = p2;
        mPoints[index2] = p1;
    }
    
    public void removeTeam(int teamIndex) {
        double[] newPoints = new double[mPoints.length - 1];
        int delta = 0;
        for (int i = 0; i < mPoints.length; i++) {
            if (i != teamIndex) {
                newPoints[i + delta] = mPoints[i];
            } else {
                delta = -1;
            }
        }
        mPoints = newPoints;
    }
}
