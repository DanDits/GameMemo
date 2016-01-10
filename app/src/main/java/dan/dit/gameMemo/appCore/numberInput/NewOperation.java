package dan.dit.gameMemo.appCore.numberInput;

import android.content.res.Resources;
import dan.dit.gameMemo.R;


public class NewOperation extends Operation {

    private Operation mToAdd;
    private OperationCallback mCb;
    private int mPosition;
    
    public NewOperation(OperationCallback cb, Operation toAdd, int position) {
        mToAdd = toAdd;
        mCb = cb;
        mPosition = position;
    }
    
    public Operation getOperationToAdd() {
        return mToAdd;
    }
    
    @Override
    public double execute(double on) {
        mCb.newOperation(mToAdd, mPosition);
        return Double.NaN;
    }

    @Override
    public Operation getInverseOperation() {
        return new RemoveOperation(mCb, mToAdd, mPosition);
    }

    @Override
    public String getName(Resources res) {
        return res.getString(R.string.number_input_new_operation_name);
    }

}
