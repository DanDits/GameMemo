package dan.dit.gameMemo.appCore.numberInput;

public abstract class AlgebraicOperation extends Operation {
    protected double mOperand;
    
    public AlgebraicOperation(double operand) {
        mOperand = operand;
        if (Double.isNaN(operand) || Double.isInfinite(operand)) {
            throw new IllegalArgumentException("Not an algebraic operand: " + mOperand);
        }
    }

    public double getOperand() {
        return mOperand;
    }

    public static boolean isZero(double num) {
        return Math.abs(num) < 1e-15;
    }

}
