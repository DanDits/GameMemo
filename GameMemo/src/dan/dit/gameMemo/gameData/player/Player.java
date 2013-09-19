package dan.dit.gameMemo.gameData.player;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class Player extends AbstractPlayerTeam implements Comparable<Player> {	

	public static final Comparator<? super Player> NAME_COMPARATOR = new Comparator<Player>() {

		@Override
		public int compare(Player p1, Player p2) {
			return p1.name.toLowerCase(Locale.US).compareTo(p2.name.toLowerCase(Locale.US));
		}
		
	};
	private final String name;
	private List<Player> singletonSelfList;
	
	public Player(String name) {
		if (!isValidPlayerName(name)) {
			throw new IllegalArgumentException("Illegal player name: " + name);
		}
		this.name = name.trim();
	}
	
	public String getName() {
		return name;
	}

    public static final int SHORT_NAME_LENGTH = 7;
    public static final int VERY_SHORT_NAME_LENGTH = 3;
    /**
     * Returns a String based on the player name in a shorter version.
     * @param maxLength The maximum length of the returned string.
     * @return A short version of the player name.
     */
	public String getShortenedName(int maxLength) {
	    if (name.length() <= maxLength) {
	        return name;
	    } else {
	        String[] parts = name.split("\\s");
	        if (parts[0].length() <= maxLength) {
	            return parts[0];
	        } else {
	            return parts[0].substring(0, maxLength);
	        }
	    }
	}
	
	public String getFirstName() {
	    return name.split("\\s")[0];
	}
	
	@Override
	public int hashCode() {
		return name.toLowerCase(Locale.US).hashCode();
	}
	
	@Override
	public boolean equals(Object other) {
		if (other instanceof Player) {
			return name.equalsIgnoreCase(((Player) other).name);
		} else {
			return super.equals(other);
		}
	}
	
	public static boolean isValidPlayerName(String name) {
		return name != null && name.length() > 0 && name.trim().length() > 0;
	}
	
	@Override
	public String toString() {
		return name;
	}

	@Override
	public Player getFirst() {
		return this;
	}

	@Override
	public List<Player> getPlayers() {
		if (singletonSelfList == null) {
			// getPlayers() makes no guarantee to always return the same list, so no need to synchronize here
			singletonSelfList = Collections.singletonList(this);
		}
		return singletonSelfList;
	}

	@Override
	public int getPlayerCount() {
		return 1;
	}

	@Override
	public boolean contains(Player p) {
		return equals(p);
	}
	
	@Override
	public int compareTo(Player other) {
		return NAME_COMPARATOR.compare(this, other);
	}
}
