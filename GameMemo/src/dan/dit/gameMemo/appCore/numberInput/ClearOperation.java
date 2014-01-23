package dan.dit.gameMemo.appCore.numberInput;

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
    public String getName() {
        return "C";
    }

}
