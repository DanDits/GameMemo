package dan.dit.gameMemo.gameData.player;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

public abstract class AbstractPlayerTeam implements Iterable<Player> {
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
}
