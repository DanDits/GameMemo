package dan.dit.gameMemo.appCore.numberInput;

import android.content.res.Resources;

public class AbsoluteOperation extends ConstructiveNumberOperation {
    private double mValue;
    public AbsoluteOperation(double value) {
        mValue = value;
    }
    
    @Override
    public double execute(double on) {
        return mValue;
    }

    @Override
    public Operation getInverseOperation() {
        return mInverse;
    }
    
    public double getValue() {
        return mValue;
    }

    @Override
    public String getName(Resources res) {
        return DOUBLE_FORMAT.format(mValue);
    }

    @Override
    public double getStartElement() {
        return 0.0;
    }

}
