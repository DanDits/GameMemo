package dan.dit.gameMemo.gameData.game.doppelkopf;

import dan.dit.gameMemo.R;
import dan.dit.gameMemo.util.compaction.Compactable;
import dan.dit.gameMemo.util.compaction.CompactedDataCorruptException;
import dan.dit.gameMemo.util.compaction.Compacter;

public class DoppelkopfRoundResult implements Compactable {
	public static final int R_BLACK = -5;
	public static final int R_0_29 = -4;
	public static final int R_30_59 = -3;
	public static final int R_60_89 = -2;
	public static final int R_90_119 = -1;
	public static final int R_120 = 0;
	public static final int R_121_150 = 1; // 120 > enemy score >= 90
	public static final int R_151_180 = 2; // 90 > enemy score >= 60
	public static final int R_181_210 = 3; // 60 > enemy score >= 30
	public static final int R_211_240 = 4; // 30 > enemy score >= 0
	public static final int R_WHITE = 5; // enemy is BLACK
	
	private int mResult;
	
	public DoppelkopfRoundResult() {
		mResult = R_120;
	}
	
	public DoppelkopfRoundResult(Compacter cmp) throws CompactedDataCorruptException {
		unloadData(cmp);
	}
	
	public DoppelkopfRoundResult(int reResult) {
		mResult = reResult;
		if (mResult > R_WHITE) {
			mResult = R_WHITE;
		} else if (mResult < R_BLACK) {
			mResult = R_BLACK;
		}
	}
	
	public void improve(boolean re) {
		if (re) {
			if (mResult < R_WHITE) {
				mResult++;
			}
		} else {
			degrade(true);
		}
	}
	
	public void degrade(boolean re) {
		if (re) {
			if (mResult > R_BLACK) {
				mResult--;
			}
		} else {
			improve(true);
		}
	}
	
	public int getReResult() {
		return mResult;
	}
	
	public int getContraResult() {
		return -mResult;
	}

	@Override
	public String compact() {
		Compacter cmp = new Compacter();
		cmp.appendData(mResult);
		return cmp.compact();
	}

	@Override
	public void unloadData(Compacter compactedData)
			throws CompactedDataCorruptException {
		if (compactedData.getSize() < 1) {
			throw new CompactedDataCorruptException().setCorruptData(compactedData);
		}
		mResult = compactedData.getInt(0);
	}
	
	@Override
	public boolean equals(Object other) {
		if (other instanceof DoppelkopfRoundResult) {
			return ((DoppelkopfRoundResult) other).mResult == mResult;
		} else {
			return super.equals(other);
		}
	}
	
	@Override
	public String toString() {
		switch (mResult) {
		case R_BLACK:
			return "Black";
		case R_WHITE:
			return "White";
		case R_120:
			return "120";
		default:
			return Integer.toString((mResult + 3 + (mResult > 0 ? 0 : 1)) * 30 + 10);
		}
	}

	public int getNameResource(boolean re) {
		switch (re ? mResult : -mResult) {
		case R_0_29:
			return R.string.doppelkopf_result_0_29;
		case R_120:
			return R.string.doppelkopf_result_120;
		case R_121_150:
			return R.string.doppelkopf_result_121_150;
		case R_151_180:
			return R.string.doppelkopf_result_151_180;
		case R_181_210:
			return R.string.doppelkopf_result_181_210;		
		case R_211_240:
			return R.string.doppelkopf_result_211_240;			
		case R_30_59:
			return R.string.doppelkopf_result_30_59;			
		case R_60_89:
			return R.string.doppelkopf_result_60_89;			
		case R_90_119:
			return R.string.doppelkopf_result_90_119;		
		case R_BLACK:
			return R.string.doppelkopf_result_black;			
		case R_WHITE:
			return R.string.doppelkopf_result_white;
		default:
			throw new IllegalStateException();
		}
	}
}
