package dan.dit.gameMemo.appCore.binokel;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import dan.dit.gameMemo.appCore.gameSetup.GameSetupOptionsController;
import dan.dit.gameMemo.gameData.game.GameKey;

/**
 * Created by daniel on 10.01.16.
 */
public class GameSetupOptions extends GameSetupOptionsController {


    public GameSetupOptions(Context context, ViewGroup container, Bundle parameters) {
        super(context, container, parameters, GameKey.BINOKELGAME);
    }

    //TODO implement methods
    @Override
    protected View init(LayoutInflater inflater) {
        return null;
    }

    @Override
    public void reset() {

    }

    @Override
    public void prepareParameters() {

    }
}
