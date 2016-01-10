package dan.dit.gameMemo.gameData.game.tichu;
/**
 * The three possible tichu bid types for a player in a TichuRound:
 * a big or a small tichu or none.<br>
 * Each type has a key to identify it uniquely and a score if
 * the bid is won.
 * @author Daniel
 *
 */
public enum TichuBidType {
	BIG("T", 200), SMALL("t", 100), NONE("n", 0);
	
	private String key;
	private int score;
	
	private TichuBidType(String key, int score) {
		this.key = key;
		this.score = score;
	}
	
	/**
	 * Returns the key of the bid type.
	 * @return The key.
	 */
	public String getKey() {
		return key;
	}
	
	/**
	 * Returns the score of the bid type.
	 * @return The score.
	 */
	public int getScore() {
		return score;
	}
	
	/**
	 * Returns the {@link TichuBidType} identified by the given key or
	 * <code>null</code> if there is no type for the given key.
	 * @param key The key.
	 * @return The TichuBidType for the key or <code>null</code>.
	 */
	public static TichuBidType getFromKey(String key) {
		if (key.equals(BIG.key))
			return BIG;
		else if (key.equals(SMALL.key))
			return SMALL;
		else if (key.equals(NONE.key))
			return NONE;
		else
			return null;
	}
}
