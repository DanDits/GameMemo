package dan.dit.gameMemo.appCore.numberInput;

public class NewOperation extends Operation {

    private Operation mToAdd;
    private OperationCallback mCb;
    
    public NewOperation(OperationCallback cb, Operation toAdd) {
        mToAdd = toAdd;
        mCb = cb;
    }
    
    @Override
    public double execute(double on) {
        mCb.newOperation(mToAdd);
        return Double.NaN;
    }

    @Override
    public Operation getInverseOperation() {
        return new RemoveOperation(mCb, mCb.getLastCreatedOperation());
    }

    @Override
    public String getName() {
        return "New";
    }

}
