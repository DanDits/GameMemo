package dan.dit.gameMemo.gameData.player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import android.content.Context;
import android.content.SharedPreferences;

public abstract class AbstractPlayerTeam implements Iterable<Player> {
    public static final String PREFERENCES_TEAM_COLOR = "dan.dit.gameMemo.PREFERENCES_TEAM_COLOR_CACHE";
    private static final int COLOR_CACHE_SIZE = 100;
	protected int mColor = PlayerColors.DEFAULT_COLOR;
	
	public abstract Player getFirst();
	public abstract List<Player> getPlayers();
	public abstract int getPlayerCount();
	public abstract boolean contains(Player p);
	
	public void setColor(int color) {
		mColor = color;
	}
	
	public int getColor() {
		return mColor;
	}
	
	@Override
	public Iterator<Player> iterator() {
		return getPlayers().iterator();
	}
	
	@Override
	public int hashCode() {
		int sum = 0;
		for (Player p : getPlayers()) {
			sum += p.hashCode();
		}
		return sum;
	}
	
	@Override
	public boolean equals(Object other) {
		if (other instanceof AbstractPlayerTeam) {
			return new HashSet<Player>(getPlayers()).equals(new HashSet<Player>(((AbstractPlayerTeam) other).getPlayers()));
		} else {
			return super.equals(other);
		}
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append('(');
		List<Player> players = getPlayers();
		Collections.sort(players, Player.NAME_COMPARATOR);
		for (Player p : players) {
			builder.append(p.toString());
			builder.append(", ");
		}
		if (builder.length() > 1) {
		    builder.deleteCharAt(builder.length() - 1);
		    builder.deleteCharAt(builder.length() - 1);
		}
		builder.append(')');
		return builder.toString();
	}
	
    public boolean containsTeam(AbstractPlayerTeam toCheck) {
        if (toCheck != null) {
            for (Player p : toCheck) {
                if (!contains(p)) {
                    return false;
                }
            }
        }
        return true;
    }
    public abstract String getShortenedName(int maxPlayerNameLength);
    
    public boolean loadCachedColor(Context context) {
        SharedPreferences pref = context.getSharedPreferences(PREFERENCES_TEAM_COLOR, Context.MODE_PRIVATE);
        String key = toString();
        if (pref.contains(key)) {
            int color = pref.getInt(key, mColor);
            setColor(color);
            return true;
        }
        return false;
    }
    
    public void saveColor(Context context) {
        SharedPreferences pref = context.getSharedPreferences(PREFERENCES_TEAM_COLOR, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        String key = toString();
        if (mColor == PlayerColors.DEFAULT_COLOR) {
            editor.remove(key);
            editor.apply();
            return; // default color is never saved
        }
        
        Map<String, ?> keys = pref.getAll();
        if (!keys.containsKey(key) && keys.size() >= COLOR_CACHE_SIZE) {
            // trying to add a new key, delete a random key to keep the cache size limited
            List<String> keyList = new ArrayList<String>(keys.keySet());
            editor.remove(keyList.get(new Random().nextInt(keyList.size())));
        }
        editor.putInt(key, mColor);
        editor.apply();
    }
}
