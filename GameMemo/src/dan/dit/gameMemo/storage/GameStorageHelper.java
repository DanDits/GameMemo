package dan.dit.gameMemo.storage;

import java.util.Collection;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.net.Uri;
import android.util.SparseArray;
import dan.dit.gameMemo.gameData.game.Game;
import dan.dit.gameMemo.gameData.game.GameKey;
import dan.dit.gameMemo.storage.database.GamesDBContentProvider;
import dan.dit.gameMemo.storage.database.tichu.TichuTable;

public final class GameStorageHelper {
	
	private GameStorageHelper() {
	}
	
	private static final SparseArray<Uri> CONTENT_URIS = new SparseArray<Uri>();
	static {
		CONTENT_URIS.put(GameKey.TICHU, Uri.parse("content://" + GamesDBContentProvider.AUTHORITY
			+ "/" + TichuTable.TABLE_TICHU_GAMES));
		GamesDBContentProvider.registerGame(GameKey.TICHU, TichuTable.TABLE_TICHU_GAMES);
	}
	
	public static String getTableName(int gameKey) {
		switch(gameKey) {
		case GameKey.TICHU:
			return TichuTable.TABLE_TICHU_GAMES;
		default:
			throw new IllegalArgumentException("Game not supported: " + gameKey);
		}
	}
	
	public static Uri getUriAllItems(int gameKey) {
		Uri uri = CONTENT_URIS.get(gameKey);
		if (uri == null) {
			throw new IllegalArgumentException("Game not supported: " + gameKey);
		}
		return uri;
	}
	
	public static Uri getUri(int gameKey, long id) {
		Uri uri = CONTENT_URIS.get(gameKey);
		if (uri == null) {
			throw new IllegalArgumentException("Game not supported: " + gameKey);
		}
		return ContentUris.withAppendedId(uri, id);
	}
	
	public static long getIdFromUri(Uri uri) {
		// probably the only forgiving method of this class that does not kill stupid requests :D
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
			return ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + TichuTable.TABLE_TICHU_GAMES;
		default:
			throw new IllegalArgumentException("Game not supported: " + gameKey);
		}
	}
	
	public static String getCursorItemType(int gameKey) {
		switch(gameKey) {
		case GameKey.TICHU:
			return ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" +  TichuTable.TABLE_TICHU_GAMES;
		default:
			throw new IllegalArgumentException("Game not supported: " + gameKey);
		}
	}

	public static Collection<String> getAvailableColumns(int gameKey) {
		switch(gameKey) {
		case GameKey.TICHU:
			return TichuTable.AVAILABLE_COLUMNS_COLL;
		default:
			throw new IllegalArgumentException("Game not supported: " + gameKey);
		}
	}
}
