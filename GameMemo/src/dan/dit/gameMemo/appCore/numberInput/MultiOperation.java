package dan.dit.gameMemo.appCore.numberInput;

public class MultiOperation extends AlgebraicOperation {
    public MultiOperation(double operand) {
        super(operand);
        if (Math.abs(operand) < 10E-15) {
            throw new IllegalArgumentException("Multiplying by zero is no invertible algebraic operation, build a SetOperation instead.");
        }
    }

    @Override
    public double execute(double on) {
        return on * mOperand;
    }

    @Override
    public Operation getInverseOperation() {
        if (mInverse == null) {
            mInverse = new DivideOperation(mOperand);
        }
        return mInverse;
    }

    @Override
    public String getName() {
        return "*" + DOUBLE_FORMAT.format(mOperand);
    }

}
