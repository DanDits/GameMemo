package dan.dit.gameMemo.gameData.game.doppelkopf;

import dan.dit.gameMemo.util.compaction.Compactable;
import dan.dit.gameMemo.util.compaction.CompactedDataCorruptException;
import dan.dit.gameMemo.util.compaction.Compacter;

public class DoppelkopfExtraScore implements Compactable {
	public static final char FOX_CAUGHT = 'F';
	public static final char CHARLY = 'C';
	public static final char DOPPELKOPF = 'D';
	
	private int mFoxCountRe;
	private int mFoxCountContra;
	private int mCharlyCountRe;
	private int mCharlyCountContra;
	private int mDoppelkopfCountRe;
	private int mDoppelkopfCountContra;
	private int mExtraRe;
	private int mExtraContra;
	
	public DoppelkopfExtraScore() {
		mFoxCountRe = mFoxCountContra = 0;
		mDoppelkopfCountRe = mDoppelkopfCountContra = 0;
		mCharlyCountRe = mCharlyCountContra = 0;
		mExtraContra = mExtraRe = 0;
	}
	
	@Override
	public boolean equals(Object other) {
		if (other instanceof DoppelkopfExtraScore) {
			DoppelkopfExtraScore o = (DoppelkopfExtraScore) other;
			return o.mFoxCountRe == mFoxCountRe && o.mFoxCountContra == mFoxCountContra
				&& o.mCharlyCountContra == mCharlyCountContra && o.mCharlyCountRe == mCharlyCountRe
				&& o.mDoppelkopfCountContra == mDoppelkopfCountContra && o.mDoppelkopfCountRe == mDoppelkopfCountRe
				&& o.mExtraContra == mExtraContra && o.mExtraRe == mExtraRe ;
		} else {
			return super.equals(other);
		}
	}
	
	public DoppelkopfExtraScore(Compacter compactedData) throws CompactedDataCorruptException {
		unloadData(compactedData);
	}
	
	public int getFoxCount(boolean re) {
		return re ? mFoxCountRe : mFoxCountContra;
	}
	
	public int getDoppelkopfCount(boolean re) {
		return re ? mDoppelkopfCountRe : mDoppelkopfCountContra;
	}
	
	public int getCharlyCount(boolean re) {
		return re ? mCharlyCountRe : mCharlyCountContra;
	}
	
	public int getOtherExtraScore(boolean re) {
		return re ? mExtraRe : mExtraContra;
	}
	
	public void setOtherExtraScore(boolean re, int otherScore) {
		if (re) {
			mExtraRe = otherScore;
		} else {
			mExtraContra = otherScore;
		}
	}
	
	public int getExtraScore(boolean re) {
		if (re) {
			return mExtraRe + mFoxCountRe + mDoppelkopfCountRe + mCharlyCountRe; 
		} else {
			return mExtraContra + mFoxCountContra + mDoppelkopfCountContra + mCharlyCountContra;
		}
	}
	
	public int getSpecialScore(char type, boolean re) {
		switch (type) {
		case FOX_CAUGHT:
			return getFoxCount(re);
		case CHARLY:
			return getCharlyCount(re);
		case DOPPELKOPF:
			return getDoppelkopfCount(re);
		default:
			return 0;
		}
	}
	
	public void nextFox(boolean re) {
		if (getMaxAmount(FOX_CAUGHT) - mFoxCountContra - mFoxCountRe > 0) {
			// still a fox available to be caught
			if (re) {
				mFoxCountRe++;
			} else {
				mFoxCountContra++;
			}
		} else {
			// no free fox, get one from other party or release all
			if (re) {
				if (mFoxCountContra > 0) {
					mFoxCountContra--;
					mFoxCountRe++;
				} else {
					mFoxCountRe = 0;
				}
			} else {
				if (mFoxCountRe > 0) {
					mFoxCountRe--;
					mFoxCountContra++;
				} else {
					mFoxCountContra = 0;
				}
			}
		}
	}
	
	public void nextDoppelkopf(boolean re) {
		if (getMaxAmount(DOPPELKOPF) - mDoppelkopfCountContra - mDoppelkopfCountRe > 0) {
			// still a doppelkopf available
			if (re) {
				mDoppelkopfCountRe++;
			} else {
				mDoppelkopfCountContra++;
			}
		} else {
			// no free doppelkopf, get one from other party or release all
			if (re) {
				if (mDoppelkopfCountContra > 0) {
					mDoppelkopfCountContra--;
					mDoppelkopfCountRe++;
				} else {
					mDoppelkopfCountRe = 0;
				}
			} else {
				if (mDoppelkopfCountRe > 0) {
					mDoppelkopfCountRe--;
					mDoppelkopfCountContra++;
				} else {
					mDoppelkopfCountContra = 0;
				}
			}
		}
	}
	
	public void nextCharly(boolean re) {
		if (getMaxAmount(CHARLY) -mCharlyCountRe - mCharlyCountContra > 0) {
			if (re) {
				mCharlyCountRe++;
			} else {
				mCharlyCountContra++;
			}
		} else {
			// no free doppelkopf, get one from other party or release all
			if (re) {
				if (mCharlyCountContra > 0) {
					mCharlyCountContra--;
					mCharlyCountRe++;
				} else {
					mCharlyCountRe = 0;
				}
			} else {
				if (mCharlyCountRe > 0) {
					mCharlyCountRe--;
					mCharlyCountContra++;
				} else {
					mCharlyCountContra = 0;
				}
			}
		}
	}
	
	public static int getMaxAmount(char type) {
		switch(type) {
		case FOX_CAUGHT:
			return 2;
		case CHARLY:
			return 1;
		case DOPPELKOPF:
			return 4;
		default:
			return 0;
		}
	}


	@Override
	public String compact() {
		Compacter cmp = new Compacter();
		cmp.appendData(mExtraRe);
		cmp.appendData(mExtraContra);
		cmp.appendData(mDoppelkopfCountRe);
		cmp.appendData(mDoppelkopfCountContra);
		cmp.appendData(mFoxCountRe);
		cmp.appendData(mFoxCountContra);
		cmp.appendData(mCharlyCountRe);
		cmp.appendData(mCharlyCountContra);
		return cmp.compact();
	}
	
	public void unloadData(Compacter compactedData) throws CompactedDataCorruptException {
		if (compactedData.getSize() < 8) {
			throw new CompactedDataCorruptException("Too little data in " + compactedData);
		}
		mExtraRe = compactedData.getInt(0);
		mExtraContra = compactedData.getInt(1);
		mDoppelkopfCountRe = compactedData.getInt(2);
		mDoppelkopfCountContra = compactedData.getInt(3);
		mFoxCountRe = compactedData.getInt(4);
		mFoxCountContra = compactedData.getInt(5);
		mCharlyCountRe = compactedData.getInt(6);
		mCharlyCountContra = compactedData.getInt(7);
	}
	
	@Override
	public String toString() {
	    return "Extras(" + mExtraRe + "," + mExtraContra + "," + mDoppelkopfCountRe + "," + mDoppelkopfCountContra 
	            + "," + mFoxCountRe + "," + mFoxCountContra + "," + mCharlyCountRe + "," + mCharlyCountContra;
	}

}
