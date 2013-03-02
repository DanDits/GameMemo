package dan.dit.gameMemo.storage;

import android.content.ContentUris;
import android.net.Uri;
import dan.dit.gameMemo.gameData.game.Game;
import dan.dit.gameMemo.gameData.game.GameKey;
import dan.dit.gameMemo.storage.database.GamesDBContentProvider;

public final class GameStorageHelper {
	
	private GameStorageHelper() {
	}
	
	public static Uri getUriAllItems(int gameKey) {
		switch(gameKey) {
		case GameKey.TICHU:
			return GamesDBContentProvider.CONTENT_URI;
		default:
			throw new IllegalArgumentException("Game not supported: " + gameKey);
		}
	}
	
	public static Uri getUri(int gameKey, long id) {
		Uri contentUri;
		switch(gameKey) {
		case GameKey.TICHU:
			contentUri = GamesDBContentProvider.CONTENT_URI;
			break;
		default:
			throw new IllegalArgumentException("Game not supported: " + gameKey);
		}
		return ContentUris.withAppendedId(contentUri, id);
	}
	
	public static long getIdFromUri(Uri uri) {
		if (uri == null) {
			return Game.NO_ID;
		}
		if (uri.isHierarchical()) {
			try {
				return ContentUris.parseId(uri);
			} catch (NumberFormatException nfe) {
				return Game.NO_ID;
			}
		}
		return Game.NO_ID;
	}
	
	public static String getCursorDirType(int gameKey) {
		switch(gameKey) {
		case GameKey.TICHU:
			return GamesDBContentProvider.CONTENT_TYPE;
		default:
			throw new IllegalArgumentException("Game not supported: " + gameKey);
		}
	}
	
	public static String getCursorItemType(int gameKey) {
		switch(gameKey) {
		case GameKey.TICHU:
			return GamesDBContentProvider.CONTENT_ITEM_TYPE;
		default:
			throw new IllegalArgumentException("Game not supported: " + gameKey);
		}
	}
}
