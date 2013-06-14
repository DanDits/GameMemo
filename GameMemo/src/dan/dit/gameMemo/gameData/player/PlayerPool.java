package dan.dit.gameMemo.gameData.player;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import android.content.Context;
import dan.dit.gameMemo.R;
import dan.dit.gameMemo.gameData.game.GameKey;
/**
 * A PlayerPool holds a set of players. 
 *  A player contained in a pool can be renamed for this pool.
 * @author Daniel
 *
 */
public class PlayerPool {		
	private List<Player> players = new LinkedList<Player>();
	
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
		return new LinkedList<Player>(players);
	}
	
	public List<Player> getAll(Comparator<? super Player> comp) {
		List<Player> res = new LinkedList<Player>(players);
		Collections.sort(res, comp);
		return res;
	}	
	
	public List<Player> getAllSortByName(boolean ascending) {
		List<Player> res = new LinkedList<Player>(players);
		Collections.sort(res, Player.NAME_COMPARATOR);
		
		if (!ascending) {
			Collections.reverse(res);
		}
		return res;
	}
	
	public PlayerAdapter makeAdapter(Context context, boolean big) {
		int layoutId = big ? R.layout.dropdown_item_big : android.R.layout.simple_dropdown_item_1line;
		PlayerAdapter adapter = new PlayerAdapter(context, layoutId, android.R.id.text1, GameKey.getAllPlayers());
		for (Player p : getAllSortByName(true)) {
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
