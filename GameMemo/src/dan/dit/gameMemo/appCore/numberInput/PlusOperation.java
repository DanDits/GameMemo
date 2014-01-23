package dan.dit.gameMemo.appCore.numberInput;

public class PlusOperation extends AlgebraicOperation {

    public PlusOperation(double operand) {
        super(operand);
    }

    @Override
    public double execute(double on) {
        return on + mOperand;
    }

    @Override
    public Operation getInverseOperation() {
        if (mInverse == null) {
            mInverse = new MinusOperation(mOperand);
        }
        return mInverse;
    }

    @Override
    public String getName() {
        return "+" + DOUBLE_FORMAT.format(mOperand);
    }

}
