package dan.dit.gameMemo.appCore.numberInput;

import android.content.res.Resources;

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
    public String getName(Resources res) {
        return "+" + DOUBLE_FORMAT.format(mOperand);
    }

}
