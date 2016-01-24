package dan.dit.gameMemo.gameData.game.binokel;

import dan.dit.gameMemo.gameData.player.AbstractPlayerTeam;
import dan.dit.gameMemo.gameData.player.Player;

/**
 * Created by daniel on 24.01.16.
 */
public class BinokelTeam {
    private int mIndex;
    private int mTotalScore;
    AbstractPlayerTeam mTeam;

    public BinokelTeam(int index, AbstractPlayerTeam team) {
        mIndex = index;
        mTeam = team;
    }

    public int getPlayerCount() {
        return mTeam.getPlayerCount();
    }

    public int getTeamIndex() {
        return mIndex;
    }

    public void setTotalScore(int value) {
        mTotalScore = value;
    }

    public void addTotalScore(int delta) {
        mTotalScore += delta;
    }

    public int getTotalScore() {
        return mTotalScore;
    }

    public AbstractPlayerTeam getPlayers() {
        return mTeam;
    }
}
