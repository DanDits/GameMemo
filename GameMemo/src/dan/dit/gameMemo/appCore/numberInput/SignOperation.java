package dan.dit.gameMemo.appCore.numberInput;

public class SignOperation extends Operation {

    @Override
    public double execute(double on) {
        return -on;
    }

    @Override
    public Operation getInverseOperation() {
        return this;
    }

    @Override
    public String getName() {
        return "±";
    }

}
