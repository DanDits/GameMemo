package dan.dit.gameMemo.gameData.player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

public final class PlayerColors {
	public static final int DEFAULT_COLOR = 0xFF000000;
	// red, blue, yellow, green, purple, orange, turquoise, lightgreen, pink, bright yellow
	private static final int[] COLORS = new int[] {0xFFFF0000, 0xFF0000FF, 0xFFFFFF00, 0xFF04B404, 
		0xFFBF00FF, 0xFFFF8000, 0xFF01DFD7, 0xFF64FE2E, 0xFFFA58F4, 0xFFF2F5A9}; 
	private static final Random RANDOM = new Random();
	private PlayerColors() {}
	
	public static int get(int index) {
		if (index < COLORS.length) {
			return COLORS[index];
		} else {
			return getRandom();
		}
	}
	
	public static int getRandom() {
		return COLORS[RANDOM.nextInt(COLORS.length)];
	}
	
	public static int getRandom(Collection<Integer> exclude) {
		int possibleRemaining = COLORS.length - exclude.size();
		if (possibleRemaining <= 0) {
			return DEFAULT_COLOR;
		}
		List<Integer> remaining = new ArrayList<Integer>(possibleRemaining);
		for (int col : COLORS) {
			if (exclude == null || !exclude.contains(Integer.valueOf(col))) {
				remaining.add(col);
			}
		}
		if (remaining.size() > 0) {
			return remaining.get(RANDOM.nextInt(remaining.size()));
		} else {
			return DEFAULT_COLOR;
		}
	}
	
	public static final int getNext(Collection<Integer> exclude) {
		for (int col : COLORS) {
			if (exclude == null || !exclude.contains(Integer.valueOf(col))) {
				return col;
			}
		}
		return DEFAULT_COLOR;
	}
}
