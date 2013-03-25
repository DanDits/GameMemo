package dan.dit.gameMemo.storage.database;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import android.database.sqlite.SQLiteDatabase;
import dan.dit.gameMemo.gameData.game.tichu.TichuGame;
import dan.dit.gameMemo.storage.GameStorageHelper;

/**
 * A helper class for creating and upgrading the tichu table in the games
 * database. See {@link GameSQLiteHelper}.
 * @author Daniel
 *
 */
public class TichuTable {

  /**
   * The tichu table name
   */
  public static final String TABLE_TICHU_GAMES = GameSQLiteHelper.GENERAL_GAME_TABLE_PREFIX + TichuGame.GAME_NAME;
  
  /**
   * An array of all available columns, querying the content provider with a column not listed here will result in an exception
   * so make sure it contains all general game columns plus the tichu specific columns.
   */
  public static final String[] AVAILABLE_COLUMNS = { GameStorageHelper.COLUMN_PLAYERS,
	        GameStorageHelper.COLUMN_ROUNDS, GameStorageHelper.COLUMN_ID, GameStorageHelper.COLUMN_STARTTIME,
	        GameStorageHelper.COLUMN_WINNER, GameStorageHelper.COLUMN_METADATA, GameStorageHelper.COLUMN_RUNTIME,
	        GameStorageHelper.COLUMN_ORIGIN};
  public static final Collection<String> AVAILABLE_COLUMNS_COLL = new HashSet<String>(
			Arrays.asList(AVAILABLE_COLUMNS));
  
  // Database creation SQL statement
  private static final String DATABASE_CREATE = "create table " 
      + TABLE_TICHU_GAMES
      + "(" 
      + GameSQLiteHelper.DATABASE_CREATE_DEFAULT_TABLE_COLUMNS
      + ");";

  private TichuTable() {} // make sure it is never instantiated
  
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
	  if (oldVersion == 1 && newVersion == 2) {
		  database.execSQL("ALTER TABLE " + TABLE_TICHU_GAMES + " ADD COLUMN " + GameSQLiteHelper.DATABASE_UPGRADE_COLUMN_METADATA_CONSTRAINT);
		  database.execSQL("ALTER TABLE " + TABLE_TICHU_GAMES + " ADD COLUMN " + GameSQLiteHelper.DATABASE_UPGRADE_COLUMN_RUNTIME_CONSTRAINT);
		  database.execSQL("ALTER TABLE " + TABLE_TICHU_GAMES + " ADD COLUMN " + GameSQLiteHelper.DATABASE_UPGRADE_COLUMN_ORIGIN_CONSTRAINT);
	  }
  }
} 