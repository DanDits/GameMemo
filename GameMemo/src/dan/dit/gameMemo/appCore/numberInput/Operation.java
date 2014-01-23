package dan.dit.gameMemo.appCore.numberInput;

import java.text.NumberFormat;

public abstract class Operation {
    protected Operation mInverse;
    public abstract double execute(double on);
    
    public abstract Operation getInverseOperation();

    public abstract String getName();
    
    public int getIconResId() {
        return 0;
    }
    
    protected static final NumberFormat DOUBLE_FORMAT = NumberFormat.getNumberInstance();

    public void setInverse(Operation inverse) {
        mInverse = inverse;
    }
}
