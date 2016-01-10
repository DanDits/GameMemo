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
        if (Double.isNaN(on)) {
            return 1.0 / mOperand;
        } else if (Double.isInfinite(on)) {
            return ((on == Double.POSITIVE_INFINITY && mOperand >= 0.0) 
                    || (on == Double.NEGATIVE_INFINITY && mOperand <= 0.0)) ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
        }
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

    @Override
    public double getStartElement() {
        return 1.0;
    }

}
