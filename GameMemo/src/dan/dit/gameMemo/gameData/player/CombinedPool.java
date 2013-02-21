package dan.dit.gameMemo.gameData.player;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import dan.dit.gameMemo.gameData.game.GameKey;

import android.content.ContentResolver;
import android.content.Context;
import android.widget.ArrayAdapter;

public class CombinedPool {
	public static final CombinedPool ALL_POOLS = new CombinedPool(); 
	private List<PlayerPool> mPools = new LinkedList<PlayerPool>();
	
	
	public boolean addPool(PlayerPool pool) {
		if (!containsPool(pool)) {
			mPools.add(pool);
			return true;
		}
		return false;
	}
	
	public boolean containsPool(PlayerPool pool) {
		return mPools.contains(pool);
	}
	
	public boolean removePool(PlayerPool pool) {
		return mPools.remove(pool);
	}
	
	public int contains(Player p) {
		int count = 0;
		for (PlayerPool pool : mPools) {
			if (pool.contains(p)) {
				count++;
			}
		}
		return count;
	}
	
	public Collection<Player> getAll() {
		LinkedList<Player> players = new LinkedList<Player>();
		for (PlayerPool pool : mPools) {
			players.addAll(pool.getAll());
		}
		return players;
	}
	
	public List<Player> getAll(Comparator<? super Player> comp) {
		List<Player> res = new LinkedList<Player>(getAll());
		Collections.sort(res, comp);
		return res;
	}	
	
	public List<Player> getAllSortByName(boolean ascending) {
		List<Player> res = new LinkedList<Player>(getAll());
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

	public int renamePlayer(int[] gameKeys, ContentResolver resolver, Player player, String newName) {
		if (!Player.isValidPlayerName(newName)) {
			return 0;
		}
		int count = 0;
		for (int key : gameKeys) {
			if (GameKey.isGameSupported(key)) {
				PlayerPool pool = GameKey.getPool(key);
				if (mPools.contains(pool)) {
					if (pool.renamePlayer(key, resolver, player, newName) != null) {
						count++;
					}
				}
			}
		}
		return count;
	}
	
	public int getPoolCount() {
		return mPools.size();
	}
	
}