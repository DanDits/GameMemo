package dan.dit.gameMemo.storage;

import java.util.Collection;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.util.SparseArray;
import dan.dit.gameMemo.gameData.game.Game;
import dan.dit.gameMemo.gameData.game.GameKey;
import dan.dit.gameMemo.storage.database.GamesDBContentProvider;
import dan.dit.gameMemo.storage.database.TichuTable;

public final class GameStorageHelper {
	
	public static final String COLUMN_ID = "_id";
	public static final String COLUMN_STARTTIME = "sTime";
	public static final String COLUMN_PLAYERS = "players";
	public static final String COLUMN_ROUNDS = "rounds";
	public static final String COLUMN_WINNER ="winner";
	/*
	 * Developers note for when adding new columns:
	 * State in which version the column was added and supply a default value for the column (mostly not null).
	 * Make a constraint for this column.
	 * Make the default creation string create this column.
	 * Ensure that all tables that this helper invokes onUpgrade/onDowngrade on handle this version
	 * change and alter the table accordingly.
	 */
	/**
	 * Added with version 2. Can hold general information not belonging to certain rounds about a game as required by the game.
	 */
	public static final String COLUMN_METADATA ="metaData";
	/**
	 * Added with version 2. Holds the time the game has (approximately) been running.
	 */
	public static final String COLUMN_RUNTIME ="rTime";
	/**
	 * Added with version 2. Holds some hints to where this game games from, the device or author.
	 */
	public static final String COLUMN_ORIGIN ="origin";

	private GameStorageHelper() {
	}
	
	private static final SparseArray<Uri> CONTENT_URIS = new SparseArray<Uri>();
	static {
		CONTENT_URIS.put(GameKey.TICHU, Uri.parse("content://" + GamesDBContentProvider.AUTHORITY
			+ "/" + TichuTable.TABLE_TICHU_GAMES));
		GamesDBContentProvider.registerGame(GameKey.TICHU, TichuTable.TABLE_TICHU_GAMES);
	}
	
	public static int getStoredGamesCount(ContentResolver resolver, int gameKey) {
		Cursor data = resolver.query(getUriAllItems(gameKey), new String[] {COLUMN_ID}, null, null, null);
		return data == null ? 0 : data.getCount();
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
