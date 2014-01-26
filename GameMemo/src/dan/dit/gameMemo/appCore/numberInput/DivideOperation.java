package dan.dit.gameMemo.appCore.numberInput;

import android.content.res.Resources;

public class DivideOperation extends AlgebraicOperation {

    public DivideOperation(double operand) {
        super(operand);
        if (AlgebraicOperation.isZero(operand)) {
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
    public String getName(Resources res) {
        return "/" + DOUBLE_FORMAT.format(mOperand);
    }

}
