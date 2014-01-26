package dan.dit.gameMemo.appCore.numberInput;

import android.content.res.Resources;

public class MultiOperation extends AlgebraicOperation {
    public MultiOperation(double operand) {
        super(operand);
        if (AlgebraicOperation.isZero(operand)) {
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
    public String getName(Resources res) {
        return "*" + DOUBLE_FORMAT.format(mOperand);
    }

}
