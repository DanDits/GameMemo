package dan.dit.gameMemo.storage.database;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import android.database.sqlite.SQLiteDatabase;
import dan.dit.gameMemo.storage.GameStorageHelper;

public class SportGameTable {
	
	 /**
	   * The sport games table name
	   */
	  public static final String TABLE_SPORT_GAMES = GameSQLiteHelper.GENERAL_GAME_TABLE_PREFIX + "SportGames";
	  
	  /**
	   * Can store the location of where the game took place, like a specific town, gym or bar.
	   */
      public static final String COLUMN_LOCATION ="location";
      
	  /**
	   * An array of all available columns, querying the content provider with a column not listed here will result in an exception
	   * so make sure it contains all general game columns plus the sport games specific columns.
	   */
	  public static final String[] AVAILABLE_COLUMNS = new String[GameStorageHelper.AVAILABLE_COLUMNS.length + 1];
	  static {
	      int index = 0;
	      for (String col : GameStorageHelper.AVAILABLE_COLUMNS) {
	          AVAILABLE_COLUMNS[index] = col;
	          index++;
	      }
	      AVAILABLE_COLUMNS[AVAILABLE_COLUMNS.length - 1] = COLUMN_LOCATION;
	  }
	  public static final Collection<String> AVAILABLE_COLUMNS_COLL = new HashSet<String>(
				Arrays.asList(AVAILABLE_COLUMNS));

	  // Database creation SQL statement
	  private static final String DATABASE_CREATE = "create table " 
	      + TABLE_SPORT_GAMES
	      + "(" 
	      + GameSQLiteHelper.DATABASE_CREATE_DEFAULT_TABLE_COLUMNS + ", "
	      + COLUMN_LOCATION + " text not null"
	      + ");";

	  private SportGameTable() {} // make sure it is never instantiated
	  
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
	      if (newVersion > 2) {
	          // table came to life with version 2
	      }
	  }
}
