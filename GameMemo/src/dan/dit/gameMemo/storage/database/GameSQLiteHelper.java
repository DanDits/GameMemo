package dan.dit.gameMemo.storage.database;

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
	
	private static final String DATABASE_NAME = "games.db";
	private static final int DATABASE_VERSION = 2;

	public static final String DATABASE_UPGRADE_COLUMN_METADATA_CONSTRAINT = COLUMN_METADATA + " text not null default ''";
	public static final String DATABASE_UPGRADE_COLUMN_RUNTIME_CONSTRAINT = COLUMN_RUNTIME + " integer default 0";
	public static final String DATABASE_UPGRADE_COLUMN_ORIGIN_CONSTRAINT = COLUMN_ORIGIN + " text not null default ''";
	public static final String DATABASE_CREATE_DEFAULT_TABLE_COLUMNS = 
			COLUMN_ID + " integer primary key autoincrement, "
			+ COLUMN_STARTTIME + " integer, " 
			+ COLUMN_PLAYERS + " text not null, "
			+ COLUMN_ROUNDS + " text not null, "
			+ COLUMN_WINNER + " integer, "
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
