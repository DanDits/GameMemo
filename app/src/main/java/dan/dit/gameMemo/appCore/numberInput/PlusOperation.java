package dan.dit.gameMemo.appCore.numberInput;

import android.content.res.Resources;

public class PlusOperation extends AlgebraicOperation {

    public PlusOperation(double operand) {
        super(operand);
    }

    @Override
    public double execute(double on) {
        if (Double.isNaN(on)) {
            return mOperand;
        } else if (Double.isInfinite(on)) {
            // remember on is finite
            return (on == Double.POSITIVE_INFINITY) ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
        }
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
    public String getName(Resources res) {
        return "+" + DOUBLE_FORMAT.format(mOperand);
    }

    @Override
    public double getStartElement() {
        return 0.0;
    }

}
