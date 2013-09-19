package dan.dit.gameMemo.gameData.player;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.res.Resources;
import android.util.Log;
import dan.dit.gameMemo.R;

public class DummyPlayer extends Player {
	public static final int FIRST_NUMBER = 1;
    private int mNumber;
	
	public DummyPlayer(int number) {
		super(String.format(mDummyBaseName, number));
		mNumber = number;
	}
	
	public int getNumber() {
	    return mNumber;
	}
	
	@Override
	public String getShortenedName(int maxLength) {
	    String usualShortened = super.getShortenedName(maxLength);
	    if (!usualShortened.equals(getName())) {
	        return "#" + String.valueOf(mNumber);
	    }
	    return usualShortened;
	}
	
    private static String mDummyBaseName = "Player %1$d";
    private static Pattern mDummyNamePattern = Pattern.compile("Player ([0-9]+)");
    
    public static void initNames(Resources res) {
        mDummyBaseName = res.getString(R.string.player_dummy_name);
        mDummyNamePattern = Pattern.compile(res.getString(R.string.player_dummy_name_regex));
    }
    
    public static boolean isDummyName(String playerName) {
        Matcher m = mDummyNamePattern.matcher(playerName);
        return m.matches();
    }

    public static int extractNumber(String name) {
        Matcher m = mDummyNamePattern.matcher(name);
        if (m.matches()) {
            String number = m.group(1);
            if (number != null) {
                int n;
                try {
                    n = Integer.parseInt(number);
                } catch (NumberFormatException nfe) {
                    Log.d("GameMemo", "Error extracting number from name " + name + " got group " + number);
                    return -1;
                }
                return n;
            }
        }
        return -1;
    }
}
