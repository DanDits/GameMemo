package dan.dit.gameMemo.gameData.game;

import java.util.LinkedList;
import java.util.List;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.text.TextUtils;
import dan.dit.gameMemo.storage.GameStorageHelper;
import dan.dit.gameMemo.storage.database.SportGameTable;
import dan.dit.gameMemo.util.compaction.CompactedDataCorruptException;
import dan.dit.gameMemo.util.compaction.Compacter;


public abstract class SportGame extends Game {
    protected String mLocation;
    
    public String getLocation() {
        return mLocation;
    }
    
    public void setLocation(String location) {
        if (location == null) {
            mLocation = "";
        } else {
            mLocation = location;
        }
    }
    
    public static List<Game> loadGamesAtSameLocation(int gameKey, ContentResolver resolver, String location, boolean throwAtFailure) throws CompactedDataCorruptException {
        if (TextUtils.isEmpty(location)) {
            throw new IllegalArgumentException("Cannot search for empty location's previous games.");
        }
        String[] projection = GameKey.getStorageAvailableColumnsProj(gameKey);;
        Cursor cursor = null;
        try {
            cursor = resolver.query(GameStorageHelper.getUriAllItems(gameKey), projection, SportGameTable.COLUMN_LOCATION + " = ?", new String[] {location},
                    GameStorageHelper.COLUMN_STARTTIME + " DESC");
            List<Game> games = null;
            if (cursor != null) {
                games = new LinkedList<Game>();
                cursor.moveToFirst();
                while (!cursor.isAfterLast()) {
                    Game g = GameKey.getBuilder(gameKey).loadCursor(cursor).build();
                    
                    if (g != null) {
                        games.add(g);
                    }
                    cursor.moveToNext();
                }
            }
            return games;
        } finally {
            // Always close the cursor
            if (cursor != null) {
                cursor.close();
            }
        }
    }
    
    @Override
    public void saveGame(ContentResolver resolver) {
        ContentValues val = new ContentValues();
        val.put(SportGameTable.COLUMN_LOCATION, mLocation);
        super.saveGame(val, resolver);
    }
    
    protected Compacter getMetaDataInCompacter() {
        Compacter cmp = new Compacter(1);
        cmp.appendData(mLocation);
        return cmp;
    }
}
