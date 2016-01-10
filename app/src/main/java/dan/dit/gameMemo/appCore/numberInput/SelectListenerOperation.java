package dan.dit.gameMemo.appCore.numberInput;

import android.content.res.Resources;

public class SelectListenerOperation extends Operation {
    
    private OperationListener mListener;
    private SelectListenerOperation mInverse;
    
    public SelectListenerOperation(OperationCallback cb, OperationListener listener) {
        OperationListener activeListener = cb.getActiveListener();
        if (activeListener == null) {
            mInverse = this; // self inverse if there is previously none selected
        } else {
            mInverse =  activeListener.getSelectListenerOperation();
        }
        mListener = listener;
    }
    
    @Override
    public double execute(double on) {
        mListener.setActive(true);
        return Double.NaN;
    }

    @Override
    public Operation getInverseOperation() {
        return mInverse;
    }

    @Override
    public String getName(Resources res) {
        return "Select Listener";
    }

    public OperationListener getListener() {
        return mListener;
    }

}
