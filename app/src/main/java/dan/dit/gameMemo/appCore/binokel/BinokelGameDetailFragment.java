package dan.dit.gameMemo.appCore.binokel;

import android.os.Bundle;

import java.util.List;

import dan.dit.gameMemo.appCore.GameDetailFragment;
import dan.dit.gameMemo.appCore.tichu.TichuGameDetailFragment;
import dan.dit.gameMemo.gameData.game.Game;
import dan.dit.gameMemo.gameData.game.GameKey;
import dan.dit.gameMemo.gameData.player.Player;
import dan.dit.gameMemo.gameData.player.PlayerPool;
import dan.dit.gameMemo.storage.GameStorageHelper;

/**
 * Created by daniel on 10.01.16.
 */
public class BinokelGameDetailFragment extends GameDetailFragment {

    public static BinokelGameDetailFragment newInstance(long gameId) {
        BinokelGameDetailFragment f = new BinokelGameDetailFragment();

        // Supply index input as an argument.
        Bundle args = new Bundle();
        args.putLong(GameStorageHelper.getCursorItemType(GameKey.BINOKELGAME), gameId);
        f.setArguments(args);

        return f;
    }

    public static BinokelGameDetailFragment newInstance(Bundle extras) {
        BinokelGameDetailFragment f = new BinokelGameDetailFragment();
        f.setArguments(extras);
        return f;
    }

    //TODO implement methods
    @Override
    protected boolean isImmutable() {
        return false;
    }

    @Override
    protected void deselectRound() {

    }

    @Override
    protected void selectRound(int position) {

    }

    @Override
    protected Game getGame() {
        return null;
    }

    @Override
    protected int getGameKey() {
        return 0;
    }

    @Override
    protected void setInfoText(CharSequence main, CharSequence extra) {

    }

    @Override
    public PlayerPool getPool() {
        return null;
    }

    @Override
    public List<Player> toFilter() {
        return null;
    }

    @Override
    public void playerChosen(int arg, Player chosen) {

    }

    @Override
    public void onPlayerColorChanged(int arg, Player concernedPlayer) {

    }

    @Override
    public void playerRemoved(int arg, Player removed) {

    }

}
