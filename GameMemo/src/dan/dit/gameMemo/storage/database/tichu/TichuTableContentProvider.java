package dan.dit.gameMemo.storage.database.tichu;

import java.util.Arrays;
import java.util.HashSet;

import dan.dit.gameMemo.gameData.game.tichu.TichuGame;
import dan.dit.gameMemo.storage.database.GameSQLiteHelper;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

/**
 * This content provider is used to communicate with {@link GameSQLiteHelper}'s database
 * and allows insertion, updating and querying tichu games.
 * @author Daniel
 *
 */
public class TichuTableContentProvider extends ContentProvider {
	  // database
	  private GameSQLiteHelper database;

	  // Used for the UriMacher
	  private static final int TICHU_GAMES = 10;
	  private static final int TICHU_GAME_ID = 20;

	  private static final String AUTHORITY = "dan.dit.gameMemo.tichu.contentprovider"; // must equal the authority in the manifest file

	  private static final String BASE_PATH = TichuGame.GAME_NAME;
	  public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY
	      + "/" + BASE_PATH);

	  public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE
	      + "/" + TichuGame.GAME_NAME + "s";
	  public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE
	      + "/" + TichuGame.GAME_NAME;

	  private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
	  static {
	    sURIMatcher.addURI(AUTHORITY, BASE_PATH, TICHU_GAMES);
	    sURIMatcher.addURI(AUTHORITY, BASE_PATH + "/#", TICHU_GAME_ID);
	  }

	  @Override
	  public boolean onCreate() {
	    database = new GameSQLiteHelper(getContext());
	    return false;
	  }

	  @Override
	  synchronized public Cursor query(Uri uri, String[] projection, String selection,
	      String[] selectionArgs, String sortOrder) {

	    // Using SQLiteQueryBuilder instead of query() method
	    SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();

	    // Check if the caller has requested a column which does not exists
	    checkColumns(projection);

	    // Set the table
	    queryBuilder.setTables(TichuTable.TABLE_TICHU_GAMES);

	    int uriType = sURIMatcher.match(uri);
	    switch (uriType) {
	    case TICHU_GAMES:
	      break;
	    case TICHU_GAME_ID:
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
	    int uriType = sURIMatcher.match(uri);
	    SQLiteDatabase sqlDB = database.getWritableDatabase();
	    long id = 0;
	    switch (uriType) {
	    case TICHU_GAMES:
	      id = sqlDB.insert(TichuTable.TABLE_TICHU_GAMES, null, values);
	      break;
	    default:
	      throw new IllegalArgumentException("Unknown URI: " + uri);
	    }
	    getContext().getContentResolver().notifyChange(uri, null);
	    return Uri.parse(TichuTableContentProvider.CONTENT_URI + "/" + id);
	  }

	  @Override
	  synchronized public int delete(Uri uri, String selection, String[] selectionArgs) {
	    int uriType = sURIMatcher.match(uri);
	    SQLiteDatabase sqlDB = database.getWritableDatabase();
	    int rowsDeleted = 0;
	    switch (uriType) {
	    case TICHU_GAMES:
	      rowsDeleted = sqlDB.delete(TichuTable.TABLE_TICHU_GAMES, selection,
	          selectionArgs);
	      break;
	    case TICHU_GAME_ID:
	      String id = uri.getLastPathSegment();
	      if (TextUtils.isEmpty(selection)) {
	        rowsDeleted = sqlDB.delete(TichuTable.TABLE_TICHU_GAMES,
	            GameSQLiteHelper.COLUMN_ID + "=" + id, 
	            null);
	      } else {
	        rowsDeleted = sqlDB.delete(TichuTable.TABLE_TICHU_GAMES,
	            GameSQLiteHelper.COLUMN_ID + "=" + id 
	            + " and " + selection,
	            selectionArgs);
	      }
	      break;
	    default:
	      throw new IllegalArgumentException("Unknown URI: " + uri);
	    }
	    getContext().getContentResolver().notifyChange(uri, null);
	    return rowsDeleted;
	  }

	  @Override
	  synchronized public int update(Uri uri, ContentValues values, String selection,
	      String[] selectionArgs) {

	    int uriType = sURIMatcher.match(uri);
	    SQLiteDatabase sqlDB = database.getWritableDatabase();
	    int rowsUpdated = 0;
	    switch (uriType) {
	    case TICHU_GAMES:
	      rowsUpdated = sqlDB.update(TichuTable.TABLE_TICHU_GAMES, 
	          values, 
	          selection,
	          selectionArgs);
	      break;
	    case TICHU_GAME_ID:
	      String id = uri.getLastPathSegment();
	      if (TextUtils.isEmpty(selection)) {
	        rowsUpdated = sqlDB.update(TichuTable.TABLE_TICHU_GAMES, 
	            values,
	            GameSQLiteHelper.COLUMN_ID + "=" + id, 
	            null);
	      } else {
	        rowsUpdated = sqlDB.update(TichuTable.TABLE_TICHU_GAMES, 
	            values,
	            GameSQLiteHelper.COLUMN_ID + "=" + id 
	            + " and " 
	            + selection,
	            selectionArgs);
	      }
	      break;
	    default:
	      throw new IllegalArgumentException("Unknown URI: " + uri);
	    }
	    getContext().getContentResolver().notifyChange(uri, null);
	    return rowsUpdated;
	  }

	  private void checkColumns(String[] projection) {
	    if (projection != null) {
	      HashSet<String> requestedColumns = new HashSet<String>(Arrays.asList(projection));
	      HashSet<String> availableColumns = new HashSet<String>(Arrays.asList(TichuTable.AVAILABLE_COLUMNS));
	      // Check if all columns which are requested are available
	      if (!availableColumns.containsAll(requestedColumns)) {
	        throw new IllegalArgumentException("Unknown columns in projection");
	      }
	    }
	  }
}
