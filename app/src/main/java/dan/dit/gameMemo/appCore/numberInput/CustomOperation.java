package dan.dit.gameMemo.appCore.numberInput;

import android.content.res.Resources;

public class CustomOperation extends Operation {
    
    private boolean mIsInverse = false;
    private String mKey;
    
    public CustomOperation(String key) {
        mKey = key;
    }
    
    @Override
    public double execute(double on) {
        return Double.NaN;
    }

    @Override
    public Operation getInverseOperation() {
        if (mIsInverse) {
            return new CustomOperation(mKey); // someone got the idea to get the inverse of the inverse... ok
        } else if (mInverse == null) {
            mInverse = new CustomOperation(mKey);
            ((CustomOperation) mInverse).mIsInverse = true;
        }      
        return mInverse;
    }
    
    public boolean isInverse() {
        return mIsInverse;
    }
    
    public String getKey() {
        return mKey;
    }
    
    @Override
    public String getName(Resources res) {
        return "Custom: " + mKey;
    }

}
