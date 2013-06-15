package dan.dit.gameMemo.gameData.game.doppelkopf;

import dan.dit.gameMemo.util.compaction.Compactable;
import dan.dit.gameMemo.util.compaction.CompactedDataCorruptException;
import dan.dit.gameMemo.util.compaction.Compacter;

public abstract class DoppelkopfRoundStyle implements Compactable {
	public static final int NO_INDEX = -1;
	protected int mType;
	private int mFirstReIndex = NO_INDEX;
	private int mSecondReIndex = NO_INDEX;
	protected static final String SOLO_STYLE_MARK = "S";
	protected static final String RE_STYLE_MARK = "R";
	
	public DoppelkopfRoundStyle(int type) {
		mType = type;
		if (!isValidType(mType)) {
			throw new IllegalArgumentException("Illegal type " + mType);
		}
	}
	
	public abstract int getNameResource();
	
	public DoppelkopfRoundStyle(Compacter compactedData) throws CompactedDataCorruptException {
		unloadData(compactedData);
	}

	public static DoppelkopfRoundStyle buildRoundStyle(Compacter compactedData) throws CompactedDataCorruptException {
		if (compactedData.getData(0).equals(SOLO_STYLE_MARK)) {
			return new DoppelkopfSolo(compactedData);
		} else if (compactedData.getData(0).equals(RE_STYLE_MARK)) {
			return new DoppelkopfRe(compactedData);			
		} else {
			return null;
		}
	}
	
	public abstract boolean isValidType(int type); // first valid type must be 0
	
	public static boolean isValidIndex(int index) {
		return index >= 0 && index < DoppelkopfGame.MAX_PLAYERS;
	}
	
	public void setFirstReIndex(int index) {
		if (isValidIndex(index) && index != mSecondReIndex) {
			mFirstReIndex = index;
		} else {
			throw new IllegalArgumentException("Illegal indices: (setting " + index + "): " + mFirstReIndex + " and " + mSecondReIndex);			
		}
	}
	
	public void setSecondReIndex(int index) {
		if (isValidIndex(index) && index != mFirstReIndex) {
			mSecondReIndex = index;
		} else {
			throw new IllegalArgumentException("Illegal indices (setting " + index + "): " + mFirstReIndex + " and " + mSecondReIndex);			
		}
	}
	
	public int getType() {
		return mType;
	}
	
	public int getFirstIndex() {
		return mFirstReIndex;
	}
	
	public int getSecondIndex() {
		return mSecondReIndex;
	}

	public abstract void setNextType();

	public void setType(int type) {
		mType = type;
	}
}
