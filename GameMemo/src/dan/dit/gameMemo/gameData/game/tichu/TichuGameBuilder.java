package dan.dit.gameMemo.gameData.game.tichu;

import dan.dit.gameMemo.gameData.game.GameBuilder;
import dan.dit.gameMemo.gameData.game.InadequateRoundInfo;
import dan.dit.gameMemo.gameData.player.PlayerDuo;
import dan.dit.gameMemo.util.compression.CompressedDataCorruptException;
import dan.dit.gameMemo.util.compression.Compressor;

public class TichuGameBuilder extends GameBuilder {
	
	public TichuGameBuilder() {
		mInst = new TichuGame();
	}
	
	@Override
	public GameBuilder loadPlayer(Compressor playerData) throws CompressedDataCorruptException {
		if (playerData.getSize() != TichuGame.TOTAL_PLAYERS) {
			throw new CompressedDataCorruptException("A tichu game needs 4 players, not " + playerData.getSize())
			.setCorruptData(playerData);
		}
		// parsing player data
		String player1 = playerData.getData(0);
		String player2 = playerData.getData(1);
		String player3 = playerData.getData(2);
		String player4 = playerData.getData(3);
		if (!TichuGame.areValidPlayers(player1, player2, player3, player4)) {
			throw new CompressedDataCorruptException("Loading players failed. Illegal or equal names.")
			.setCorruptData(playerData);
		}
		PlayerDuo firstTeam = new PlayerDuo(TichuGame.PLAYERS.populatePlayer(player1), TichuGame.PLAYERS.populatePlayer(player2));
		PlayerDuo secondTeam = new PlayerDuo(TichuGame.PLAYERS.populatePlayer(player3), TichuGame.PLAYERS.populatePlayer(player4));
		((TichuGame) mInst).setupPlayers(firstTeam.getFirst(), firstTeam.getSecond(), secondTeam.getFirst(), secondTeam.getSecond());
		return this;
	}

	@Override
	public GameBuilder loadMetadata(Compressor metaData) throws CompressedDataCorruptException {
		// parsing meta data, no data is required to construct a valid game
		
		if (metaData.getSize() >= 1) {
			String usesMercyRuleData = metaData.getData(0);
			boolean mercyRuleEnabled = TichuGame.META_DATA_USES_MERCY_RULE.equals(usesMercyRuleData);
			((TichuGame) mInst).mMercyRuleEnabled = mercyRuleEnabled;
		}
		if (metaData.getSize() >= 2) {
			String scoreLimitData = metaData.getData(1);
			int scoreLimit;
			try {
				scoreLimit = Integer.parseInt(scoreLimitData);
			} catch (NumberFormatException nfe) {
				throw new CompressedDataCorruptException("Could not parse score limit meta data.", nfe);
			}
			if (scoreLimit >= TichuGame.MIN_SCORE_LIMIT && scoreLimit <= TichuGame.MAX_SCORE_LIMIT) {
				((TichuGame) mInst).mScoreLimit = scoreLimit;
			}
		}
		return this;
	}

	@Override
	public GameBuilder loadRounds(Compressor roundsData) throws CompressedDataCorruptException {
		// parsing round data
		for (String round : roundsData) {
			TichuRound currTichuRound = null;
			try {
				currTichuRound = new TichuRound(new Compressor(round));
			} catch (InadequateRoundInfo e) {
				throw new CompressedDataCorruptException("Could not parse round, info is not adequate.", e);
			}
			if (currTichuRound != null) {
				if (mInst.isFinished()) {
					break;
				}
				mInst.addRound(currTichuRound);
			}
		}
		return this;
	}

}
