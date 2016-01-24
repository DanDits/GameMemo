package dan.dit.gameMemo.gameData.game.binokel;

/**
 * Created by daniel on 23.01.16.
 */
public enum BinokelRoundType {
    TRUMP_EICHEL("Eichel"),
    TRUMPF_BLATT("Blatt"),
    TRUMPF_HERZ("Herz"),
    TRUMPF_SCHELLEN("Schellen"),
    UNTEN_DURCH("UntenDurch", 300),
    DURCH("Durch", 1000);

    public static final int MIN_SPECIAL_GAME_VALUE = 100;
    public static final int MAX_SPECIAL_GAME_VALUE = 3000;
    private int mSpecialRoundDefaultScore = -1;
    private String mKey;

    BinokelRoundType(String key) {
        mKey = key;
    }
    BinokelRoundType(String key, int specialRoundDefaultScore) {
        mKey = key;
        mSpecialRoundDefaultScore = specialRoundDefaultScore;
    }

    public boolean isSpecial() {
        return mSpecialRoundDefaultScore != -1;
    }

    public int getSpecialDefaultScore() {
        return mSpecialRoundDefaultScore;
    }

    public String getKey() {
        return mKey;
    }

    public static BinokelRoundType getFromKey(String key) {
        for (BinokelRoundType type : BinokelRoundType.values()) {
            if (type.getKey().equals(key)) {
                return type;
            }
        }
        return null;
    }
}
