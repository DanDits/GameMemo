package dan.dit.gameMemo.gameData.game;

import dan.dit.gameMemo.util.compaction.CompactedDataCorruptException;
import dan.dit.gameMemo.util.compaction.Compacter;

public abstract class GameBuilder {
	protected Game mInst;
	
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
		try {
			setStarttime(Long.parseLong(allData.getData(0)));
		} catch (NumberFormatException nfe) {
			throw new CompactedDataCorruptException("Could not parse start time.", nfe);
		}
		try {
			setRunningTime(Long.parseLong(allData.getData(1)));
		} catch (NumberFormatException nfe) {
			throw new CompactedDataCorruptException("Could not parse run time.", nfe);
		}
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
		mInst.originData = originData.compress();
		return this;
	}
	
	public Game build() {
		Game temp = mInst;
		mInst = null;
		return temp;
	}
}
