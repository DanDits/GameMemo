package dan.dit.gameMemo.gameData.game.doppelkopf;

import java.util.HashMap;
import java.util.Map;

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
	protected static final Map<String, DoppelkopfRuleSystem> ALL = new HashMap<String, DoppelkopfRuleSystem>();
	private static DoppelkopfRuleSystem INSTANCE;
	
	static {
		// init all (sub)classes
		DoppelkopfRuleSystem.getInstance();
		DoppelkopfRuleSystemKA.getInstance();
	}
	
	public static DoppelkopfRuleSystem[] getAllSystems() {
		DoppelkopfRuleSystem[] all = new DoppelkopfRuleSystem[ALL.size()];
		int index = 0;
		for (DoppelkopfRuleSystem sys : ALL.values()) {
			all[index++] = sys;
		}
		return all;
	}
	
	private String mName;
	private int mDescriptionResource;
	protected DoppelkopfRuleSystem(String name, int descrResource) {
		mName = name;
		mDescriptionResource = descrResource;
		ALL.put(name, this);
	}
	
	public int getScoreMultplier() { // applies to getTotalScore and getBidAndResultScore, unequal to zero
		return 1;
	}
	
	public DoppelkopfRoundResult makeNeutralRoundResult() {
		return new DoppelkopfRoundResult();
	}
	
	public DoppelkopfRoundResult makeRoundResult(Compacter compactedData) throws CompactedDataCorruptException {
		return new DoppelkopfRoundResult(compactedData);
	}
	
	protected int getWinner(DoppelkopfRound round) {
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
				+ ((hasWinner) ? getPointsForOverwinningEnemyBid(reWon, round) : 0)
				+ (!reWon ? getScoreForGegenDieAlten(round.getRoundStyle()) : 0);
		int reSum = reWon ? sum : -sum;
		return (re ? reSum : -reSum) * getScoreMultplier();
	}
	
	protected int getPointsForOverwin(boolean re, DoppelkopfRoundResult res) {
		int overwin = 0;
		if (re) {
			overwin = (res.getReResult() - DoppelkopfRoundResult.R_120) - 1;
		} else {
			overwin = (res.getContraResult() - DoppelkopfRoundResult.R_120) - 1;			
		}
		return overwin < 0 ? 0 : overwin;
	}
	
	protected int getPointsForOverwinningEnemyBid(boolean reWon, DoppelkopfRound round) {
	    // get points for getting a higher round result than required because of the enemy bid
	    DoppelkopfBid enemyBid = reWon ? round.getContraBid() : round.getReBid();
	    int ownResult = reWon ? round.getRoundResult().getReResult() : round.getRoundResult().getContraResult();
	    switch (enemyBid.getType()) {
	    case DoppelkopfBid.OHNE_90:
	        return ownResult == DoppelkopfRoundResult.R_120 ? 1 :
	            ownResult > DoppelkopfRoundResult.R_120 ? ownResult - DoppelkopfRoundResult.R_120 :
	                0;
	    case DoppelkopfBid.OHNE_60:
           return ownResult == DoppelkopfRoundResult.R_90_119 ? 1 :
                  ownResult == DoppelkopfRoundResult.R_120 ? 2 :
                ownResult > DoppelkopfRoundResult.R_120 ? 1 + ownResult - DoppelkopfRoundResult.R_120 :
                    0;
	    case DoppelkopfBid.OHNE_30:
           return ownResult == DoppelkopfRoundResult.R_60_89 ? 1 :
                ownResult == DoppelkopfRoundResult.R_90_119 ? 2 :
                ownResult == DoppelkopfRoundResult.R_120 ? 3 :
                ownResult > DoppelkopfRoundResult.R_120 ? 2 + ownResult - DoppelkopfRoundResult.R_120 :
                    0;
	    case DoppelkopfBid.SCHWARZ:
           return ownResult == DoppelkopfRoundResult.R_30_59 ? 1 :
                ownResult == DoppelkopfRoundResult.R_60_89 ? 2 :
                ownResult == DoppelkopfRoundResult.R_90_119 ? 3 :
                ownResult == DoppelkopfRoundResult.R_120 ? 4 :
                ownResult > DoppelkopfRoundResult.R_120 ? 3 + ownResult - DoppelkopfRoundResult.R_120 :
                    0;
	    default:
	        return 0;
	    }
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
		return mName;
	}
	
	public int getDescriptionResource() {
		return mDescriptionResource;
	}
	
	@Override
	public boolean equals(Object other) {
		if (other instanceof DoppelkopfRuleSystem) {
			return ((DoppelkopfRuleSystem) other).getName().equals(getName());
		} else {
			return super.equals(other);
		}
	}
	public static DoppelkopfRuleSystem getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new DoppelkopfRuleSystem(NAME_TOURNAMENT1, dan.dit.gameMemo.R.string.doppelkopf_rule_system_descr_tournament1);
		}
		return INSTANCE;
	}
	
	public static DoppelkopfRuleSystem getInstanceByName(String data) {
		DoppelkopfRuleSystem sys = ALL.get(data);
		return sys;
	}

	public boolean keepsGiver(DoppelkopfGame doppelkopfGame, int roundIndex) {
		DoppelkopfRound round = (DoppelkopfRound) doppelkopfGame.getRound(roundIndex);
		return round.isSolo() && round.getRoundStyle().getType() != DoppelkopfSolo.STILLE_HOCHZEIT 
				&& (enforcesDutySolo(doppelkopfGame, roundIndex) || 
						doppelkopfGame.getSoloCount(round.getRoundStyle().getFirstIndex(), roundIndex) <= doppelkopfGame.getDutySoliCountPerPlayer());
	}
	
	public boolean isFinished(DoppelkopfGame doppelkopfGame, int round) {
		// finished when all rounds are played and all soli done
		boolean limitCond = (!doppelkopfGame.hasLimit()) ? 
					DoppelkopfGame.IS_FINISHED_WITHOUT_LIMIT : 
					(doppelkopfGame.getRoundCount() >= doppelkopfGame.getLimit() * doppelkopfGame.getPlayerCount());
		boolean soliCond = isSoliConditionFulfilled(doppelkopfGame, round);
		return limitCond && soliCond;
	}
	
	protected boolean isSoliConditionFulfilled(DoppelkopfGame game, int round) {
	    return game.getRemainingSoli(round) == 0;
	}

    public boolean isLockStateAcceptable(DoppelkopfGame game,
            boolean state_locked) {
        // If the lock state is acceptable then the game can be considered finished according to the rule system even though
        // isFinished() might think differently
        if (!state_locked) {
            return true; // always allowed to unlock a game since these games get no special treatment
        } else {
            return !game.hasLimit() && isSoliConditionFulfilled(game, game.getRoundCount());
        }
    }

	public boolean enforcesDutySolo(DoppelkopfGame doppelkopfGame, int round) {
		if (doppelkopfGame.hasLimit() && doppelkopfGame.getDutySoliCountPerPlayer() > 0) {
			if (!isFinished(doppelkopfGame, round)) {
				int remainingGames = doppelkopfGame.getLimit() * doppelkopfGame.getPlayerCount() - round;
				return remainingGames <= doppelkopfGame.getRemainingSoli(round);
			}
		}
		return false;
	}

	public int getDefaultDurchlaeufe() {
		return 4;
	}

	public int getDefaultDutySoli() {
		return 1;
	}

}
