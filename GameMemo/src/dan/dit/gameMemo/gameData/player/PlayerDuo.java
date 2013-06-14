package dan.dit.gameMemo.gameData.player;

import java.util.LinkedList;
import java.util.List;

public class PlayerDuo extends AbstractPlayerTeam {
	private Player first;
	private Player second;
	
	public PlayerDuo(Player first, Player second) {
		this.first = first;
		this.second = second;
		if (first == null || second == null) {
			throw new NullPointerException();
		}
	}
	
	public PlayerDuo(String first, String second, PlayerPool pool) {
		this(pool.populatePlayer(first), pool.populatePlayer(second));
	}

	@Override
	public Player getFirst() {
		return first;
	}
	
	public Player getSecond() {
		return second;
	}

	@Override
	public List<Player> getPlayers() {
		LinkedList<Player> all = new LinkedList<Player>();
		all.add(first);
		all.add(second);
		return all;
	}

	@Override
	public int hashCode() {
		return first.hashCode() + second.hashCode();
	}
	
	@Override
	public boolean equals(Object other) {
		if (other instanceof PlayerDuo) {
			PlayerDuo o = (PlayerDuo) other;
			return (first.equals(o.first) || first.equals(o.second)) && (second.equals(o.first) || second.equals(o.second));
		} else {
			return super.equals(other);
		}
	}
	
	@Override
	public String toString() {
		return "(" + first + ", " + second + ")";
	}

	@Override
	public int getPlayerCount() {
		return 2;
	}

	@Override
	public boolean contains(Player p) {
		return first.equals(p) || second.equals(p);
	}
	
	public boolean contains(String playerName) {
		return first.getName().equalsIgnoreCase(playerName) || second.getName().equalsIgnoreCase(playerName);
	}
}
