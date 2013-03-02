package dan.dit.gameMemo.gameData.player;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import dan.dit.gameMemo.util.compression.Compressible;
import dan.dit.gameMemo.util.compression.Compressor;

public abstract class PlayerTeam implements Compressible, Iterable<Player> {

	public abstract Player getFirst();

	public abstract List<Player> getPlayers();
	public abstract int getPlayerCount();
	public abstract boolean contains(Player p);
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
		if (other instanceof PlayerTeam) {
			return new HashSet<Player>(getPlayers()).equals(new HashSet<Player>(((PlayerTeam) other).getPlayers()));
		} else {
			return super.equals(other);
		}
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append('(');
		for (Player p : getPlayers()) {
			builder.append(p.toString());
			builder.append(", ");
		}
		builder.deleteCharAt(builder.length() - 1);
		builder.deleteCharAt(builder.length() - 1);
		builder.append(')');
		return builder.toString();
	}
	
	@Override
	public String compress() {
		Compressor cmp = new Compressor(getPlayerCount());
		for (Player p : getPlayers()) {
			cmp.appendData(p.getName());
		}
		return cmp.compress();
	}
}
