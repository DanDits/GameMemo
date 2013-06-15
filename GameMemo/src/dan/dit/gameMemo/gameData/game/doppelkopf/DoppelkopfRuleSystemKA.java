package dan.dit.gameMemo.gameData.game.doppelkopf;


public class DoppelkopfRuleSystemKA extends DoppelkopfRuleSystem {
	public static final String NAME_KA1 = "ka1";
	private static DoppelkopfRuleSystem INSTANCE;
	
	private DoppelkopfRuleSystemKA() {
		super(NAME_KA1, dan.dit.gameMemo.R.string.doppelkopf_rule_system_descr_ka1);
	}
	
	public int getScoreForReContraBid() {
		return 1;
	}
	
	public static DoppelkopfRuleSystem getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new DoppelkopfRuleSystemKA();
		}
		return INSTANCE;
	}
	
	public boolean keepsGiver(DoppelkopfGame doppelkopfGame, int roundIndex) {
		DoppelkopfRound round = (DoppelkopfRound) doppelkopfGame.getRound(roundIndex);
		return round.isSolo() && round.getRoundStyle().getType() != DoppelkopfSolo.STILLE_HOCHZEIT;
	}
	
	public boolean isFinished(DoppelkopfGame doppelkopfGame, int round) {
		boolean limitCond = doppelkopfGame.getLimit() == DoppelkopfGame.NO_LIMIT ? DoppelkopfGame.IS_FINISHED_WITHOUT_LIMIT : doppelkopfGame.getDurchlauf(round) >= doppelkopfGame.getLimit();
		boolean soliCond = doppelkopfGame.getRemainingSoli(round) == 0;
		return limitCond && soliCond;
	}
	
	public boolean enforcesDutySolo(DoppelkopfGame doppelkopfGame, int round) {
		// enforce duty soli after round limit is exceeded
		if (doppelkopfGame.hasLimit() && doppelkopfGame.getDutySoliCountPerPlayer() > 0) {
			if (doppelkopfGame.getDurchlauf(round) >= doppelkopfGame.getLimit() && !doppelkopfGame.isFinished()) {
				return true;
			}
		}
		return false;
	}
	
	public int getDefaultDurchlaeufe() {
		return 2;
	}

	public int getDefaultDutySoli() {
		return 0;
	}
}
