package dan.dit.gameMemo.storage.database;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

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
 * Uri format: 
 * All games (parameter: gamekey): content://AUTHORITY/#
 * Game by id (parameter: gamekey, gameid): content://AUTHORITY/#/_id/# 
 * Game by starttime (parameter: gamekey, starttime): content://AUTHORITY/#/sTime/#
 * @author Daniel
 *
 */

public class GamesDBContentProvider extends ContentProvider {
	  // database 
	private GameSQLiteHelper database;

	// must equal the authority in the manifest file
	public static final String AUTHORITY = "dan.dit.gameMemo.gamesData.contentprovider";

	private static final int URI_TYPE_ALL = 0;
	private static final int URI_TYPE_SINGLE_ID = 1;
	private static final int URI_TYPE_SINGLE_STARTTIME = 2;
	private static final UriMatcher sURIMatcher = new UriMatcher(
			UriMatcher.NO_MATCH);

	static {
		sURIMatcher.addURI(AUTHORITY, "content://" + AUTHORITY + "/#/" + GameStorageHelper.COLUMN_ID + "/#", URI_TYPE_SINGLE_ID);
		sURIMatcher.addURI(AUTHORITY, "content://" + AUTHORITY + "/#/" + GameStorageHelper.COLUMN_STARTTIME + "/#", URI_TYPE_SINGLE_STARTTIME);
		sURIMatcher.addURI(AUTHORITY, "content://" + AUTHORITY + "/#", URI_TYPE_ALL);
	}

	@Override
	public boolean onCreate() {
		database = new GameSQLiteHelper(getContext());
		return false;
	}

	private int matchUriType(Uri uri) {
		List<String> pathSegments = uri.getPathSegments(); //TODO temp fix
		if (pathSegments.size() == 1) {
			return URI_TYPE_ALL;
		} else {
			for (String segm : pathSegments) {
				if (segm.equals(GameStorageHelper.COLUMN_ID)) {
					return URI_TYPE_SINGLE_ID;
				}
				if (segm.equals(GameStorageHelper.COLUMN_STARTTIME)) {
					return URI_TYPE_SINGLE_STARTTIME;
				}
			}
		}
		return UriMatcher.NO_MATCH;
	}
	
	@Override
	synchronized public Cursor query(Uri uri, String[] projection,
			String selection, String[] selectionArgs, String sortOrder) {
		int uriType = matchUriType(uri);
		int gameKey = Integer.parseInt(uri.getPathSegments().get(0));
		
		// Using SQLiteQueryBuilder instead of query() method
		SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
		
		// Check if the caller has requested a column which does not exists
		checkColumns(projection, gameKey);

		queryBuilder.setTables(GameStorageHelper.getTableName(gameKey));
		queryBuilder.appendWhere(GameStorageHelper.COLUMN_GAME_KEY + "=" + gameKey);
		switch (uriType) {
		case URI_TYPE_ALL:
			break;
		case URI_TYPE_SINGLE_ID:
			// Adding the ID to the original query
			queryBuilder.appendWhere(" and " + GameStorageHelper.COLUMN_ID + "="
					+ uri.getLastPathSegment());
			break;
		case URI_TYPE_SINGLE_STARTTIME:
			queryBuilder.appendWhere(" and " + GameStorageHelper.COLUMN_STARTTIME + "="
					+ uri.getLastPathSegment());
			break;			
		default:
			throw new IllegalArgumentException("Unknown URI: " + uri + " matched type = " + uriType);
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
		int uriType = matchUriType(uri);
		int gameKey = Integer.parseInt(uri.getPathSegments().get(0));
		
		SQLiteDatabase sqlDB = database.getWritableDatabase();
		long id = 0;
		values.put(GameStorageHelper.COLUMN_GAME_KEY, gameKey);
		switch (uriType) {
		case URI_TYPE_ALL:
			id = sqlDB.insert(GameStorageHelper.getTableName(gameKey), null, values);
			break;
		default:
			throw new IllegalArgumentException("Unknown or illegal URI: " + uri);
		}
		getContext().getContentResolver().notifyChange(uri, null);
		return GameStorageHelper.getUriWithId(gameKey, id);
	}

	@Override
	synchronized public int delete(Uri uri, String selection,
			String[] selectionArgs) {
		int uriType = matchUriType(uri);
		int gameKey = Integer.parseInt(uri.getPathSegments().get(0));
		
		SQLiteDatabase sqlDB = database.getWritableDatabase();
		int rowsDeleted = 0;
		switch (uriType) {
		case URI_TYPE_ALL:
			if (TextUtils.isEmpty(selection)) {
				rowsDeleted = sqlDB.delete(GameStorageHelper.getTableName(gameKey),
						GameStorageHelper.COLUMN_GAME_KEY + "=" + gameKey, null);
			} else {
				rowsDeleted = sqlDB.delete(GameStorageHelper.getTableName(gameKey),
								GameStorageHelper.COLUMN_GAME_KEY + "=" + gameKey + " and " 
								+ selection, selectionArgs);
			}
			break;
		case URI_TYPE_SINGLE_ID:
			String id = uri.getLastPathSegment();
			if (TextUtils.isEmpty(selection)) {
				rowsDeleted = sqlDB.delete(GameStorageHelper.getTableName(gameKey),
						GameStorageHelper.COLUMN_ID + "=" + id + " and " 
						+ GameStorageHelper.COLUMN_GAME_KEY + "=" + gameKey, null);
			} else {
				rowsDeleted = sqlDB.delete(GameStorageHelper.getTableName(gameKey),
						GameStorageHelper.COLUMN_ID + "=" + id + " and "
								+ GameStorageHelper.COLUMN_GAME_KEY + "=" + gameKey + " and " 
								+ selection, selectionArgs);
			}
			break;
		case URI_TYPE_SINGLE_STARTTIME:
			String startTime = uri.getLastPathSegment();
			if (TextUtils.isEmpty(selection)) {
				rowsDeleted = sqlDB.delete(GameStorageHelper.getTableName(gameKey),
						GameStorageHelper.COLUMN_STARTTIME + "=" + startTime + " and " 
						+ GameStorageHelper.COLUMN_GAME_KEY + "=" + gameKey, null);
			} else {
				rowsDeleted = sqlDB.delete(GameStorageHelper.getTableName(gameKey),
						GameStorageHelper.COLUMN_STARTTIME + "=" + startTime + " and "
								+ GameStorageHelper.COLUMN_GAME_KEY + "=" + gameKey + " and " 
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
		int uriType = matchUriType(uri);
		int gameKey = Integer.parseInt(uri.getPathSegments().get(0));
		
		SQLiteDatabase sqlDB = database.getWritableDatabase();
		int rowsUpdated = 0;
		switch (uriType) {
		case URI_TYPE_ALL:
			if (TextUtils.isEmpty(selection)) {
				rowsUpdated = sqlDB.update(GameStorageHelper.getTableName(gameKey),
						values, GameStorageHelper.COLUMN_GAME_KEY + "=" + gameKey, null);
			} else {
				rowsUpdated = sqlDB.update(GameStorageHelper.getTableName(gameKey),
						values, GameStorageHelper.COLUMN_GAME_KEY + "=" + gameKey + " and "
								+ selection, selectionArgs);
			}
			break;
		case URI_TYPE_SINGLE_ID:
			String id = uri.getLastPathSegment();
			if (TextUtils.isEmpty(selection)) {
				rowsUpdated = sqlDB.update(GameStorageHelper.getTableName(gameKey),
						values, GameStorageHelper.COLUMN_ID + "=" + id + " and "
						+ GameStorageHelper.COLUMN_GAME_KEY + "=" + gameKey, null);
			} else {
				rowsUpdated = sqlDB.update(GameStorageHelper.getTableName(gameKey),
						values, GameStorageHelper.COLUMN_ID + "=" + id + " and "
								+ GameStorageHelper.COLUMN_GAME_KEY + "=" + gameKey + " and "
								+ selection, selectionArgs);
			}
			break;
		case URI_TYPE_SINGLE_STARTTIME:
			String startTime = uri.getLastPathSegment();
			if (TextUtils.isEmpty(selection)) {
				rowsUpdated = sqlDB.update(GameStorageHelper.getTableName(gameKey),
						values, GameStorageHelper.COLUMN_STARTTIME + "=" + startTime + " and "
						+ GameStorageHelper.COLUMN_GAME_KEY + "=" + gameKey, null);
			} else {
				rowsUpdated = sqlDB.update(GameStorageHelper.getTableName(gameKey),
						values, GameStorageHelper.COLUMN_STARTTIME + "=" + startTime + " and "
								+ GameStorageHelper.COLUMN_GAME_KEY + "=" + gameKey + " and "
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
