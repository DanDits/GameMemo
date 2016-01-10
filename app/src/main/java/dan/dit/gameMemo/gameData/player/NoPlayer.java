package dan.dit.gameMemo.gameData.player;

public class NoPlayer extends Player {
    public static final NoPlayer INSTANCE = new NoPlayer();
	
    private NoPlayer() {
		super("-");
	}

}
