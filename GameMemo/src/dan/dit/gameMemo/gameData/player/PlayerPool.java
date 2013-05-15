package dan.dit.gameMemo.gameData.player;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import android.content.ContentResolver;
import android.content.Context;
import dan.dit.gameMemo.R;
/**
 * A PlayerPool holds a set of players. 
 *  A player contained in a pool can be renamed for this pool. To rename a
 * player for multiple pools use a {@link CombinedPool}.
 * @author Daniel
 *
 */
public class PlayerPool {		
	private List<Player> players = new LinkedList<Player>();
	private List<WeakReference<PlayerNameChangeListener>> mListeners = new LinkedList<WeakReference<PlayerNameChangeListener>>();
	
	public interface PlayerNameChangeListener {
		void playerNameChanged(String oldName, Player newPlayer);
	}
	
	public PlayerPool() {
		CombinedPool.ALL_POOLS.addPool(this);
	}
	
	public void registerListener(PlayerNameChangeListener listener, boolean highPriority) {
		WeakReference<PlayerNameChangeListener> ref = new WeakReference<PlayerNameChangeListener>(listener);
		if (highPriority) {
			mListeners.add(0, ref);
		} else {
			mListeners.add(ref);
		}
	}
	
	public void unregisterListener(PlayerNameChangeListener listener) {
		if (listener == null) {
			return;
		}
		Iterator<WeakReference<PlayerNameChangeListener>> it = mListeners.iterator();
		while (it.hasNext()) {
			WeakReference<PlayerNameChangeListener> ref = it.next();
			PlayerNameChangeListener l = ref.get();
			if (l == null || listener.equals(l)) {
				it.remove();
			}
		}
	}
	
	private void notifyListeners(String oldName, Player newPlayer) {
		Iterator<WeakReference<PlayerNameChangeListener>> it = mListeners.iterator();
		while (it.hasNext()) {
			WeakReference<PlayerNameChangeListener> ref = it.next();
			PlayerNameChangeListener l = ref.get();
			if (l == null) {
				it.remove();
			} else {
				l.playerNameChanged(oldName, newPlayer);
			}
		}
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
	
	public PlayerAdapter makeAdapter(Context context, boolean big) {
		int layoutId = big ? R.layout.dropdown_item_big : android.R.layout.simple_dropdown_item_1line;
		PlayerAdapter adapter = new PlayerAdapter(context, layoutId, android.R.id.text1);
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

	/**
	 * Renames the given player contained in this pool, giving him the given new name. The given
	 * player instance should be forgotten and the returned used, if not <code>null</code>.
	 * @param player The player to rename.
	 * @param newName The new valid name of the player
	 * @param mRenameInGameIds 
	 * @return <code>false</code> if renaming fails: The given name is invalid or player not
	 * contained in this pool.
	 */
	public boolean renamePlayer(int gameKey, ContentResolver resolver, Player player, String newName, long[] mRenameInGameIds) {
		if (!Player.isValidPlayerName(newName) || !contains(player)) {
			return false;
		}
		// if renaming for specific games only it can happen that the player to rename still exists in some games but is not in the pool anymore
		// but with this knowledge here we can not keep both players since renaming for specific games could also elimate the old player totally
		boolean result = false;
		if (player.rename(gameKey, resolver, newName, mRenameInGameIds) > 0) {
			result = true;
			if (contains(newName)) {
				// already got a player with this name... merge them, so remove one that we do not keep duplicates
				players.remove(player);
			}
			Player newPlayer = populatePlayer(newName);//itself or the other merged player
			newPlayer.adapterNameLetterCase(newName); // so if 'hans' gets renamed to 'bla' and there already is 'BLA', BLA will change its name to bla
			notifyListeners(player.getName(), newPlayer);
		}
		return result;
	}
	
	public int size() {
		return players.size();
	}

}
