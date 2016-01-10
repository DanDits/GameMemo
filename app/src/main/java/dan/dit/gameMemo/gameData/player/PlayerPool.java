package dan.dit.gameMemo.gameData.player;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import android.content.Context;
import dan.dit.gameMemo.gameData.game.GameKey;
/**
 * A PlayerPool holds a set of players. 
 *  A player contained in a pool can be renamed for this pool.
 * @author Daniel
 *
 */
public class PlayerPool {		
	private Set<Player> players = new TreeSet<Player>();
	
	private boolean addPlayer(Player p) {
		if (!contains(p)) {
			players.add(p);
			return true;
		}
		return false;
	}
	
	public boolean contains(String name) {
		if (!Player.isValidPlayerName(name)) {
			return false;
		}
		String playerName = name.trim();
		for (Player p : players) {
			if (p.getName().equalsIgnoreCase(playerName)) {
				return true;
			}
		}
		return false;
	}
	
	public boolean contains(Player p) {
		if (p != null) {
			return players.contains(p);
		}
		return false;
	}
	
	public Player populatePlayer(String name) {
		if (!Player.isValidPlayerName(name)) {
			throw new IllegalArgumentException("Illegal player name " + name);
		}
		String trimmedName = name.trim();
		Player player = null;
		for (Player p : players) {
			if (p.getName().equalsIgnoreCase(trimmedName)) {
				player = p;
			}
		}
		if (player == null) {
			player = new Player(trimmedName);
			addPlayer(player);
		}
		return player;
	}

	public Collection<Player> getAll() {
		return new TreeSet<Player>(players);
	}
	
	public PlayerAdapter makeAdapter(Context context, boolean big) {
		PlayerAdapter adapter = new PlayerAdapter(big, context, GameKey.getAllPlayers());
		for (Player p : getAll()) {
			adapter.add(p);
		}
		return adapter;
	}
	
	public PlayerAdapter makeAdapter(Context context,
			List<Player> toFilter, boolean big) {
		PlayerAdapter adapter = (PlayerAdapter) makeAdapter(context, big);
		adapter.addFilterPlayers(toFilter);
		return adapter;
	}
	
	public int size() {
		return players.size();
	}

	public boolean removePlayer(String name) {
		Iterator<Player> it = players.iterator();
		while (it.hasNext()) {
			Player p = it.next();
			if (p.getName().equalsIgnoreCase(name)) {
				it.remove();
				return true;
			}
		}
		return false;
	}

}
