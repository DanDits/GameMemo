package dan.dit.gameMemo.gameData.game.user;

import java.util.List;

import android.content.ContentResolver;
import android.content.res.Resources;
import dan.dit.gameMemo.gameData.game.Game;
import dan.dit.gameMemo.gameData.game.GameKey;
import dan.dit.gameMemo.gameData.game.GameRound;
import dan.dit.gameMemo.gameData.player.AbstractPlayerTeam;
import dan.dit.gameMemo.gameData.player.Player;
import dan.dit.gameMemo.gameData.player.PlayerPool;

public class UserGame extends Game {
    public static final String NAME = "User game";
    public static final PlayerPool PLAYERS = new PlayerPool();
    
    // metadata
    private boolean mHighestScoreWins;
    private double mStartScore;
    
    @Override
    public AbstractPlayerTeam getWinner() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void addRound(GameRound round) {
        // TODO Auto-generated method stub

    }

    @Override
    public void reset() {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean isFinished() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    protected void setupPlayers(List<Player> players) {
        // TODO Auto-generated method stub

    }

    @Override
    protected String getPlayerData() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected int getWinnerData() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    protected String getMetaData() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void synch() {
        // TODO Auto-generated method stub

    }

    @Override
    public int getKey() {
        return GameKey.USERGAME;
    }

    @Override
    public String getFormattedInfo(Resources res) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void saveGame(ContentResolver resolver) {
        super.saveGame(null, resolver);
    }

}
