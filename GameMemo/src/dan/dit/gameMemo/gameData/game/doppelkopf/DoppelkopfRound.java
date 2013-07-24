package dan.dit.gameMemo.gameData.game.doppelkopf;

import dan.dit.gameMemo.gameData.game.GameRound;
import dan.dit.gameMemo.util.compaction.CompactedDataCorruptException;
import dan.dit.gameMemo.util.compaction.Compacter;


public class DoppelkopfRound extends GameRound {
	
	private DoppelkopfRoundStyle mRoundStyle;
	private DoppelkopfRoundResult mRoundResult;
	private DoppelkopfBid mReBid;
	private DoppelkopfBid mContraBid;
	private DoppelkopfExtraScore mExtraScore;
	private DoppelkopfRuleSystem mRuleSys;
	
	private DoppelkopfRound(DoppelkopfRuleSystem ruleSys, int reIndex,  int gameStyle) {
		mRuleSys = ruleSys;
		mRoundStyle = new DoppelkopfSolo(gameStyle, reIndex);	
		mReBid = new DoppelkopfBid();
		mContraBid = new DoppelkopfBid();
		mRoundResult = mRuleSys.makeNeutralRoundResult();
		mExtraScore = new DoppelkopfExtraScore();
	}
	
	private DoppelkopfRound(DoppelkopfRuleSystem ruleSys, int firstReIndex, int secondReIndex,  int gameStyle) {
		mRuleSys = ruleSys;
		mRoundStyle = new DoppelkopfRe(gameStyle, firstReIndex, secondReIndex);
		mReBid = new DoppelkopfBid();
		mContraBid = new DoppelkopfBid();
		mRoundResult = mRuleSys.makeNeutralRoundResult();
		mExtraScore = new DoppelkopfExtraScore();
	}
	
	protected DoppelkopfRound(DoppelkopfRuleSystem ruleSys, Compacter compactedData) throws CompactedDataCorruptException {
		mRuleSys = ruleSys;
		unloadData(compactedData);
	}

	public static DoppelkopfRound makeRound(DoppelkopfRuleSystem ruleSys, int firstReIndex, int secondReIndex) {
		if (DoppelkopfRoundStyle.isValidIndex(firstReIndex) && DoppelkopfRoundStyle.isValidIndex(secondReIndex) 
				&& firstReIndex != secondReIndex) {
			return new DoppelkopfRound(ruleSys, firstReIndex, secondReIndex, DoppelkopfRe.DEFAULT_RE_STYLE);
		} else if (DoppelkopfRoundStyle.isValidIndex(firstReIndex)) {
			return new DoppelkopfRound(ruleSys, firstReIndex, DoppelkopfSolo.DEFAULT_SOLO_TYPE);
		} else if (DoppelkopfRoundStyle.isValidIndex(secondReIndex)) {
			return new DoppelkopfRound(ruleSys, secondReIndex, DoppelkopfSolo.DEFAULT_SOLO_TYPE);
		} else {
			return null;
		}
	}
	
	@Override
	public String compact() {
		Compacter cmp = new Compacter();
		cmp.appendData(mRoundStyle.compact());
		cmp.appendData(mRoundResult.compact());
		cmp.appendData(mReBid.getType());
		cmp.appendData(mContraBid.getType());
		cmp.appendData(mExtraScore.compact());
		return cmp.compact();
	}

	public void unloadData(Compacter compactedData) throws CompactedDataCorruptException {
		mRoundStyle = DoppelkopfRoundStyle.buildRoundStyle(new Compacter(compactedData.getData(0)));
		mRoundResult = mRuleSys.makeRoundResult(new Compacter(compactedData.getData(1)));
		if (mRoundStyle == null || mRoundResult == null) {
			throw new CompactedDataCorruptException("Round style or round result invalid from data " + compactedData);
		}
		mReBid = new DoppelkopfBid(compactedData.getInt(2));
		mContraBid = new DoppelkopfBid(compactedData.getInt(3));
		mExtraScore = new DoppelkopfExtraScore(new Compacter(compactedData.getData(4)));
	}
	
	public DoppelkopfExtraScore getExtraScore() {
		return mExtraScore;
	}
	
	public DoppelkopfRoundResult getRoundResult() {
		return mRoundResult;
	}

	public DoppelkopfBid getReBid() {
		return mReBid;
	}
	
	public DoppelkopfBid getContraBid() {
		return mContraBid;
	}

	public void setResult(DoppelkopfRoundResult res) {
		if (res == null) {
			throw new IllegalArgumentException("Given DoppelkopfRoundResult is null.");
		}
		mRoundResult = res;
	}
	
	public void setBid(boolean re, int bid) {
		if (re) {
			mReBid = new DoppelkopfBid(bid);
		} else {
			mContraBid = new DoppelkopfBid(bid);
		}
	}
	
	@Override
	public boolean equals(Object other) {
		if (other instanceof DoppelkopfRound) {
			DoppelkopfRound o = (DoppelkopfRound) other;
			return o.mReBid.equals(mReBid) && o.mContraBid.equals(mContraBid) 
					&& o.mExtraScore.equals(mExtraScore) 
					&& o.mRoundResult.equals(mRoundResult)
					&& o.mRoundStyle.equals(mRoundStyle)
					&& o.mRuleSys.equals(mRuleSys);
		} else {
			return super.equals(other);
		}
	}
	
	@Override
	public String toString() {
		return "Re result=" + mRoundResult.toString() + ", ReBid= " + mReBid.toString() + ", ContraBid= " + mContraBid.toString()
				+ "\nreScore=" + mRuleSys.getTotalScore(true, this);
	}

	public DoppelkopfRoundStyle getRoundStyle() {
		return mRoundStyle;
	}
	
	public void setRoundStyle(DoppelkopfRoundStyle style) {
		if (style == null) {
			throw new IllegalArgumentException("Given DoppelkopfRoundStyle is null.");
		}
		mRoundStyle = style;
	}

	public boolean isSolo() {
		return mRoundStyle instanceof DoppelkopfSolo;
	}

	public boolean isPlayerRe(int index) {
		return getRoundStyle().getFirstIndex() == index || getRoundStyle().getSecondIndex() == index;
	}

	public DoppelkopfRound makeCopy() {
		try {
			return new DoppelkopfRound(mRuleSys, new Compacter(this.compact()));
		} catch (CompactedDataCorruptException e) {
			throw new IllegalStateException("Could not copy game round! " + toString() + " data= " + this.compact());
		}
	}


}
