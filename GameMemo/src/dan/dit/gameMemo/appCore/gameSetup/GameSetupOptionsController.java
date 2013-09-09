package dan.dit.gameMemo.appCore.gameSetup;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public abstract class GameSetupOptionsController {
    protected View mRoot;
    protected int mGameKey;
    protected ViewGroup mContainer;
    protected Context mContext;
    protected Bundle mParameters;
    
    protected GameSetupOptionsController() {} // only used by NoOptions subclass
    public GameSetupOptionsController(Context context, ViewGroup container, Bundle parameters, int gameKey) {
        mGameKey = gameKey;
        mContainer = container;
        mParameters = parameters;
        mContext = context;
        mRoot = init((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE));
        mContainer.addView(mRoot);
    }
    
    protected abstract View init(LayoutInflater inflater);
    public abstract void reset();
    public abstract void prepareParameters();
    
    public Bundle getParameters() {
        prepareParameters();
        return mParameters;
    }
    
}
