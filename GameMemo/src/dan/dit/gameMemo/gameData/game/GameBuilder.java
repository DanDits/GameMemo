package dan.dit.gameMemo.gameData.game;

import dan.dit.gameMemo.util.compression.CompressedDataCorruptException;
import dan.dit.gameMemo.util.compression.Compressor;

public abstract class GameBuilder {
	protected Game mInst;
	
	public static Compressor getCompressor(Game game) { // works together with loadAll
		Compressor cmp = new Compressor(6);
		cmp.appendData(game.startTime);
		cmp.appendData(game.mRunningTime);
		cmp.appendData(game.getPlayerData());
		cmp.appendData(game.getRoundsData());
		cmp.appendData(game.getMetaData());
		cmp.appendData(game.getOriginData());
		return cmp;
	}
	
	public final GameBuilder loadAll(Compressor allData) throws CompressedDataCorruptException {
		if (allData.getSize() < 6) {
			throw new CompressedDataCorruptException("Too little data provided to construct a game.");
		}
		try {
			setStarttime(Long.parseLong(allData.getData(0)));
		} catch (NumberFormatException nfe) {
			throw new CompressedDataCorruptException("Could not parse start time.", nfe);
		}
		try {
			setRunningTime(Long.parseLong(allData.getData(1)));
		} catch (NumberFormatException nfe) {
			throw new CompressedDataCorruptException("Could not parse run time.", nfe);
		}
		Compressor playerData = new Compressor(allData.getData(2));
		Compressor roundsData = new Compressor(allData.getData(3));
		Compressor metaData = new Compressor(allData.getData(4));
		Compressor originData = new Compressor(allData.getData(5));
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

	public abstract GameBuilder loadMetadata(Compressor metaData) throws CompressedDataCorruptException; // must be invoked before loadPlayer and before loadRounds
	public abstract GameBuilder loadPlayer(Compressor playerData) throws CompressedDataCorruptException;
	public abstract GameBuilder loadRounds(Compressor roundData) throws CompressedDataCorruptException; // must be invoked after loadPlayer and loadMetaData
	
	public GameBuilder loadOrigin(Compressor originData) {
		mInst.originData = originData.compress();
		return this;
	}
	
	public Game build() {
		Game temp = mInst;
		mInst = null;
		return temp;
	}
}
