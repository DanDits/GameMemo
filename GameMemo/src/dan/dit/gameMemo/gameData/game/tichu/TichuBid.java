package dan.dit.gameMemo.gameData.game.tichu;

/**
 * A TichuBid is a simple structure that consists of
 * a {@link TichuBidType} and is either won or lost.
 * @author Daniel
 *
 */
public class TichuBid {
	private boolean won;
	private TichuBidType type;
	
	/**
	 * Creates a new TichuBid with the given type and winning state.
	 * @param type The type that must not be <code>null</code>.
	 * @param won If the bid is won.
	 */
	public TichuBid(TichuBidType type, boolean won) {
		this.won = won;
		this.type = type;
		if (type == null) {
			throw new IllegalArgumentException("Tichu bid type cannot be null.");
		}
	}
	
	/**
	 * Returns the type of the bid.
	 * @return The type.
	 */
	public TichuBidType getType() {
		return type;
	}
	
	/**
	 * Returns if this bid is won or not.
	 * @return If it is won.
	 */
	public boolean isWon() {
		return won;
	}
	
	/**
	 * Returns the score of this bid. If the bid is won
	 * this is the positive score of the bid type, else the negative
	 * score of the bid type.
	 * @return The score of this bid.
	 */
	public int getScore() {
		return won ? type.getScore() : -type.getScore();
	}
	
	@Override
	public int hashCode() {
		return type.hashCode() + 5 * (won ? 1 : 0); // 5 is a random number greater than the amount of TichuBidTypes
	}
	
	@Override
	public boolean equals(Object other) {
		if (other instanceof TichuBid) {
			return type.equals(((TichuBid) other).type) && won == ((TichuBid) other).won;
		} else {
			return super.equals(other);
		}
	}
	
	@Override
	public String toString() {
		return type.getKey() + (won ? "(won)" : "(lost)");
	}
}
