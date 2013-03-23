package dan.dit.gameMemo.storage.database;

import dan.dit.gameMemo.storage.GameStorageHelper;
import dan.dit.gameMemo.storage.database.tichu.TichuTable;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * A class that helps to open a sqlite database and creating and upgrading
 * tables for games.
 * @author Daniel
 *
 */
public class GameSQLiteHelper extends SQLiteOpenHelper {
	public static final String GENERAL_GAME_TABLE_PREFIX = "game_";
	private static final String DATABASE_NAME = "games.db";
	private static final int DATABASE_VERSION = 2;

	public static final String DATABASE_UPGRADE_COLUMN_METADATA_CONSTRAINT = GameStorageHelper.COLUMN_METADATA + " text not null default ''";
	public static final String DATABASE_UPGRADE_COLUMN_RUNTIME_CONSTRAINT = GameStorageHelper.COLUMN_RUNTIME + " integer default 0";
	public static final String DATABASE_UPGRADE_COLUMN_ORIGIN_CONSTRAINT = GameStorageHelper.COLUMN_ORIGIN + " text not null default ''";
	public static final String DATABASE_CREATE_DEFAULT_TABLE_COLUMNS = 
			GameStorageHelper.COLUMN_ID + " integer primary key autoincrement, "
			+ GameStorageHelper.COLUMN_STARTTIME + " integer, " 
			+ GameStorageHelper.COLUMN_PLAYERS + " text not null, "
			+ GameStorageHelper.COLUMN_ROUNDS + " text not null, "
			+ GameStorageHelper.COLUMN_WINNER + " integer, "
			+ DATABASE_UPGRADE_COLUMN_METADATA_CONSTRAINT + ", "
			+ DATABASE_UPGRADE_COLUMN_RUNTIME_CONSTRAINT + ", "
			+ DATABASE_UPGRADE_COLUMN_ORIGIN_CONSTRAINT;
	
	/**
	 * Create a helper object to create, open, and/or manage a database for games.
	 * @param context Used to open or create the database.
	 */
	public GameSQLiteHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}
	
	@Override
	public void onCreate(SQLiteDatabase database) {
		TichuTable.onCreate(database);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		TichuTable.onUpgrade(db, oldVersion, newVersion);
	}

}
