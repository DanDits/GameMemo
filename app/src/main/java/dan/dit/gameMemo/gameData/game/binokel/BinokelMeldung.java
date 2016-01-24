package dan.dit.gameMemo.gameData.game.binokel;

import dan.dit.gameMemo.util.compaction.Compactable;
import dan.dit.gameMemo.util.compaction.CompactedDataCorruptException;
import dan.dit.gameMemo.util.compaction.Compacter;

/**
 * Created by daniel on 24.01.16.
 */
public class BinokelMeldung implements Compactable {
    private int mPlayerIndex;
    private int mTeamIndex;
    private int mScore;

    public BinokelMeldung(int teamIndex, int playerIndex) {
        mTeamIndex = teamIndex;
        mPlayerIndex = playerIndex;
    }

    protected BinokelMeldung(Compacter data) throws CompactedDataCorruptException {
        unloadData(data);
    }

    public int getScore() {
        return mScore;
    }

    public void setScore(int score) {
        this.mScore = score;
    }

    @Override
    public String compact() {
        Compacter cmp = new Compacter(1);
        cmp.appendData(mScore);
        return cmp.compact();
    }

    @Override
    public void unloadData(Compacter compactedData) throws CompactedDataCorruptException {
        if (compactedData == null || compactedData.getSize() < 1) {
            throw new CompactedDataCorruptException("Too little data given.");
        }
        mScore = compactedData.getInt(0);
    }
}
