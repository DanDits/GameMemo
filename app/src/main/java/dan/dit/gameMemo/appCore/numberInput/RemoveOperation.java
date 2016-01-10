package dan.dit.gameMemo.appCore.numberInput;

import android.content.res.Resources;

public class RemoveOperation extends Operation {
    private OperationCallback mCb;
    private Operation mToRemove;
    private int mPosition;
    
    public RemoveOperation(OperationCallback cb, Operation toRemove, int position) {
        mCb = cb;
        mToRemove = toRemove;
        mPosition = position;
    }
    
    @Override
    public double execute(double on) {
        mCb.removeOperation(mToRemove);
        return Double.NaN;
    }

    @Override
    public Operation getInverseOperation() {
        return new NewOperation(mCb, mToRemove, mPosition);
    }

    @Override
    public String getName(Resources res) {
        return "Remove";
    }

}
