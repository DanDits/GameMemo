package dan.dit.gameMemo.gameData.player;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import android.content.ContentResolver;
import android.content.Context;
import android.widget.ArrayAdapter;
/**
 * A PlayerPool holds a set of players. Each player instance is contained in none or
 * exactly one pool. A player contained in a pool can be renamed for this pool. To rename a
 * player for multiple pools use a {@link CombinedPool}.
 * @author Daniel
 *
 */
public class PlayerPool {		
	private List<Player> players = new LinkedList<Player>();
	
	public PlayerPool() {
		CombinedPool.ALL_POOLS.addPool(this);
	}
	
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
	
	public ArrayAdapter<Player> makeAdapter(Context context) {
		ArrayAdapter<Player> adapter = new ArrayAdapter<Player>(context, android.R.layout.simple_spinner_item, android.R.id.text1);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		for (Player p : getAllSortByName(true)) {
			adapter.add(p);
		}
		return adapter;
	}
	
	public ArrayAdapter<Player> makeAdapter(Context context,
			List<Player> toFilter) {
		ArrayAdapter<Player> adapter = makeAdapter(context);
		if (toFilter != null) {
			for (Player p : toFilter) {
				adapter.remove(p);
			}
		}
		return adapter;
	}

	/**
	 * Renames the given player contained in this pool, giving him the given new name. The given
	 * player instance should be forgotten and the returned used, if not <code>null</code>.
	 * @param player The player to rename.
	 * @param newName The new valid name of the player
	 * @return <code>null</code> if renaming fails: The given name is invalid or player not
	 * contained in this pool.<br>
	 * Else the given player instance if the new name was not already being used by another player
	 * or the other player instance.
	 */
	public Player renamePlayer(int gameKey, ContentResolver resolver, Player player, String newName) {
		if (!Player.isValidPlayerName(newName) || !contains(player)) {
			return null;
		}
		if (contains(newName)) {
			// already got a player with this name... merge them, so remove one that we do not keep duplicates
			players.remove(player);
		}
		player.rename(gameKey, resolver, newName); // true
		Player result = populatePlayer(newName);//itself or the other merged player
		result.adapterNameLetterCase(newName); // so if 'hans' gets renamed to 'bla' and there already is 'BLA', BLA will change its name to bla
		return result;
	}
	
	public int size() {
		return players.size();
	}

}
