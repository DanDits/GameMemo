package dan.dit.gameMemo.appCore.numberInput;

import android.content.res.Resources;

public class MultiOperation extends AlgebraicOperation {
    public MultiOperation(double operand) {
        super(operand);
        if (AlgebraicOperation.isZero(operand)) {
            throw new IllegalArgumentException("Multiplying by zero is no invertible algebraic operation, build an AbsoluteOperation instead.");
        }
    }

    @Override
    public double execute(double on) {
        if (Double.isNaN(on)) {
            return mOperand;
        } else if (Double.isInfinite(on)) {
            // remember that mOperand is not zero by assertion to MultiOperation
            return ((on == Double.POSITIVE_INFINITY && mOperand >= 0.0) 
                    || (on == Double.NEGATIVE_INFINITY && mOperand <= 0.0)) ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
        }
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

    @Override
    public double getStartElement() {
        return 1.0;
    }

}
