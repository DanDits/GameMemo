package dan.dit.gameMemo.gameData.game.doppelkopf;

import dan.dit.gameMemo.R;
import dan.dit.gameMemo.util.compaction.CompactedDataCorruptException;
import dan.dit.gameMemo.util.compaction.Compacter;

public class DoppelkopfRe extends DoppelkopfRoundStyle {
	public static final int NORMAL = 0; // type with lowest index = first valid type
	public static final int HOCHZEIT = 1;
	public static final int ARMUT = 2; // type with highest index = last valid type
	public static final int DEFAULT_RE_STYLE = NORMAL;
	
	
	public DoppelkopfRe(int type, int firstReIndex, int secondReIndex) {
		super(type);
		setFirstReIndex(firstReIndex);
		setSecondReIndex(secondReIndex);
	}
	
	public DoppelkopfRe(Compacter compactedData) throws CompactedDataCorruptException {
		super(compactedData);
	}

	public static final DoppelkopfRoundStyle makeHochzeit(int firstReIndex, int secondReIndex) {
		if (isValidIndex(firstReIndex) && isValidIndex(secondReIndex) && firstReIndex != secondReIndex) {
			return new DoppelkopfRe(HOCHZEIT, firstReIndex, secondReIndex);
		} else if (isValidIndex(firstReIndex)) {
			return DoppelkopfSolo.makeStilleHochzeit(firstReIndex);
		} else if (isValidIndex(secondReIndex)) {
			return DoppelkopfSolo.makeStilleHochzeit(secondReIndex);
		} else {
			throw new IllegalArgumentException("Illegal indices for hochzeit: " + firstReIndex + " and " + secondReIndex);
		}
	}
	
	public static final DoppelkopfRoundStyle makeNormal(int firstReIndex, int secondReIndex) {
		return new DoppelkopfRe(NORMAL, firstReIndex, secondReIndex);
	}
	
	public static final DoppelkopfRoundStyle makeArmut(int firstReIndex, int secondReIndex) {
		return new DoppelkopfRe(ARMUT, firstReIndex, secondReIndex);
	}
	
	public boolean isValidType(int type) {
		return type >= NORMAL && type <= ARMUT;
	}

	@Override
	public String compact() {
		Compacter cmp = new Compacter();
		cmp.appendData(DoppelkopfRoundStyle.RE_STYLE_MARK);
		cmp.appendData(getType());
		cmp.appendData(getFirstIndex());
		cmp.appendData(getSecondIndex());
		return cmp.compact();
	}

	@Override
	public void unloadData(Compacter compactedData)
			throws CompactedDataCorruptException {
		if (compactedData.getSize() < 4) {
			throw new CompactedDataCorruptException().setCorruptData(compactedData);
		}
		mType = compactedData.getInt(1);
		if (!isValidType(mType)) {
			throw new CompactedDataCorruptException().setCorruptData(compactedData);
		}
		setFirstReIndex(compactedData.getInt(2));
		setSecondReIndex(compactedData.getInt(3));
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof DoppelkopfRe) {
			return ((DoppelkopfRe) other).mType == mType 
					&& ((((DoppelkopfRe) other).getFirstIndex() == getFirstIndex()
							&& ((DoppelkopfRe) other).getSecondIndex() == getSecondIndex())
						|| (((DoppelkopfRe) other).getFirstIndex() == getSecondIndex()
								&& ((DoppelkopfRe) other).getSecondIndex() == getFirstIndex()));
		} else {
			return super.equals(other);
		}
	}
	
	@Override
	public String toString() {
		String typeString = "";
		switch (mType) {
		case NORMAL:
			typeString = "Normal";
			break;
		case ARMUT:
			typeString = "Armut";
			break;
		case HOCHZEIT:
			typeString = "Hochzeit";
			break;
		}
		return "GameStyle: " + typeString;
	}

	@Override
	public int getNameResource() {
		switch (mType) {
		case NORMAL:
			return R.string.doppelkopf_game_style_normal;
		case HOCHZEIT:
			return R.string.doppelkopf_game_style_hochzeit;
		case ARMUT:
			return R.string.doppelkopf_game_style_armut;
		default:
			throw new IllegalStateException();
		}
	}

	@Override
	public void setNextType() {
		mType++;
		if (mType > ARMUT) {
			mType = 0;
		}
	}
}
