package dan.dit.gameMemo.gameData.statistics.tichu;

import java.util.LinkedList;
import java.util.List;

import dan.dit.gameMemo.gameData.game.Game;
import dan.dit.gameMemo.gameData.game.GameRound;
import dan.dit.gameMemo.gameData.game.tichu.TichuBid;
import dan.dit.gameMemo.gameData.game.tichu.TichuBidType;
import dan.dit.gameMemo.gameData.game.tichu.TichuGame;
import dan.dit.gameMemo.gameData.game.tichu.TichuRound;
import dan.dit.gameMemo.gameData.player.Player;
import dan.dit.gameMemo.gameData.player.AbstractPlayerTeam;
import dan.dit.gameMemo.gameData.statistics.GameStatistic;


public class TichuGameStatisticBuilder extends dan.dit.gameMemo.gameData.statistics.GameStatisticBuilder {
	private List<TichuGame> games = new LinkedList<TichuGame>();
	
	public TichuGameStatisticBuilder(List<Player> players) {
		super(players);
	}
	
	@Override
	public void addGame(Game game) {
		games.add((TichuGame) game);
	}

	@Override
	public boolean removeGame(Game game) {
		return games.remove((TichuGame) game);
	}

	@Override
	public GameStatistic build() {
		TichuStatistic stat = new TichuStatistic(players, games.size());
		for (int i = 0; i < players.size(); i++) {
			Player p = players.get(i);
			int gamesWonCount = 0;
			int gamesPlayedCount = 0;
			int gameRoundsPlayedCount = 0;
			int gameRoundsWonRawScoreCount = 0;
			int smallTichuBidCount = 0;
			int bigTichuBidCount = 0;
			int smallTichuBidWonCount = 0;
			int bigTichuBidWonCount = 0;
			int finisherCount = 0;
			int validFinisherPositionsCount = 0;
			int scoreRaw = 0;
			int scoreTichus = 0;
			for (TichuGame g : games) {
				int playerId = g.getPlayerId(p);
				if (playerId != TichuGame.INVALID_PLAYER_ID) {
					if (g.isFinished()) {
						gamesPlayedCount++;
					}
					AbstractPlayerTeam winner = g.getWinner();
					if (winner != null && winner.contains(p)) {
						gamesWonCount++;
					}
					for (GameRound r : g.getRounds()) {
						gameRoundsPlayedCount++;
						TichuRound round = (TichuRound) r;
						if (g.getTeam1().contains(p)) {
							scoreRaw += round.getRawScoreTeam1();
							if (round.getRawScoreTeam1() >= round.getRawScoreTeam2()) {
								gameRoundsWonRawScoreCount++;
							}
						} else {
							assert g.getTeam2().contains(p);
							scoreRaw += round.getRawScoreTeam2();
							if (round.getRawScoreTeam2() >= round.getRawScoreTeam1()) {
								gameRoundsWonRawScoreCount++;
							}
						}
						int finisherPos = round.getFinisherPos(playerId);
						if (finisherPos != TichuRound.FINISHER_POS_UNKNOWN) {
							finisherCount += finisherPos;
							validFinisherPositionsCount++;
						}
						// tichus
						TichuBid bid = round.getTichuBid(playerId);
						scoreTichus += bid.getScore();
						if (bid.getType().equals(TichuBidType.SMALL)) {
							smallTichuBidCount++;
							if (bid.isWon()) {
								smallTichuBidWonCount++;
							}
						} else if (bid.getType().equals(TichuBidType.BIG)) {
							bigTichuBidCount++;
							if (bid.isWon()) {
								bigTichuBidWonCount++;
							}
						}
					}
				}
			}

			stat.setPlayerStatistic(TichuStatisticType.GAMES_PLAYED, i, gamesPlayedCount);
			stat.setPlayerStatistic(TichuStatisticType.GAMES_WON_ABS, i, gamesWonCount);
			stat.setPlayerStatistic(TichuStatisticType.SMALL_TICHU_BIDS_ABS, i, smallTichuBidCount);
			stat.setPlayerStatistic(TichuStatisticType.SMALL_TICHUS_WON_ABS, i, smallTichuBidWonCount);
			stat.setPlayerStatistic(TichuStatisticType.BIG_TICHU_BIDS_ABS, i, bigTichuBidCount);
			stat.setPlayerStatistic(TichuStatisticType.BIG_TICHUS_WON_ABS, i, bigTichuBidWonCount);
			stat.setPlayerStatistic(TichuStatisticType.GAME_ROUNDS_PLAYED, i, gameRoundsPlayedCount);
			stat.setPlayerStatistic(TichuStatisticType.GAME_ROUNDS_WON_RAWSCORE_ABS, i, gameRoundsWonRawScoreCount);
			stat.setPlayerStatistic(TichuStatisticType.FINISHER_POS_AVERAGE, i, 
					validFinisherPositionsCount > 0 ? (finisherCount / (double) validFinisherPositionsCount) : 0);
			stat.setPlayerStatistic(TichuStatisticType.SCORE_PER_ROUND_NO_TICHUS, i, 
					gameRoundsPlayedCount > 0 ? scoreRaw / gameRoundsPlayedCount : 0);
			stat.setPlayerStatistic(TichuStatisticType.SCORE_PER_ROUND_INCL_TICHUS, i, 
					gameRoundsPlayedCount > 0 ? (scoreRaw + scoreTichus) / gameRoundsPlayedCount : 0);
		}
		return stat;
	}

	public void addGames(List<TichuGame> games) {
		for (TichuGame g : games) {
			addGame(g);
		}
	}

}
