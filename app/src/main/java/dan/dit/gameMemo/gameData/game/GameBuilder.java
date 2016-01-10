package dan.dit.gameMemo.gameData.game;

import android.database.Cursor;
import dan.dit.gameMemo.storage.GameStorageHelper;
import dan.dit.gameMemo.util.compaction.CompactedDataCorruptException;
import dan.dit.gameMemo.util.compaction.Compacter;

public abstract class GameBuilder {
	protected Game mInst;
	
	public GameBuilder loadCursor(Cursor cursor) throws CompactedDataCorruptException {
	    String playerData = cursor.getString(cursor
                .getColumnIndexOrThrow(GameStorageHelper.COLUMN_PLAYERS));
        String roundsData = cursor.getString(cursor
                .getColumnIndexOrThrow(GameStorageHelper.COLUMN_ROUNDS));
        String metaData = cursor.getString(cursor.getColumnIndexOrThrow(GameStorageHelper.COLUMN_METADATA));
        long startTime = cursor.getLong(cursor
                .getColumnIndexOrThrow(GameStorageHelper.COLUMN_STARTTIME));
        long id = cursor.getLong(cursor.getColumnIndexOrThrow(GameStorageHelper.COLUMN_ID));
        long runningTime = cursor.getLong(cursor.getColumnIndexOrThrow(GameStorageHelper.COLUMN_RUNTIME));
        String originData = cursor.getString(cursor.getColumnIndexOrThrow(GameStorageHelper.COLUMN_ORIGIN));
        
        loadMetadata(new Compacter(metaData))
        .setStarttime(startTime)
        .setRunningTime(runningTime)
        .setId(id)
        .loadPlayer(new Compacter(playerData))
        .loadOrigin(new Compacter(originData))
        .loadRounds(new Compacter(roundsData));    
        return this;
	}
	
	public static Compacter getCompressor(Game game) { // works together with loadAll
		Compacter cmp = new Compacter(6);
		cmp.appendData(game.startTime);
		cmp.appendData(game.mRunningTime);
		cmp.appendData(game.getPlayerData());
		cmp.appendData(game.getRoundsData());
		cmp.appendData(game.getMetaData());
		cmp.appendData(game.getOriginData());
		return cmp;
	}
	
	public final GameBuilder loadAll(Compacter allData) throws CompactedDataCorruptException {
		if (allData.getSize() < 6) {
			throw new CompactedDataCorruptException("Too little data provided to construct a game.");
		}
		setStarttime(allData.getLong(0));
		setRunningTime(allData.getLong(1));
		Compacter playerData = new Compacter(allData.getData(2));
		Compacter roundsData = new Compacter(allData.getData(3));
		Compacter metaData = new Compacter(allData.getData(4));
		Compacter originData = new Compacter(allData.getData(5));
		loadMetadata(metaData);
		loadPlayer(playerData);
		loadRounds(roundsData);
		loadOrigin(originData);
		return this;
	}
	
	public GameBuilder setId(long id) {
		if (!Game.isValidId(id) && id != Game.NO_ID) {
			throw new IllegalArgumentException("Invalid id and not NO_ID: " + id);
		}
		mInst.mId = id;
		return this;
	}
	
	public GameBuilder setStarttime(long startTime) {
		mInst.startTime = startTime;
		return this;
	}
	
	public GameBuilder setRunningTime(long runningTime) {
		mInst.mRunningTime = runningTime;
		return this;
	}

	public abstract GameBuilder loadMetadata(Compacter metaData) throws CompactedDataCorruptException; // must be invoked before loadPlayer and before loadRounds
	public abstract GameBuilder loadPlayer(Compacter playerData) throws CompactedDataCorruptException;
	public abstract GameBuilder loadRounds(Compacter roundData) throws CompactedDataCorruptException; // must be invoked after loadPlayer and loadMetaData
	
	public GameBuilder loadOrigin(Compacter originData) {
		mInst.originData = originData.compact();
		return this;
	}
	
	public Game build() {
		Game temp = mInst;
		mInst = null;
		return temp;
	}
}
