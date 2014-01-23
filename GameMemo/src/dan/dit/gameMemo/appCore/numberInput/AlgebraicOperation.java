package dan.dit.gameMemo.appCore.numberInput;

public abstract class AlgebraicOperation extends Operation {
    protected double mOperand;
    
    public AlgebraicOperation(double operand) {
        mOperand = operand;
    }

    public double getOperand() {
        return mOperand;
    }

}
