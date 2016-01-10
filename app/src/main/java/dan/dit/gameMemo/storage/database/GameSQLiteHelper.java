package dan.dit.gameMemo.storage.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import dan.dit.gameMemo.storage.GameStorageHelper;

/**
 * A class that helps to open a sqlite database and creating and upgrading
 * tables for games.
 * @author Daniel
 *
 */
public class GameSQLiteHelper extends SQLiteOpenHelper {
	public static final String GENERAL_GAME_TABLE_PREFIX = "game_";
	private static final String DATABASE_NAME = "gamesData";
	/*
	 * Change log: 
	 * 1 Default version
	 * 2 Added SportGameTable
	 */
	private static final int DATABASE_VERSION = 2;

	public static final String DATABASE_CREATE_DEFAULT_TABLE_COLUMNS = 
			GameStorageHelper.COLUMN_ID + " integer primary key autoincrement, "
			+ GameStorageHelper.COLUMN_STARTTIME + " integer, " 
			+ GameStorageHelper.COLUMN_PLAYERS + " text not null, "
			+ GameStorageHelper.COLUMN_ROUNDS + " text not null, "
			+ GameStorageHelper.COLUMN_WINNER + " integer, "
			+ GameStorageHelper.COLUMN_METADATA + " text not null, "
			+ GameStorageHelper.COLUMN_RUNTIME + " integer, "
			+ GameStorageHelper.COLUMN_ORIGIN + " text not null, "
			+ GameStorageHelper.COLUMN_GAME_KEY + " integer";
	
	/**
	 * Create a helper object to create, open, and/or manage a database for games.
	 * @param context Used to open or create the database.
	 */
	public GameSQLiteHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}
	
	@Override
	public void onCreate(SQLiteDatabase database) {
		CardGameTable.onCreate(database);
		SportGameTable.onCreate(database);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		CardGameTable.onUpgrade(db, oldVersion, newVersion);
		if (newVersion == 2) {
	        SportGameTable.onCreate(db);
		} else {
		    SportGameTable.onUpgrade(db, oldVersion, newVersion);
		}
	}
	
	@Override
	public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        super.onDowngrade(db, oldVersion, newVersion);
	    // Any table that requires onDowngrade must be invoked here
	}

}
