package dan.dit.gameMemo.gameData.player;

import java.util.ArrayList;
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

}
