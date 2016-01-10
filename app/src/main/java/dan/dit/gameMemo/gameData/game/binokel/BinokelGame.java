package dan.dit.gameMemo.gameData.game.binokel;

import android.content.ContentResolver;
import android.content.res.Resources;

import java.util.List;

import dan.dit.gameMemo.gameData.game.Game;
import dan.dit.gameMemo.gameData.game.GameRound;
import dan.dit.gameMemo.gameData.player.AbstractPlayerTeam;
import dan.dit.gameMemo.gameData.player.Player;
import dan.dit.gameMemo.gameData.player.PlayerPool;

/**
 * Created by daniel on 10.01.16.
 */
public class BinokelGame extends Game {
    public static final String GAME_NAME = "Binokel";
    public static final PlayerPool PLAYERS = new PlayerPool();
    //TODO implements methods
    @Override
    public AbstractPlayerTeam getWinner() {
        return null;
    }

    @Override
    public void addRound(GameRound round) {

    }

    @Override
    public void reset() {

    }

    @Override
    public boolean isFinished() {
        return false;
    }

    @Override
    protected void setupPlayers(List<Player> players) {

    }

    @Override
    protected String getPlayerData() {
        return null;
    }

    @Override
    protected int getWinnerData() {
        return 0;
    }

    @Override
    protected String getMetaData() {
        return null;
    }

    @Override
    public void synch() {

    }

    @Override
    public int getKey() {
        return 0;
    }

    @Override
    public String getFormattedInfo(Resources res) {
        return null;
    }

    @Override
    public void saveGame(ContentResolver resolver) {

    }
}
