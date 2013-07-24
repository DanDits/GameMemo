package dan.dit.gameMemo.gameData.statistics;

import java.util.Collection;
import java.util.List;

import dan.dit.gameMemo.gameData.game.Game;
import dan.dit.gameMemo.gameData.player.Player;

public abstract class GameStatisticBuilder {
	protected final List<Player> players;
	
	public interface StatisticBuildCompleteListener {
		void statisticComplete(GameStatistic result);
	}

	public GameStatisticBuilder(List<Player> players) {
		this.players = players;
	}
	
	public final List<Player> getPlayers() {
		return players;
	}

	public abstract void addGame(Game game);
	
	public void addGames(Collection<Game> games) {
		for (Game g : games) {
			addGame(g);
		}
	}
	
	public abstract boolean removeGame(Game game);
	public abstract GameStatistic build();
}
