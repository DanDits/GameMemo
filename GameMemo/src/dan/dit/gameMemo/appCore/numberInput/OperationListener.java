package dan.dit.gameMemo.appCore.numberInput;

/**
 * Listener that waits for some operation to happen, usually some number
 * input from the user.
 * @author Daniel
 *
 */
public interface OperationListener {
    SelectListenerOperation getSelectListenerOperation(); // the operation toselect this listener
    double getNumber(); // its own currently displayed number if any
    boolean operationExecuted(double result, Operation op); // the operation op operated on the number of this listener called if this listener is currently active 
    boolean isActive();
    void setActive(boolean active);
}
