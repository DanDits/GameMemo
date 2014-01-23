package dan.dit.gameMemo.appCore.numberInput;

public class RemoveOperation extends Operation {
    private OperationCallback mCb;
    private Operation mToRemove;
    
    public RemoveOperation(OperationCallback cb, Operation toRemove) {
        mCb = cb;
        mToRemove = toRemove;
    }
    
    @Override
    public double execute(double on) {
        mCb.removeOperation(mToRemove);
        return Double.NaN;
    }

    @Override
    public Operation getInverseOperation() {
        return new NewOperation(mCb, mToRemove);
    }

    @Override
    public String getName() {
        return "Remove";
    }

}
