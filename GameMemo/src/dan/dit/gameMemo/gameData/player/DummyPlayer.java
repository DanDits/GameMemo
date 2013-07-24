package dan.dit.gameMemo.gameData.player;

import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.util.Log;
import android.util.SparseArray;

public class DummyPlayer extends Player {
	private int mNumber;
	private DummyPool mPool;
	private DummyPlayer(DummyPool pool, String baseName, int number) {
		super(String.format(baseName, number));
		mPool = pool;
		if (mPool == null) {
			throw new IllegalArgumentException("DummyPool is null.");
		}
		mNumber = number;
	}
	
	public void release() {
		mPool.releaseDummy(this);
	}
	
	public void obtain() {
		mPool.obtain(this);
	}
	
	public static class DummyPool {
		private static final int FIRST_FREE_NUMBER = 1;
		private Set<Integer> mUsedNumbers = new TreeSet<Integer>();
		private SparseArray<DummyPlayer> mRecycle = new SparseArray<DummyPlayer>();
		 // since we need a context to get a string resource this is here to offer a default, but always call setDummyBaseName
		private String mDummyBaseName = "Player %1$d";
		private Pattern mDummyNamePattern = Pattern.compile("Player ([0-9]+)");
		
		public void setDummyBaseName(String dummyBaseName, String dummyNameRegex) {
			mRecycle.clear();
			mDummyBaseName = dummyBaseName;
			mDummyNamePattern = Pattern.compile(dummyNameRegex);
		}
		
		private void releaseDummy(DummyPlayer dummyPlayer) {
			Log.d("Tichu", "Releasing dummy " + dummyPlayer);
			mUsedNumbers.remove(dummyPlayer.mNumber);
		}
		
		private DummyPlayer getOrMakeObtainedDummy(int number) {
			DummyPlayer obtainedPlayer = mRecycle.get(number);
			if (obtainedPlayer == null) {
				obtainedPlayer = new DummyPlayer(this, mDummyBaseName, number);
				obtainedPlayer.setColor(PlayerColors.get(number));
			}
			obtain(obtainedPlayer);
			return obtainedPlayer;
		}
		
		public DummyPlayer obtainNewDummy() {
			int freeNumber = FIRST_FREE_NUMBER;
			for (int number : mUsedNumbers) {
				if (number != freeNumber) {
					break;
				} else {
					freeNumber = number + 1;
				}
			}
			assert !mUsedNumbers.contains(Integer.valueOf(freeNumber));
			return getOrMakeObtainedDummy(freeNumber);
		}
		
		public DummyPlayer obtainDummy(String playerName) {
			Matcher m = mDummyNamePattern.matcher(playerName);
			if (m.matches()) {
				int number = FIRST_FREE_NUMBER - 1;
				for (int i = 0; i < m.groupCount(); i++) {
					String groupMatch = m.group(i + 1);
					if (groupMatch != null) {
						try {
							number = Integer.parseInt(groupMatch);					
						} catch (NumberFormatException nfe) {
							return null;
						}
					}
				}
				if (number >= FIRST_FREE_NUMBER) {
					return getOrMakeObtainedDummy(number);
				}
			}
			return null;
		}
		
		public DummyPlayer obtainDummy(Player player) {
			if (player instanceof DummyPlayer) {
				obtain((DummyPlayer) player);
				return (DummyPlayer) player;
			} else {
				return obtainDummy(player.getName());
			}
		}
		
		private void obtain(DummyPlayer toObtain) {
			Log.d("Tichu", "Obtaining dummy " + toObtain);
			mRecycle.put(toObtain.mNumber, toObtain);
			mUsedNumbers.add(Integer.valueOf(toObtain.mNumber));			
		}
	}
}
