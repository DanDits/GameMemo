package dan.dit.gameMemo.appCore.gameSetup;

import android.view.LayoutInflater;
import android.view.View;

public class GameSetupNoOptions extends GameSetupOptionsController {
    public static final GameSetupNoOptions INSTANCE = new GameSetupNoOptions();
    
    private GameSetupNoOptions() {
        super();
    }

    @Override
    protected View init(LayoutInflater inflater) {
        return null; // nothing to initialize
    }

    @Override
    public void reset() {
        // do nothing
    }

    @Override
    public void prepareParameters() {
        // do nothing
    }

}
