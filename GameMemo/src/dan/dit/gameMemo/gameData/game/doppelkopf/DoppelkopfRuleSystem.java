package dan.dit.gameMemo.gameData.game.doppelkopf;

import dan.dit.gameMemo.util.compaction.CompactedDataCorruptException;
import dan.dit.gameMemo.util.compaction.Compacter;

/**
 * A singleton class that represents a collection of rules for a {@link DoppelkopfGame}. An object of this class
 * or any sub class is immutable.<br>
 * This class implements the tournament rules, subclasses can freely overwrite any method,
 * even return a different {@link DoppelkopfRoundResult} subclass.
 * @author Daniel
 *
 */
public class DoppelkopfRuleSystem {
	
	public static final String NAME_TOURNAMENT1 = "tournament1";
	private static DoppelkopfRuleSystem INSTANCE;

	private DoppelkopfRuleSystem() {}
	
	public int getScoreMultplier() { // applies to getTotalScore and getBidAndResultScore
		return 1;
	}
	
	public DoppelkopfRoundResult makeNeutralRoundResult() {
		return new DoppelkopfRoundResult();
	}
	
	public DoppelkopfRoundResult makeRoundResult(Compacter compactedData) throws CompactedDataCorruptException {
		return new DoppelkopfRoundResult(compactedData);
	}
	
	private int getWinner(DoppelkopfRound round) {
		// 0 = no winner, 1 = re won, 2 = contra won
		DoppelkopfRoundResult res = round.getRoundResult();
		DoppelkopfBid reBid = round.getReBid();
		DoppelkopfBid contraBid = round.getContraBid();
		boolean hasWinner = false;
		boolean reWon = false;
		// determine if there is a winner and who won, reWon variable is only valid if there is a winner
		if (reBid.isDefinitelyWon(true, res)) {
			hasWinner = reWon = true;
		} else if (contraBid.isDefinitelyWon(false, res)) {
			hasWinner = true;
			reWon = false;
		} else if (reBid.isDefinitelyLost(true, res) && (!contraBid.hasBid() || !contraBid.isDefinitelyLost(false, res))) {
			hasWinner = true;
			reWon = false;
		} else if (contraBid.isDefinitelyLost(false, res) && (!reBid.hasBid() || !reBid.isDefinitelyLost(true, res))) {
			hasWinner = true;
			reWon = true;
		}
		// try to handle a special case where the was no winner determined
		if (!hasWinner) {
			if (!reBid.hasBid() && contraBid.hasBid() && res.getReResult() >= DoppelkopfRoundResult.R_120) {
				hasWinner = reWon = true;
			}
		}
		return hasWinner ? (reWon ? 1 : 2) : 0;
	}
	
	public int getTotalScore(boolean re, DoppelkopfRound round) {
		int bidAndResultScore = getBidAndResultScore(re, round) / getScoreMultplier();
		
		// extra scores
		DoppelkopfExtraScore extra = round.getExtraScore();
		int mixedExtra = extra.getExtraScore(true) - extra.getExtraScore(false);
		
		// sum it up
		int sum = (re ? mixedExtra : -mixedExtra) + bidAndResultScore;
		return sum * ( re && round.isSolo() ? 3 : 1 ) * getScoreMultplier();
	}
	
	public int getBidAndResultScore(boolean re, DoppelkopfRound round) {
		DoppelkopfRoundResult res = round.getRoundResult();
		DoppelkopfBid reBid = round.getReBid();
		DoppelkopfBid contraBid = round.getContraBid();
		int pointsByBid = (reBid.hasBid() ? getScoreForReContraBid() : 0) + (contraBid.hasBid() ? getScoreForReContraBid() : 0) 
				+ reBid.getBidsWithoutReContra() + contraBid.getBidsWithoutReContra();
		int winnerCode = getWinner(round);
		boolean hasWinner = winnerCode != 0;
		boolean reWon = winnerCode == 1;
		// sum it up
		int sum = (hasWinner ? pointsByBid : 0) 
				+ getPointsForOverwin(reWon, res) 
				+ ((hasWinner) ? getPointsForWinner(reWon) : 0)
				+ (!reWon ? getScoreForGegenDieAlten(round.getRoundStyle()) : 0);
		int reSum = reWon ? sum : -sum;
		return (re ? reSum : -reSum) * getScoreMultplier();
	}
	
	private int getPointsForOverwin(boolean re, DoppelkopfRoundResult res) {
		int overwin = 0;
		if (re) {
			overwin = (res.getReResult() - DoppelkopfRoundResult.R_120) - 1;
		} else {
			overwin = (res.getContraResult() - DoppelkopfRoundResult.R_120) - 1;			
		}
		return overwin < 0 ? 0 : overwin;
	}
	
	public int getPointsForWinner(boolean re) {
		return 1;
	}
	
	public int getScoreForGegenDieAlten(DoppelkopfRoundStyle style) {
		return style instanceof DoppelkopfSolo && style.getType() != DoppelkopfSolo.STILLE_HOCHZEIT ? 0 : 1;
	}
	
	public int getScoreForReContraBid() {
		return 2;
	}
	
	public String getName() {
		return NAME_TOURNAMENT1;
	}
	
	@Override
	public boolean equals(Object other) {
		if (other instanceof DoppelkopfRuleSystem) {
			return ((DoppelkopfRuleSystem) other).getName().equals(getName());
		} else {
			return super.equals(other);
		}
	}

	public static DoppelkopfRuleSystem getInstanceByName(String data) {
		DoppelkopfRuleSystem sys = null;
		if (NAME_TOURNAMENT1.equals(data)) {
			if (INSTANCE == null) {
				INSTANCE = new DoppelkopfRuleSystem();
			}
			sys = INSTANCE;
		} 
		return sys;
	}

	public boolean enforcesDutySolo(DoppelkopfGame doppelkopfGame, int round) {
		if (doppelkopfGame.hasLimit()) {
			if (doppelkopfGame.getDurchlauf(round) >= doppelkopfGame.getLimit() && !doppelkopfGame.isFinished()) {
				return true;
			}
		}
		return false;
	}
}
