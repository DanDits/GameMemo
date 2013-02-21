package dan.dit.gameMemo.gameData.game.tichu;

import dan.dit.gameMemo.gameData.game.GameRound;
import dan.dit.gameMemo.gameData.game.InadequateRoundInfo;
import dan.dit.gameMemo.util.compression.CompressedDataCorruptException;
import dan.dit.gameMemo.util.compression.Compressor;
public class TichuRound extends GameRound {
	public static final int UNKNOWN_SCORE = 1337; // any illegal score
	public static final int FINISHER_POS_LAST = 4;
	public static final int FINISHER_POS_UNKNOWN = 5;
	private int rawScoreTeam1;
	private int rawScoreTeam2;
	private TichuBid[] tichus;
	private int[] finishers; // player 1 and 2 are in team 1, player 3 and 4 are in team 2
	
	protected TichuRound(Compressor data) throws CompressedDataCorruptException, InadequateRoundInfo {
		if (data.getSize() < 4) {
			throw new CompressedDataCorruptException("Too little data in compressor.").setCorruptData(data);
		}
		rawScoreTeam1 = Integer.parseInt(data.getData(0));
		rawScoreTeam2 = Integer.parseInt(data.getData(1));
		String finisherData = data.getData(2);
		finishers = new int[finisherData.length()];
		for (int i = 0; i < finishers.length; i++) {
			finishers[i] = Integer.parseInt(String.valueOf(finisherData.charAt(i)), 10);
		}
		String tichuData = data.getData(3);
		TichuBidType[] bids = new TichuBidType[TichuGame.TOTAL_PLAYERS];
		for (int i = 0; i < bids.length; i++) {
			bids[i] = TichuBidType.getFromKey(String.valueOf(tichuData.charAt(i)));
		}
		finishers = completeFinishers(finishers, rawScoreTeam1, rawScoreTeam2);
		tichus = makeTichus(bids, finishers[0]);
		checkAndThrowTichuRoundState();
	}
	
	private TichuRound(int score1, int score2, int[] minOneFinishers, TichuBid[] tichus) throws InadequateRoundInfo {
		this.rawScoreTeam1 = score1;
		this.rawScoreTeam2 = score2;
		this.finishers = minOneFinishers;
		this.tichus = tichus;
		checkAndThrowTichuRoundState();
	}
	
	public static final TichuRound buildRound(int score1, int score2, int[] minOneFinishers, TichuBidType[] tichuBids) {
		if (!checkFinisherArray(minOneFinishers)) {
			throw new IllegalArgumentException("Given finisher array is not valid.");
		} else if (tichuBids.length != TichuGame.TOTAL_PLAYERS) {
			throw new IllegalArgumentException(TichuGame.TOTAL_PLAYERS + " tichu bid types needed.");
		}
		int[] finishers = minOneFinishers;
		int scoreTeam1;
		int scoreTeam2;
		if (minOneFinishers.length > 1 && areFirstTwoFinishersInSameTeam(minOneFinishers)) {
			// manually set up scores, so then the constructor will work even if both scores are UNKNOWN
			// finisher array is more trustworthy than the given scores
			if (minOneFinishers[0] == TichuGame.PLAYER_ONE_ID || minOneFinishers[0] == TichuGame.PLAYER_TWO_ID) {
				//team 1 finished first
				scoreTeam1 = 200;
				scoreTeam2 = 0;
			} else {
				scoreTeam1 = 0;
				scoreTeam2 = 200;
			}
		} else if (score1 == UNKNOWN_SCORE && score2 == UNKNOWN_SCORE) {
			return null; //Both scores are unknown and its not a clear 200:0
		} else {
			// try to reconstruct the scores 
			if (isValidScore(score1, score2, minOneFinishers)) {
				// both scores were known and valid together, perfect
				assert score1 != UNKNOWN_SCORE && score2 != UNKNOWN_SCORE;
				scoreTeam1 = score1;
				scoreTeam2 = score2;
			} else if (isValidScore(score1)) {
				assert score1 != UNKNOWN_SCORE;
				scoreTeam1 = score1;
				scoreTeam2 = calculateOtherTeamScore(score1, minOneFinishers, true);
				if (scoreTeam2 == UNKNOWN_SCORE) {
					// failed calculation of team2 score, lets make a last try to calculate team1 score if possible
					if (isValidScore(score2)) {
						scoreTeam2 = score2;
						scoreTeam1 = calculateOtherTeamScore(score2, minOneFinishers, false);
						if (scoreTeam1 == UNKNOWN_SCORE) {
							return null; //Could not calculate score for team 1 or 2 from other score
						}
					} else {
						return null; // Could not calculate score for team 2 from score1
					}
				}
			} else if (isValidScore(score2)) {
				assert score2 != UNKNOWN_SCORE;
				scoreTeam2 = score2;
				scoreTeam1 = calculateOtherTeamScore(score2, minOneFinishers, false);
				if (scoreTeam1 == UNKNOWN_SCORE) {
					return null; //Could not calculate score for team 1 from score2
				}
			} else {
				//at least one score was known, but still both were invalid
				return null; //Could not create TichuRound from scores score1 and score2
			}
		}
		finishers = completeFinishers(finishers, scoreTeam1, scoreTeam2);
		TichuBid[] tichus = makeTichus(tichuBids, finishers[0]);
		TichuRound round = null;
		try {
			round = new TichuRound(scoreTeam1, scoreTeam2, finishers, tichus);
		} catch (InadequateRoundInfo iri) {
			assert false; // since it filtered out everything inadequate
			return null;
		}
		return round;
	}

	public int getRawScoreTeam1() {
		return rawScoreTeam1;
	}
	
	public int getRawScoreTeam2() {
		return rawScoreTeam2;
	}
	
	public int getScoreTeam1(boolean useMercyRule) {
		return calculateScore(true, useMercyRule);
	}
	
	public int getScoreTeam2(boolean useMercyRule) {
		return calculateScore(false, useMercyRule);
	}
	
	private int calculateScore(boolean team1, boolean useMercyRule) {
		int friendIndex1, friendIndex2, enemyIndex1, enemyIndex2;
		int rawScore;
		if (team1) {
			friendIndex1 = 0;
			friendIndex2 = 1;
			enemyIndex1 = 2;
			enemyIndex2 = 3;
			rawScore = rawScoreTeam1;
		} else {
			friendIndex1 = 2;
			friendIndex2 = 3;
			enemyIndex1 = 0;
			enemyIndex2 = 1;
			rawScore = rawScoreTeam2;
		}
		if (!useMercyRule) {
			int tichuPoints = tichus[friendIndex1].getScore() + tichus[friendIndex2].getScore();
			return tichuPoints + rawScore;
		} else {
			// with mercy rule one does not lose points for a lost tichu
			int tichuPoints = Math.max(0, tichus[friendIndex1].getScore()) + Math.max(0, tichus[friendIndex2].getScore());
			// one gains points for lost tichu bids of the enemy
			tichuPoints += -Math.min(0, tichus[enemyIndex1].getScore()) - Math.min(0, tichus[enemyIndex2].getScore());
			return tichuPoints + rawScore;
		}
	}
	
	public TichuBid getTichuBid(int playerId) {
		return tichus[playerId - TichuGame.PLAYER_ONE_ID];
	}
	
	public int getFinisherPos(int playerId) {
		for (int i = 0; i < finishers.length; i++) {
			if (finishers[i] == playerId) {
				return i + 1;
			}
		}
		if (finishers.length > 2 && areFirstTwoFinishersInSameTeam(finishers)) {
			return FINISHER_POS_LAST;
		} else {
			return FINISHER_POS_UNKNOWN;
		}
	}
	
	@Override
	public String compress() {
		Compressor cmp = new Compressor();
		// score team1 in slot 0
		cmp.appendData(rawScoreTeam1);
		//score team2 in slot 1
		cmp.appendData(rawScoreTeam2);
		// finishers in slot 2
		StringBuilder finisherData = new StringBuilder(TichuGame.TOTAL_PLAYERS);
		for (int i : finishers) {
			finisherData.append(i);
		}
		cmp.appendData(finisherData.toString());
		// tichus in slot 3
		StringBuilder tichuData = new StringBuilder(TichuGame.TOTAL_PLAYERS);
		for (TichuBid bid : tichus) {
			tichuData.append(bid.getType().getKey());
		}
		cmp.appendData(tichuData.toString());
		return cmp.compress();
	}
	
	/**
	 * Returns the estimated score of the other team, given the first team has the given score.
	 * This estimation is inaccurate for 200:0 situations.
	 * @param ofScore The given score of one team.
	 * @return The estimated score of the other team. UNKNOWN_SCORE if given score is no valid score.
	 */
	public static int getEstimatedOtherTeamScore(int ofScore) {
		if (!isValidScore(ofScore)) {
			return UNKNOWN_SCORE;
		}
		if (ofScore == 200) {
			return 0;
		} else {
			return 100 - ofScore; // so we always return 100 if given score is 0, even though it could be 200
		}
	}
	
	private static int calculateOtherTeamScore(int teamScore, int[] finishers, boolean givenIsTeam1Score) {
		assert checkFinisherArray(finishers);
		assert isValidScore(teamScore);
		if (finishers.length > 1 && areFirstTwoFinishersInSameTeam(finishers)) {
			if (teamScore == 0 && isInTeam(finishers[0], givenIsTeam1Score)) {
				return UNKNOWN_SCORE; // team 1 has score 0 even though they finished first and second?!
			} else if (teamScore == 200 && isInTeam(finishers[0], !givenIsTeam1Score)) {
				return UNKNOWN_SCORE; // team 1 has score 200 even though the other players finished first and second?!
			} else {
				// if teamscore is 0 then other players finished first and get 200, 
				// else if teamscore is 200 then they finished first and other get 0, else given score is invalid
				return teamScore == 0 ? 200 : (teamScore == 200 ? 0 : UNKNOWN_SCORE); 
			}
		} else if (finishers.length == 1) {
			if (teamScore == 200 && isInTeam(finishers[0], !givenIsTeam1Score)) {
				return UNKNOWN_SCORE;
			} else if (teamScore == 200) {
				return 0;
			} else if (teamScore == 0) {
				if (isInTeam(finishers[0], givenIsTeam1Score)) {
					return 100; // the first finisher is in the team that scored 0, no 200:0 possible
				} else {
					return UNKNOWN_SCORE; // since a player of the other team finished first it can be a 100:0 or 200:0
				}
			} else {
				return 100 - teamScore;
			}
		} else {
			// got more than one finisher and the first two are not in the same team, no 200:0 possible here
			if (teamScore == 200) {
				return UNKNOWN_SCORE; // not possible
			} else {
				return 100 - teamScore;
			}
		}
	}
	
	public static boolean isInTeam(int playerId, boolean team1) {
		if (team1) {
			return playerId == TichuGame.PLAYER_ONE_ID || playerId == TichuGame.PLAYER_TWO_ID;
		} else {
			return playerId == TichuGame.PLAYER_THREE_ID || playerId == TichuGame.PLAYER_FOUR_ID;
		}
	}
	
	public static boolean isValidScore(int score) {
		return score % 5 == 0 && ((score >= -25 && score <= 125) || score == 200);
	}
	
	public static boolean isValidScore(int firstScore, int secondScore, int[] finishers) {
		if ( isValidScore(firstScore) && isValidScore(secondScore) 
				&& ((firstScore + secondScore == 100) || (firstScore == 0 && secondScore == 200) || (firstScore == 200 && secondScore == 0))) {
			//score combination is valid, now check if it is possible to score this combination with the given finishers
			if (firstScore == 200 && (isInTeam(finishers[0], false)
					|| (finishers.length > 1 && isInTeam(finishers[1], false)))) {
				return false;
			} else if (secondScore == 200 && (isInTeam(finishers[0], true)
					|| (finishers.length > 1 && isInTeam(finishers[1], true)))) {
				return false;
			} else {
				return true;
			}
		}
		return false;
	}
	
	private static boolean checkFinisherArray(int[] finishers) {
		if (finishers.length >= 1 && finishers.length <= TichuGame.TOTAL_PLAYERS) {
			// check if given array contains only numbers from PLAYER_ONE_ID to PLAYER_FOUR_ID 
			// (both inclusive) and no duplicates
			for (int i = 0; i < finishers.length ; i++) {
				if (finishers[i] < TichuGame.PLAYER_ONE_ID || finishers[i] > TichuGame.PLAYER_FOUR_ID) {
					return false; // illegal player id
				} else {
					for (int j = i + 1; j < finishers.length; j++) {
						if (finishers[i] == finishers[j]) {
							return false; // duplicate player id
						}
					}
				}
			}
			return true;
		}
		return false;
	}
	
	private static int[] completeFinishers(int[] finishers, int scoreTeam1, int scoreTeam2) {
		assert checkFinisherArray(finishers);
		if (finishers.length == 1) {
			if (scoreTeam1 == 200) {
				assert scoreTeam2 == 0 && isInTeam(finishers[0], true);
				return new int[] {finishers[0], finishers[0] == TichuGame.PLAYER_ONE_ID ? TichuGame.PLAYER_TWO_ID : TichuGame.PLAYER_ONE_ID};
			} else if (scoreTeam2 == 200) {
				assert scoreTeam1 == 0 && isInTeam(finishers[0], false);
				return new int[] {finishers[0], finishers[0] == TichuGame.PLAYER_THREE_ID ? TichuGame.PLAYER_FOUR_ID : TichuGame.PLAYER_THREE_ID};	
			}
			return finishers; // cannot complete anything if its not a 200:0
		}
		if (areFirstTwoFinishersInSameTeam(finishers)) {
			// the last two players did not finish, no need to save them
			assert (scoreTeam1 == 200 && scoreTeam2 == 0 && isInTeam(finishers[0], true))
					|| (scoreTeam1 == 0 && scoreTeam2 == 200 && isInTeam(finishers[0], false));
			return finishers.length > 2 ? new int[] {finishers[0], finishers[1]} : finishers; 
		}
		if (finishers.length == 3) {
			int sum = 0;
			for (int i = 0; i < finishers.length; i++) {
				sum += finishers[i];
			}
			int missingPlayerId = TichuGame.PLAYER_ONE_ID + TichuGame.PLAYER_TWO_ID + TichuGame.PLAYER_THREE_ID + TichuGame.PLAYER_FOUR_ID - sum;
			return new int[] {finishers[0], finishers[1], finishers[2], missingPlayerId};
		}
		return finishers;
	}
	
	private static TichuBid[] makeTichus(TichuBidType[] bids, int firstFinisher) {
		TichuBid[] tichus = new TichuBid[bids.length];
		for (int i = 0; i < bids.length; i++) {
			tichus[i] = new TichuBid(bids[i], firstFinisher == i + TichuGame.PLAYER_ONE_ID);
		}
		return tichus;
	}
	
	private static boolean areFirstTwoFinishersInSameTeam(int[] finishers) {
		assert checkFinisherArray(finishers);
		return (isInTeam(finishers[0], true) && isInTeam(finishers[1], true))
				||  (isInTeam(finishers[0], false) && isInTeam(finishers[1], false));
	}
	
	private void checkAndThrowTichuRoundState() throws InadequateRoundInfo {
		boolean scoreInvalid = !isValidScore(rawScoreTeam1, rawScoreTeam2, finishers);
		boolean tichusInvalid = tichus.length != TichuGame.TOTAL_PLAYERS;
		for (int i = 0; i < tichus.length; i++) {
			if (tichus[i] == null) {
				tichusInvalid = true;
			} else if (tichus[i].isWon() && finishers[0] != i + TichuGame.PLAYER_ONE_ID) {
				tichusInvalid = true;
			} else if (!tichus[i].isWon() && finishers[0] == i + TichuGame.PLAYER_ONE_ID) {
				tichusInvalid = true;
			}
		}
		boolean finishersInvalid = !checkFinisherArray(finishers);
		if (rawScoreTeam1 == 200 && ((finishers[0] != TichuGame.PLAYER_ONE_ID && finishers[0] != TichuGame.PLAYER_TWO_ID) 
				|| (finishers[1] != TichuGame.PLAYER_ONE_ID && finishers[1] != TichuGame.PLAYER_TWO_ID))) {
			finishersInvalid = true;
		} else if (rawScoreTeam2 == 200 && ((finishers[0] != TichuGame.PLAYER_THREE_ID && finishers[0] != TichuGame.PLAYER_FOUR_ID) 
				|| (finishers[1] != TichuGame.PLAYER_THREE_ID && finishers[1] != TichuGame.PLAYER_FOUR_ID))) {
			finishersInvalid = true;
		}
		if (scoreInvalid || tichusInvalid || finishersInvalid) {
			throw new InadequateRoundInfo("TichuRound in illegal state: Scoreinvalid(" + scoreInvalid + "), tichusInvalid(" + tichusInvalid
					+ "), finishersInvalid(" + finishersInvalid + ").");
		}
	}
	
	@Override
	public boolean equals(Object other) {
		if (other instanceof TichuRound) {
			TichuRound o = (TichuRound) other;
			boolean scoreEqual = rawScoreTeam1 == o.rawScoreTeam1 && rawScoreTeam2 == o.rawScoreTeam2;
			boolean finishersEqual = finishers.length == o.finishers.length;
			if (finishersEqual) {
				for (int i = 0; i < finishers.length; i++) {
					if (finishers[i] != o.finishers[i]) {
						finishersEqual = false;
					}
				}
			}
			boolean tichusEqual = tichus.length == o.tichus.length;
			if (tichusEqual) {
				for (int i = 0; i < tichus.length; i++) {
					if (!tichus[i].equals(o.tichus[i])) {
						tichusEqual = false;
					}
				}
			}
			return scoreEqual && finishersEqual && tichusEqual;
		} else {
			return super.equals(other);
		}
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("TichuRound: Score(");
		builder.append(rawScoreTeam1);
		builder.append(":");
		builder.append(rawScoreTeam2);
		builder.append("), Players(");
		for (int i = 0; i < finishers.length; i++) {
			builder.append(finishers[i]);
			builder.append("[");
			builder.append(tichus[finishers[i] - 1]);
			builder.append("]");
			if (i < finishers.length - 1) {
				builder.append(", ");
			}
		}
		builder.append(")");
		return builder.toString();
	}
}
