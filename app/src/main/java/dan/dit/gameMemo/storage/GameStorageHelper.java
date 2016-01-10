package dan.dit.gameMemo.storage;

import java.util.Collection;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.SparseArray;
import dan.dit.gameMemo.gameData.game.Game;
import dan.dit.gameMemo.gameData.game.GameKey;
/**
 * General helper class for data storage of games. Supports storage in different type of storage devices, but
 * currently and in planned future only a local database is supported. Handles multiple tables that support a set of columns,
 * but share the columns defined by this class.
 * @author Daniel
 *
 */
/*
 * Note when adding a new table: Invoke the onCreate and onUpgrade/Downgrade from the GameSQLiteHelper class.
 */
public final class GameStorageHelper {
	public static final int URI_TYPE_ALL = 0;
	public static final int URI_TYPE_SINGLE_ID = 1;
	public static final int URI_TYPE_SINGLE_STARTTIME = 2;
	
    public static final String[] AVAILABLE_COLUMNS = { GameStorageHelper.COLUMN_PLAYERS,
        GameStorageHelper.COLUMN_ROUNDS, GameStorageHelper.COLUMN_ID, GameStorageHelper.COLUMN_STARTTIME,
        GameStorageHelper.COLUMN_WINNER, GameStorageHelper.COLUMN_METADATA, GameStorageHelper.COLUMN_RUNTIME,
        GameStorageHelper.COLUMN_ORIGIN, GameStorageHelper.COLUMN_GAME_KEY};
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
	 * Can hold general information not belonging to certain rounds about a game as required by the game.
	 */
	public static final String COLUMN_METADATA ="metaData";
	/**
	 * Holds the time the game has (approximately) been running.
	 */
	public static final String COLUMN_RUNTIME ="rTime";
	/**
	 * Holds some hints to where this game games from, the device or author.
	 */
	public static final String COLUMN_ORIGIN ="origin";

	/**
	 * Identifies the game the data belongs to.
	 */
	public static final String COLUMN_GAME_KEY= "gameKey";
	
	private GameStorageHelper() {
	}
	
	private static final SparseArray<Uri> CONTENT_URIS = new SparseArray<Uri>();
	private static final SparseArray<Uri> CONTENT_URIS_ID = new SparseArray<Uri>();
	private static final SparseArray<Uri> CONTENT_URIS_STARTTIME = new SparseArray<Uri>();
	
	public static String getTableName(int gameKey) {
		return GameKey.getStorageTableName(gameKey);
	}
	

	public static Collection<String> getAvailableColumns(int gameKey) {
		return GameKey.getStorageAvailableColumns(gameKey);
	}
	
	public static class RequestStoredGamesCountTask extends AsyncTask<Integer, Void, Integer[]> {
	    private ContentResolver mResolver;
	    private RequestStoredGamesCountTask.Callback mCallback;
	    private Integer[] mGameKey;
	    public interface Callback {
	        /**
	         * Returns the games count for all given game keys. Both arrays
	         * are of equal length which is equal to the amount of game keys given to the task's execute
	         * method in the same order.
	         * @param gameKeys The game keys in the same order as given to the task by execute().
	         * @param gamesCount The amount of games stored for each key.
	         */
	        void receiveStoredGamesCount(Integer[] gameKeys, Integer[] gamesCount);
	    }
	    public RequestStoredGamesCountTask(ContentResolver resolver, RequestStoredGamesCountTask.Callback callback) {
	        mResolver = resolver;
	        mCallback = callback;
	    }
	    @Override
	    protected Integer[] doInBackground(Integer... gameKey) {
	        mGameKey = gameKey;
	        return getStoredGamesCount(mResolver, mGameKey);
	    }
	    
	    /** The system calls this to perform work in the UI thread and delivers
	      * the result from doInBackground() */
	    @Override
	    protected void onPostExecute(Integer[] result) {
	        mCallback.receiveStoredGamesCount(mGameKey, result);
	    }

	}
	
	private static Integer[] getStoredGamesCount(ContentResolver resolver, Integer... gameKey) {
        Cursor data = null;
        Integer[] count = new Integer[gameKey.length];
        int index = 0;
        for (int key : gameKey) {
            try {
                data = resolver.query(getUriAllItems(key), new String[] {COLUMN_ID}, null, null, null);
                if (data != null) {
                    count[index] = Integer.valueOf(data.getCount());
                }
            } finally {
                if (data != null) {
                    data.close();
                }
            }
            index++;
        }
        return count;
	}
	
	public static Uri getUriAllItems(int gameKey) {
		Uri uri = CONTENT_URIS.get(gameKey);
		if (uri == null) {
			uri = GameKey.getStorageUri(gameKey, URI_TYPE_ALL);
			CONTENT_URIS.append(gameKey, uri);
		}
		return uri;
	}
	   
	public static Uri getUriWithId(int gameKey, long id) {
		Uri uri = CONTENT_URIS_ID.get(gameKey);
		if (uri == null) {
			uri = GameKey.getStorageUri(gameKey, URI_TYPE_SINGLE_ID);
			CONTENT_URIS_ID.append(gameKey, uri);
		}
		return ContentUris.withAppendedId(uri, id);
	}
	
	public static Uri getUriWithStarttime(int gameKey, long starttime) {
		Uri uri = CONTENT_URIS_STARTTIME.get(gameKey);
		if (uri == null) {
			uri = GameKey.getStorageUri(gameKey, URI_TYPE_SINGLE_STARTTIME);
			CONTENT_URIS_STARTTIME.append(gameKey, uri);
		}
		return ContentUris.withAppendedId(uri, starttime);	
	}
	
	public static long getIdOrStarttimeFromUri(Uri uri) {
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
		return ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + gameKey;
	}
	
	public static String getCursorItemType(int gameKey) {
		return ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + gameKey;
	}
}
