package dan.dit.gameMemo.appCore.numberInput;

public interface OnOperationClickedListener {
    void onOperationClickedBeforeExecute(Operation which);
    void onOperationClickedAfterExecute(Operation which);
}
