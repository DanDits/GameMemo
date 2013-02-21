package dan.dit.gameMemo.gameData.statistics.tichu;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dan.dit.gameMemo.gameData.player.Player;
import dan.dit.gameMemo.gameData.statistics.GameStatistic;


public class TichuStatistic extends GameStatistic {
	private List<Player> players;
	private Map<TichuStatisticType, double[]> stat;
	private int gamesCount;
	
	TichuStatistic(List<Player> players, int gamesCount) {
		this.players = players;
		this.gamesCount = gamesCount;
		this.stat = new HashMap<TichuStatisticType, double[]>(TichuStatisticType.values().length);
	}
	
	void setPlayerStatistic(TichuStatisticType key, int playerIndex, double value) {
		double[] data = stat.get(key);
		if (data == null) {
			data = new double[players.size()];
			stat.put(key, data);
		}
		data[playerIndex] = value;
	}
	
	public double getStatistic(TichuStatisticType key, int playerIndex) {
		return stat.get(key)[playerIndex];
	}
	
	public double[] getStatistic(TichuStatisticType key) {
		return stat.get(key);
	}
	
	public double[] getStatistics(int playerIndex) {
		TichuStatisticType[] types = TichuStatisticType.values();
		double[] playerStats = new double[types.length];
		for (int i = 0; i < types.length; i++) {
			playerStats[i] = getStatistic(types[i], playerIndex);
		}
		return playerStats;
	}
	
	public int getIndex(Player search) {
		if (search != null) {
			int index = 0;
			for (Player p : players) {
				if (p.equals(search)) {
					return index; 
				}
				index++;
			}
		}
		return -1;
	}
	
	public int getGamesCount() {
		return gamesCount;
	}
}
