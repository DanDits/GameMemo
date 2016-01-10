package dan.dit.gameMemo.gameData.game;
/**
 * This exception is used to signal that a {@link GameRound} could not be created from
 * the given data or parameters or is in an invalid state and therefore cannot be used anymore.
 * @author Daniel
 *
 */
public class InadequateRoundInfo extends Exception {
	private static final long serialVersionUID = 4513686839989955948L;

	/**
	 * Creates an InadequateRoundInfo exception with a description.
	 */
	public InadequateRoundInfo(String descr) {
		super(descr);
	}
	
	/**
	 * Creates an InadequateRoundInfo.
	 */
	public InadequateRoundInfo() {
		super();
	}

	/**
	 * Creates an InadequateRoundInfo with the given cause.
	 * @param cause The cause why the round info is inadequate.
	 */
	public InadequateRoundInfo(Throwable cause) {
		super(cause);
	}
}
