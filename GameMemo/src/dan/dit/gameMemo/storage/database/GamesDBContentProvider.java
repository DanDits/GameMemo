package dan.dit.gameMemo.storage.database;

import java.util.Arrays;
import java.util.HashSet;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import dan.dit.gameMemo.storage.GameStorageHelper;

/**
 * This content provider is used to communicate with {@link GameSQLiteHelper}'s database
 * and allows insertion, updating and querying games.
 *
 * @author Daniel
 *
 */

public class GamesDBContentProvider extends ContentProvider {
	  // database 
	private GameSQLiteHelper database;

	// must equal the authority in the manifest file
	public static final String AUTHORITY = "dan.dit.gameMemo.games_db.contentprovider";

	private static final int URI_TYPE_ALL = 0;
	private static final int URI_TYPE_SINGLE_ID = 1;
	private static final int URI_COUNT_PER_GAME = 2;
	private static final UriMatcher sURIMatcher = new UriMatcher(
			UriMatcher.NO_MATCH);

	public static void registerGame(int gameKey, String path) {
		sURIMatcher.addURI(AUTHORITY, path, gameKey * URI_COUNT_PER_GAME + URI_TYPE_ALL);
		sURIMatcher.addURI(AUTHORITY, path + "/#", gameKey
				* URI_COUNT_PER_GAME + URI_TYPE_SINGLE_ID);
	}

	@Override
	public boolean onCreate() {
		database = new GameSQLiteHelper(getContext());
		return false;
	}

	@Override
	synchronized public Cursor query(Uri uri, String[] projection,
			String selection, String[] selectionArgs, String sortOrder) {
		int uriKey = sURIMatcher.match(uri);
		int uriType = uriKey % URI_COUNT_PER_GAME;
		int gameKey = uriKey / URI_COUNT_PER_GAME;
		
		// Using SQLiteQueryBuilder instead of query() method
		SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();

		// Check if the caller has requested a column which does not exists
		checkColumns(projection, gameKey);

		queryBuilder.setTables(GameStorageHelper.getTableName(gameKey));
		switch (uriType) {
		case URI_TYPE_ALL:
			break;
		case URI_TYPE_SINGLE_ID:
			// Adding the ID to the original query
			queryBuilder.appendWhere(GameSQLiteHelper.COLUMN_ID + "="
					+ uri.getLastPathSegment());
			break;
		default:
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}

		SQLiteDatabase db = database.getWritableDatabase();
		Cursor cursor = queryBuilder.query(db, projection, selection,
				selectionArgs, null, null, sortOrder);
		// Make sure that potential listeners are getting notified
		cursor.setNotificationUri(getContext().getContentResolver(), uri);

		return cursor;
	}

	@Override
	public String getType(Uri uri) {
		return null;
	}

	@Override
	synchronized public Uri insert(Uri uri, ContentValues values) {
		int uriKey = sURIMatcher.match(uri);
		int uriType = uriKey % URI_COUNT_PER_GAME;
		int gameKey = uriKey / URI_COUNT_PER_GAME;
		SQLiteDatabase sqlDB = database.getWritableDatabase();
		long id = 0;
		switch (uriType) {
		case URI_TYPE_ALL:
			id = sqlDB.insert(GameStorageHelper.getTableName(gameKey), null, values);
			break;
		default:
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}
		getContext().getContentResolver().notifyChange(uri, null);
		return GameStorageHelper.getUri(gameKey, id);
	}

	@Override
	synchronized public int delete(Uri uri, String selection,
			String[] selectionArgs) {
		int uriKey = sURIMatcher.match(uri);
		int uriType = uriKey % URI_COUNT_PER_GAME;
		int gameKey = uriKey / URI_COUNT_PER_GAME;
		SQLiteDatabase sqlDB = database.getWritableDatabase();
		int rowsDeleted = 0;
		switch (uriType) {
		case URI_TYPE_ALL:
			rowsDeleted = sqlDB.delete(GameStorageHelper.getTableName(gameKey), selection,
					selectionArgs);
			break;
		case URI_TYPE_SINGLE_ID:
			String id = uri.getLastPathSegment();
			if (TextUtils.isEmpty(selection)) {
				rowsDeleted = sqlDB.delete(GameStorageHelper.getTableName(gameKey),
						GameSQLiteHelper.COLUMN_ID + "=" + id, null);
			} else {
				rowsDeleted = sqlDB.delete(GameStorageHelper.getTableName(gameKey),
						GameSQLiteHelper.COLUMN_ID + "=" + id + " and "
								+ selection, selectionArgs);
			}
			break;
		default:
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}
		getContext().getContentResolver().notifyChange(uri, null);
		return rowsDeleted;
	}

	@Override
	synchronized public int update(Uri uri, ContentValues values,
			String selection, String[] selectionArgs) {

		int uriKey = sURIMatcher.match(uri);
		int uriType = uriKey % URI_COUNT_PER_GAME;
		int gameKey = uriKey / URI_COUNT_PER_GAME;
		SQLiteDatabase sqlDB = database.getWritableDatabase();
		int rowsUpdated = 0;
		switch (uriType) {
		case URI_TYPE_ALL:
			rowsUpdated = sqlDB.update(GameStorageHelper.getTableName(gameKey), values,
					selection, selectionArgs);
			break;
		case URI_TYPE_SINGLE_ID:
			String id = uri.getLastPathSegment();
			if (TextUtils.isEmpty(selection)) {
				rowsUpdated = sqlDB.update(GameStorageHelper.getTableName(gameKey),
						values, GameSQLiteHelper.COLUMN_ID + "=" + id, null);
			} else {
				rowsUpdated = sqlDB.update(GameStorageHelper.getTableName(gameKey),
						values, GameSQLiteHelper.COLUMN_ID + "=" + id + " and "
								+ selection, selectionArgs);
			}
			break;
		default:
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}
		getContext().getContentResolver().notifyChange(uri, null);
		return rowsUpdated;
	}

	private void checkColumns(String[] projection, int gameKey) {
		if (projection != null) {
			HashSet<String> requestedColumns = new HashSet<String>(
					Arrays.asList(projection));
			// Check if all columns which are requested are available
			if (!GameStorageHelper.getAvailableColumns(gameKey).containsAll(requestedColumns)) {
				throw new IllegalArgumentException(
						"Unknown columns in projection");
			}
		}
	}
}
