package dan.dit.gameMemo.appCore.numberInput;

public class DivideOperation extends AlgebraicOperation {

    public DivideOperation(double operand) {
        super(operand);
        if (Math.abs(operand) < 10E-15) {
            throw new IllegalArgumentException("Dividing by zero is no (invertible) algebraic operation.");
        }
    }

    @Override
    public double execute(double on) {
        return on / mOperand;
    }

    @Override
    public Operation getInverseOperation() {
        if (mInverse == null) {
            mInverse = new MultiOperation(mOperand);
        }
        return mInverse;
    }

    @Override
    public String getName() {
        return "/" + DOUBLE_FORMAT.format(mOperand);
    }

}
