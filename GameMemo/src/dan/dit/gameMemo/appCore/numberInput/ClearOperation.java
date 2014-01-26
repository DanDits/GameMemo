package dan.dit.gameMemo.appCore.numberInput;

import android.content.res.Resources;

public class ClearOperation extends Operation {

    @Override
    public double execute(double on) {
        return Double.NaN;
    }

    @Override
    public Operation getInverseOperation() {
        return mInverse;
    }

    @Override
    public String getName(Resources res) {
        return "C";
    }

}
