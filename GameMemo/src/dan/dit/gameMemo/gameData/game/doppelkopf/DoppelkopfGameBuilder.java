package dan.dit.gameMemo.gameData.game.doppelkopf;

import java.util.ArrayList;
import java.util.List;

import dan.dit.gameMemo.gameData.game.GameBuilder;
import dan.dit.gameMemo.gameData.player.Player;
import dan.dit.gameMemo.util.compaction.CompactedDataCorruptException;
import dan.dit.gameMemo.util.compaction.Compacter;

public class DoppelkopfGameBuilder extends GameBuilder {

	public DoppelkopfGameBuilder() {
		mInst = new DoppelkopfGame();
	}
	
	@Override
	public GameBuilder loadMetadata(Compacter metaData)
			throws CompactedDataCorruptException {
		DoppelkopfGame game = ((DoppelkopfGame) mInst);
		if (metaData.getSize() < 3) {
			throw new CompactedDataCorruptException("Too little data for Doppelkopf metaData.").setCorruptData(metaData);
		}
		game.mRuleSystem = DoppelkopfRuleSystem.getInstanceByName(metaData.getData(0));
		if (game.mRuleSystem == null) {
			game.mRuleSystem = DoppelkopfRuleSystem.getInstanceByName(DoppelkopfRuleSystem.NAME_TOURNAMENT1);
		}
		int roundLimit = DoppelkopfGame.extractRoundLimitOfMetadata(metaData);
		game.mRoundLimit = roundLimit < 0 ? DoppelkopfGame.NO_LIMIT : roundLimit;
		
		int dutySoliPerPlayer = DoppelkopfGame.extractDutySoliOfMetadata(metaData);
		game.mDutySoliCountPerPlayer = dutySoliPerPlayer < 0 ? 0 : dutySoliPerPlayer;
		return this;
	}

	@Override
	public GameBuilder loadPlayer(Compacter playerData)
			throws CompactedDataCorruptException {
		if (playerData.getSize() < DoppelkopfGame.MIN_PLAYERS) {
			throw new CompactedDataCorruptException("A doppelkopf game needs at most 4 players, not " + playerData.getSize())
			.setCorruptData(playerData);
		}
		// parsing player data
		String player1 = playerData.getData(0);
		String player2 = playerData.getData(1);
		String player3 = playerData.getData(2);
		String player4 = playerData.getData(3);
		String player5 = playerData.getSize() >= 5 ? playerData.getData(4) : null;
		
		List<Player> players = new ArrayList<Player>(DoppelkopfGame.MAX_PLAYERS);
		players.add(DoppelkopfGame.PLAYERS.populatePlayer(player1));
		players.add(DoppelkopfGame.PLAYERS.populatePlayer(player2));
		players.add(DoppelkopfGame.PLAYERS.populatePlayer(player3));
		players.add(DoppelkopfGame.PLAYERS.populatePlayer(player4));
		if (Player.isValidPlayerName(player5)) {
			players.add(DoppelkopfGame.PLAYERS.populatePlayer(player5));
		}
		((DoppelkopfGame) mInst).setupPlayers(players);
		return this;
	}

	@Override
	public GameBuilder loadRounds(Compacter roundData)
			throws CompactedDataCorruptException {
		// parsing round data
		for (String round : roundData) {
			DoppelkopfRound currDKRound = null;
			currDKRound = new DoppelkopfRound(((DoppelkopfGame) mInst).getRuleSystem(), new Compacter(round));
			if (currDKRound != null) {
				if (mInst.isFinished()) {
					break;
				}
				mInst.addRound(currDKRound);
			}
		}
		return this;
	}

}
