package dan.dit.gameMemo.gameData.statistics.tichu;


public enum TichuStatisticType {
	GAMES_PLAYED(), GAMES_WON_ABS(GAMES_PLAYED),  
	GAME_ROUNDS_PLAYED(), GAME_ROUNDS_WON_RAWSCORE_ABS(GAME_ROUNDS_PLAYED),	
	SCORE_PER_ROUND_NO_TICHUS(), SCORE_PER_ROUND_INCL_TICHUS(),
	SMALL_TICHU_BIDS_ABS(), SMALL_TICHUS_WON_ABS(SMALL_TICHU_BIDS_ABS),  
	BIG_TICHU_BIDS_ABS(), BIG_TICHUS_WON_ABS(BIG_TICHU_BIDS_ABS),
	FINISHER_POS_AVERAGE(false);
	
	public static final int DEFAULT_MINIMUM_ROUNDS_FOR_MEANINGFUL = 20;
	public static final int DEFAULT_MINIMUM_GAMES_FOR_MEANINGFUL = 5;
	
	private TichuStatisticType absoluteType;
	private String name;
	private boolean higherIsBetter = true;
	
	private TichuStatisticType(TichuStatisticType absoluteType) {
		this(absoluteType.isHigherBetter());
		this.absoluteType = absoluteType;
	}
	
	private TichuStatisticType(boolean isHigherBetter) {
		this();
		this.higherIsBetter = isHigherBetter;
	}
	
	private TichuStatisticType() {
	}
	
	public TichuStatisticType getAbsoluteType() {
		return absoluteType;
	}
	
	/**
	 * Returns <code>true</code> if a higher value is 'better' than a lower
	 * value. This should be used for default sorting behavior only since the
	 * interpretation of 'better' might be subjective.
	 * @return If a higher value is 'better'.
	 */
	public boolean isHigherBetter() {
		return higherIsBetter;
	}
	
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		if (name == null) {
			return super.toString();
		} else {
			return name;
		}
	}

	public boolean isMeaningfulData(double data, double roundsPlayed, double gamesPlayed) {
		switch (this) {
		default:
			return roundsPlayed >= DEFAULT_MINIMUM_ROUNDS_FOR_MEANINGFUL && gamesPlayed >= DEFAULT_MINIMUM_GAMES_FOR_MEANINGFUL;
		}
	}
}
