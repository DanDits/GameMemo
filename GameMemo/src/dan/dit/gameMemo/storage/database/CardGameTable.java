package dan.dit.gameMemo.storage.database;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import android.database.sqlite.SQLiteDatabase;
import dan.dit.gameMemo.storage.GameStorageHelper;

public class CardGameTable {
	
	 /**
	   * The card games table name
	   */
	  public static final String TABLE_CARD_GAMES = GameSQLiteHelper.GENERAL_GAME_TABLE_PREFIX + "CardGames";
	  
	  
	  /**
	   * An array of all available columns, querying the content provider with a column not listed here will result in an exception
	   * so make sure it contains all general game columns plus the tichu specific columns.
	   */
	  public static final String[] AVAILABLE_COLUMNS = { GameStorageHelper.COLUMN_PLAYERS,
		        GameStorageHelper.COLUMN_ROUNDS, GameStorageHelper.COLUMN_ID, GameStorageHelper.COLUMN_STARTTIME,
		        GameStorageHelper.COLUMN_WINNER, GameStorageHelper.COLUMN_METADATA, GameStorageHelper.COLUMN_RUNTIME,
		        GameStorageHelper.COLUMN_ORIGIN, GameStorageHelper.COLUMN_GAME_KEY};
	  public static final Collection<String> AVAILABLE_COLUMNS_COLL = new HashSet<String>(
				Arrays.asList(AVAILABLE_COLUMNS));
	  
	  // Database creation SQL statement
	  private static final String DATABASE_CREATE = "create table " 
	      + TABLE_CARD_GAMES
	      + "(" 
	      + GameSQLiteHelper.DATABASE_CREATE_DEFAULT_TABLE_COLUMNS
	      + ");";

	  private CardGameTable() {} // make sure it is never instantiated
	  
	  /**
	   * When the database is created, create the table for tichu games.
	   * @param database The newly created database.
	   */
	  public static void onCreate(SQLiteDatabase database) {
	    database.execSQL(DATABASE_CREATE);
	  }

	  /**
	   * Upgrade the table in the database.
	   * @param database The upgrading database.
	   * @param oldVersion The old version of the database.
	   * @param newVersion The new version of the database.
	   */
	  public static void onUpgrade(SQLiteDatabase database, int oldVersion,
	      int newVersion) {
	  }
}
