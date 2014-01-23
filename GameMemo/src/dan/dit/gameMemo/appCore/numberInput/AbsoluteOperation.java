package dan.dit.gameMemo.appCore.numberInput;

public class AbsoluteOperation extends Operation {
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
    public String getName() {
        return DOUBLE_FORMAT.format(mValue);
    }

}
