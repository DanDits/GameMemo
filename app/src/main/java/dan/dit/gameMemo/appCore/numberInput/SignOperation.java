package dan.dit.gameMemo.appCore.numberInput;

import android.content.res.Resources;

public class SignOperation extends ConstructiveNumberOperation {

    @Override
    public double execute(double on) {
        return -on;
    }

    @Override
    public Operation getInverseOperation() {
        return this;
    }

    @Override
    public String getName(Resources res) {
        return "±";
    }

    @Override
    public double getStartElement() {
        return 0.0;
    }

}
