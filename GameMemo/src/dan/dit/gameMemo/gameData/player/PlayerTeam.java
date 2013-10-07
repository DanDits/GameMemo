package dan.dit.gameMemo.gameData.player;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class PlayerTeam extends AbstractPlayerTeam {
	List<Player> mPlayers = new LinkedList<Player>();
	
	@Override
	public Player getFirst() {
		return mPlayers.get(0);
	}

	@Override
	public List<Player> getPlayers() {
		return new ArrayList<Player>(mPlayers);
	}

	@Override
	public int getPlayerCount() {
		return mPlayers.size();
	}

	@Override
	public boolean contains(Player p) {
		return mPlayers.contains(p);
	}

	public void addPlayer(Player player) {
		if (player != null && !mPlayers.contains(player)) {
			mPlayers.add(player);
		}
	}

    @Override
    public String getShortenedName(int maxPlayerNameLength) {
        Iterator<Player> it = mPlayers.iterator();
        StringBuilder builder = new StringBuilder();
        while (it.hasNext()) {
            builder.append(it.next().getShortenedName(maxPlayerNameLength));
            if (it.hasNext()) {
                builder.append(" & ");
            }
        }
        return builder.toString();
    }

}
