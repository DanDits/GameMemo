package dan.dit.gameMemo.gameData.game;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentResolver;
import android.net.Uri;
import dan.dit.gameMemo.gameData.game.tichu.TichuGame;
import dan.dit.gameMemo.gameData.game.tichu.TichuGameBuilder;
import dan.dit.gameMemo.gameData.player.Player;
import dan.dit.gameMemo.gameData.player.PlayerPool;
import dan.dit.gameMemo.gameData.statistics.GameStatisticBuilder;
import dan.dit.gameMemo.gameData.statistics.tichu.TichuGameStatisticBuilder;
import dan.dit.gameMemo.storage.GameStorageHelper;
import dan.dit.gameMemo.util.compression.CompressedDataCorruptException;

/**
 * This helper class stores all game keys for classes that need to separate
 * subclasses of {@link Game} to distinguish between different static methods or constants.<br>
 * Just because a constant is listed here does not necessarily mean that the class supports this game.
 * @author Daniel
 *
 */
public final class GameKey {
	public static final String EXTRA_GAMEKEY = "dan.dit.gameMemo.EXTRA_GAMEKEY";
	/**
	 * The constant for the {@link TichuGame} class.
	 */
	public static final int TICHU = 1;
	public static final int[] ALL_GAMES = new int[] {TICHU};
	
	private GameKey() {}
	
	public static GameStatisticBuilder getStatisticBuilder(int gameKey, List<Player> players) {
		switch(gameKey) {
		case GameKey.TICHU:
			return new TichuGameStatisticBuilder(players);
		default:
			throw new IllegalArgumentException("Game not supported: " + gameKey);	
		}
	}
	
	public static int getGameIconId(int gameKey) {
		switch(gameKey) {
		case GameKey.TICHU:
			return dan.dit.gameMemo.R.drawable.tichu_phoenix;
		default:
			throw new IllegalArgumentException("Game not supported: " + gameKey);	
		}
	}
	
	public static CharSequence getGameName(int gameKey) {
		switch(gameKey) {
		case GameKey.TICHU:
			return TichuGame.GAME_NAME;
		default:
			throw new IllegalArgumentException("Game not supported: " + gameKey);	
		}
	}
	
	public static PlayerPool getPool(int gameKey) {
		switch(gameKey) {
		case GameKey.TICHU:
			return TichuGame.PLAYERS;
		default:
			throw new IllegalArgumentException("Game not supported: " + gameKey);	
		}
	}
	
	public static GameBuilder getBuilder(int gameKey) {
		switch(gameKey) {
		case GameKey.TICHU:
			return new TichuGameBuilder();
		default:
			throw new IllegalArgumentException("Game not supported: " + gameKey);			
		}
	}
	
	public static List<Game> loadGames(int gameKey, ContentResolver resolver, List<Long> timestamps) {
		List<Game> gamesToSend = new ArrayList<Game>(timestamps.size());
		switch (gameKey) {
		case GameKey.TICHU:
			try {
				gamesToSend = TichuGame.loadGames(resolver, GameStorageHelper.getUriAllItems(gameKey), timestamps, false);
			} catch (CompressedDataCorruptException e) {
				assert false; // load games will not throw as requested by the parameter
			}
			break;
		default:
			throw new IllegalArgumentException("Game not supported: " + gameKey);				
		}
		return gamesToSend;
	}
	
	public static List<Game> loadGames(int gameKey, ContentResolver resolver, Uri uri) throws CompressedDataCorruptException {
		switch (gameKey) {
		case GameKey.TICHU:
			return TichuGame.loadGames(resolver, uri, true);
		default:
			throw new IllegalArgumentException("Game not supported: " + gameKey);				
		}
	}
	
	public static final boolean isGameSupported(int gameKey) {
		return gameKey == GameKey.TICHU; // add all supported game keys for the static methods that take a gamekey argument and do not throw on usage
	}
}
