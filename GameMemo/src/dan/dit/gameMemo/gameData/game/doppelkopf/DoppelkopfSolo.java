package dan.dit.gameMemo.gameData.game.doppelkopf;

import dan.dit.gameMemo.R;
import dan.dit.gameMemo.util.compaction.CompactedDataCorruptException;
import dan.dit.gameMemo.util.compaction.Compacter;

public class DoppelkopfSolo extends DoppelkopfRoundStyle {
	public static final int FLEISCHLOSER = 0; // type with lowest index = first valid type
	public static final int BUBEN = 1;
	public static final int DAMEN = 2;
	public static final int STILLE_HOCHZEIT = 3; 
	public static final int FARB = 4; // type with highest index = last valid type
	public static final int DEFAULT_SOLO_TYPE = DoppelkopfSolo.FLEISCHLOSER;
	
	public DoppelkopfSolo(int type, int firstReIndex) {
		super(type);
		setFirstReIndex(firstReIndex);
	}

	public DoppelkopfSolo(Compacter compactedData) throws CompactedDataCorruptException {
		super(compactedData);
	}

	public static DoppelkopfRoundStyle makeStilleHochzeit(int firstReIndex) {
		return new DoppelkopfSolo(STILLE_HOCHZEIT, firstReIndex);
	}
	
	@Override
	public boolean isValidType(int type) {
		return type >= FLEISCHLOSER && type <= STILLE_HOCHZEIT;
	}

	@Override
	public boolean keepsGiver() {
		return mType != STILLE_HOCHZEIT;
	}
	
	public boolean isValidDutySolo() {
		return mType != STILLE_HOCHZEIT;
	}

	@Override
	public String compact() {
		Compacter cmp = new Compacter();
		cmp.appendData(DoppelkopfRoundStyle.SOLO_STYLE_MARK);
		cmp.appendData(getType());
		cmp.appendData(getFirstIndex());
		return cmp.compact();
	}

	@Override
	public void unloadData(Compacter compactedData)
			throws CompactedDataCorruptException {
		if (compactedData.getSize() < 3) {
			throw new CompactedDataCorruptException().setCorruptData(compactedData);
		}
		mType = compactedData.getInt(1);
		if (!isValidType(mType)) {
			throw new CompactedDataCorruptException().setCorruptData(compactedData);
		}
		setFirstReIndex(compactedData.getInt(2));
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof DoppelkopfSolo) {
			return ((DoppelkopfSolo) other).mType == mType 
					&& ((DoppelkopfSolo) other).getFirstIndex() == getFirstIndex();
		} else {
			return super.equals(other);
		}
	}
	
	@Override
	public String toString() {
		String typeString = "";
		switch (mType) {
		case FLEISCHLOSER:
			typeString = "Fleischloser";
			break;
		case BUBEN:
			typeString = "BubenSolo";
			break;
		case DAMEN:
			typeString = "DamenSolo";
			break;
		case FARB:
			typeString = "Farb";
			break;
		case STILLE_HOCHZEIT:
			typeString = "StilleHochzeit";
			break;
			
		}
		return "GameStyle: " + typeString;
	}

	@Override
	public int getNameResource() {
		switch (mType) {
		case BUBEN:
			return R.string.doppelkopf_game_style_buben_solo;
		case DAMEN:
			return R.string.doppelkopf_game_style_damen_solo;
		case FARB:
			return R.string.doppelkopf_game_style_farb_solo;
		case FLEISCHLOSER:
			return R.string.doppelkopf_game_style_fleischloser;
		case STILLE_HOCHZEIT:
			return R.string.doppelkopf_game_style_stille_hochzeit;
		default:
			throw new IllegalStateException();
		}
	}

	@Override
	public void setNextType() {
		int next = DEFAULT_SOLO_TYPE;
		switch (mType) {
		case FLEISCHLOSER:
			next = BUBEN; break;
		case BUBEN:
			next = DAMEN; break;
		case DAMEN:
			next = FARB; break;
		case FARB:
			next = STILLE_HOCHZEIT; break;
		case STILLE_HOCHZEIT:
			next = FLEISCHLOSER; break;
		default:
			throw new IllegalStateException();
		}
		mType = next;
	}
}
