package dan.dit.gameMemo.gameData.game.doppelkopf;


public class DoppelkopfBid {
	public static final int NONE = 0;
	public static final int RE_CONTRA = 1;
	public static final int OHNE_90 = 2;
	public static final int OHNE_60 = 3;
	public static final int OHNE_30 = 4;
	public static final int SCHWARZ = 5;
	
	protected int mType;
	
	public DoppelkopfBid() {
		mType = NONE;
	}
	
	public DoppelkopfBid(int bidType) {
		if (bidType < 0 || bidType > SCHWARZ) {
			throw new IllegalArgumentException("Illegal bid type = " + bidType);
		}
		mType = bidType;
	}
	
	public int getNextBidType() {
		// cycle through bids
		return (mType + 1 ) % (SCHWARZ + 1);
	}
	
	public void nextBid() {
		mType = getNextBidType();		
	}
	
	public int getType() {
		return mType;
	}
	
	public boolean hasBid() {
		return mType != NONE;
	}
	
	public int getBidsWithoutReContra() {
		return mType >= RE_CONTRA ? mType - RE_CONTRA : 0;
	}

	/**
	 * Check if the bid was definitely won by the given party and the given RoundResult.
	 * @param re <code>true</code> to check if the re party has definitely won. 
	 * <code>false</code> to check if the contra party has definitely won.
	 * @param res The RoundResult.
	 * @return <code>true</code> if the result (re or contra) is sufficient for this bid.
	 * If <code>false</code> you cannot be sure since the case of a result of 120 and a contra bid requires
	 * the re party to only have 120. (Maybe also other cases involving 120 points)
	 */
	public boolean isDefinitelyWon(boolean re, DoppelkopfRoundResult res) {
		int resType = re ? res.getReResult() : res.getContraResult();
		switch (mType) {
		case NONE:
			// Watch case: re also wins without bid when it has 120, and contra made >= contra bid!
			return re ? resType >= DoppelkopfRoundResult.R_121_150 : resType >= DoppelkopfRoundResult.R_120;
		case RE_CONTRA:
			return resType >= DoppelkopfRoundResult.R_121_150; 
		case OHNE_90:
			return resType >= DoppelkopfRoundResult.R_151_180;
		case OHNE_60:
			return resType >= DoppelkopfRoundResult.R_181_210;
		case OHNE_30:
			return resType >= DoppelkopfRoundResult.R_211_240;
		case SCHWARZ:
			return resType >= DoppelkopfRoundResult.R_WHITE;
		}
		return false;
	}
	
	/**
	 * Check if the bid was definitely lost by the given party and the given RoundResult.
	 * @param re <code>true</code> to check if the re party has definitely lost. 
	 * <code>false</code> to check if the contra party has definitely lost.
	 * @param res The RoundResult.
	 * @return <code>true</code> if the result (re or contra) is insufficient for this bid.
	 * If <code>false</code> you cannot be sure of the result. (Cases involving 120 points).
	 */
	public boolean isDefinitelyLost(boolean re, DoppelkopfRoundResult res) {
		int resType = re ? res.getReResult() : res.getContraResult();
		switch (mType) {
		case NONE:
			return resType <= DoppelkopfRoundResult.R_90_119;
		case RE_CONTRA:
			return resType <= DoppelkopfRoundResult.R_120;				
		case OHNE_90:
			return resType <= DoppelkopfRoundResult.R_121_150;	
		case OHNE_60:
			return resType <= DoppelkopfRoundResult.R_151_180;	
		case OHNE_30:
			return resType <= DoppelkopfRoundResult.R_181_210;			
		case SCHWARZ:
			return resType <= DoppelkopfRoundResult.R_211_240;
		}
		return false;
	}
	
	@Override
	public boolean equals(Object other) {
		if (other instanceof DoppelkopfBid) {
			return mType == ((DoppelkopfBid) other).mType;
		} else {
			return super.equals(other);
		}
	}
	
	@Override
	public int hashCode() {
		return mType;
	}
	
	@Override
	public String toString() {
		switch (mType) {
		case NONE:
			return "None";
		case RE_CONTRA:
			return "Re/Contra";
		case OHNE_30:
			return "Ohne30";
		case OHNE_60:
			return "Ohne60";
		case OHNE_90:
			return "Ohne90";
		case SCHWARZ:
			return "Schwarz";
		}
		return "";
	}
}
