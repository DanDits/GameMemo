package dan.dit.gameMemo.gameData.statistics;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class StatisticsDbHelper extends SQLiteOpenHelper {
    // If you change the database schema, you must increment the database version.
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "Statistics.db";
    private static StatisticsDbHelper INSTANCE;
    
    public static StatisticsDbHelper makeInstance(Context applicationContext) {
        if (INSTANCE == null) {
            synchronized (StatisticsDbHelper.class) {
                if (INSTANCE == null) {// ensure we are using the application context for the static member!
                    INSTANCE = new StatisticsDbHelper(applicationContext.getApplicationContext()); 
                }
            }
        }
        return INSTANCE;
    }
    
    public static StatisticsDbHelper getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException();
        }
        return INSTANCE;
    }
    private StatisticsDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Attributes table
        db.execSQL(StatisticsContract.Attribute.SQL_CREATE_ENTRIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // currently only one version, edit if db version changes
    }

}
