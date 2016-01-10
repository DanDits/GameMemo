package dan.dit.gameMemo.appCore.numberInput;

import java.text.NumberFormat;

import android.content.res.Resources;

/**
 * An operation in this context is something that can be added to a {@link NumberInputController} 
 * and then be executed. Usually each operation should define its own inverse and be invertible, if this is not the
 * case then the history will be cleaned as soon as this operation is used.<br>
 * 
 * @author Daniel
 *
 */
public abstract class Operation {
    protected Operation mInverse;
    
    /**
     * Execute the operation (on the given number).
     * @param on The number to execute the operation on, can be ignored.
     * @return The resulting number of the operation. If there is no real result, best use
     * Double.NaN.
     */
    public abstract double execute(double on);
    
    /**
     * Returns the inverse operation to this operation which will, when executed after
     * this operation is executed on the number returned from execute(), result in the previous number 
     * and state.
     * @return The inverse operation or <code>null</code> if there is no inverse.
     */
    public abstract Operation getInverseOperation();

    /**
     * Returns the display name of this operation.
     * @param res The resource to load the display name from.
     * @return The display name.
     */
    public abstract String getName(Resources res);
    
    /**
     * Returns the icon id used for displaying the operation in the UI.
     * @return The icon drawable resource id or by default 0 for no icon.
     */
    public int getIconResId() {
        return 0;
    }
    
    /**
     * When displaying a double value, use this to uniformally format it.
     */
    protected static final NumberFormat DOUBLE_FORMAT = NumberFormat.getNumberInstance();

    /**
     * Setter for the inverse.
     * @param inverse The inverse or <code>null</code>.
     */
    public void setInverse(Operation inverse) {
        mInverse = inverse;
    }
}
