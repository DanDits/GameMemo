package dan.dit.gameMemo.appCore.numberInput;

/**
 * Describes an operation that, when used, will apply a constructive change to the
 * current number, instead of meta operations that only alter other operations.<br>
 * Known direct subclasses: {@link AlgebraicOperation}, {@link SignOperation},
 * {@link AbsoluteOperation}.
 * @author Daniel
 *
 */
public abstract class ConstructiveNumberOperation extends Operation {
    /**
     * Returns the start element to apply the operation on if there is
     * currently no number given (NaN).
     * @return The start element.
     */
    public abstract double getStartElement();
}
