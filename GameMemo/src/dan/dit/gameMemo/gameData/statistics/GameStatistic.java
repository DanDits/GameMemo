package dan.dit.gameMemo.gameData.statistics;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.content.ContentValues;
import android.text.TextUtils;
import dan.dit.gameMemo.R;
import dan.dit.gameMemo.gameData.game.Game;
import dan.dit.gameMemo.gameData.game.GameKey;
import dan.dit.gameMemo.gameData.game.GameRound;
import dan.dit.gameMemo.gameData.player.AbstractPlayerTeam;

public abstract class GameStatistic extends StatisticAttribute {
    public static final int PRESENTATION_TYPE_ABSOLUTE = 0;
    public static final int PRESENTATION_TYPE_PERCENTAGE = 1;
    public static final int PRESENTATION_TYPE_PROPORTION = 2;
    public static final int PRESENTATION_TYPES_COUNT = 3;
    
    protected int mPresentationType = PRESENTATION_TYPE_ABSOLUTE;
    protected int mIsPercentual = 0; // 0 not set, 1 set to percentage type, 2 set to proportion type
    protected String mReferenceStatisticIdentifier;
    protected List<Game> mAllGames;
    
    public final static GameStatistic NO_STATISTIC = new GameStatistic() {

        @Override
        protected double calculateValue(Game game, AttributeData data) {return Double.NaN;}

        @Override
        protected double calculateValue(Game game, GameRound round,
                AttributeData data) {return Double.NaN;}

        @Override
        public boolean acceptGame(Game game, AttributeData data) {return false; }

        @Override
        public boolean acceptRound(Game game, GameRound round,
                AttributeData data) {return false;}
        
    };
    static {
        NO_STATISTIC.mIdentifier = "~~~NO_STATISTIC_HAHAHAHAHA~~~";
    }
    
    // fields required for user made statistics
    public static final int ATTRIBUTE_TYPE_USER_STATISTIC = 1;
    
    /*
     * Thats a kind of ugly class, due to java not allowing multiple inheritance and since this
     * is required to be a GameStatistic, this can't extend the wrapper class of StatisticAttribute
     * and method implementations are duplicates.
     * @author Daniel
     *
     */
    protected static class WrappedGameStatistic extends GameStatistic {
        protected GameStatistic mBaseStat;
        private WrappedGameStatistic(GameStatistic baseStat) {
            mBaseStat = baseStat;
        }
        
        @Override
        public void initCalculation() {
            mBaseStat.initCalculation();
        }
        
        @Override
        public boolean isPercentual() {
            if (mBaseStat.mIsPercentual == 0) {
                return mBaseStat.isPercentual();
            } else {
                return super.isPercentual();
            }
        }
        
        @Override
        public boolean requiresCustomValue() {
            return mBaseStat.requiresCustomValue();
        }
        @Override
        protected void addSaveData(ContentValues val) {
            super.addSaveData(val);
            val.put(StatisticsContract.Attribute.COLUMN_NAME_BASE_ATTRIBUTE, mBaseStat.mIdentifier);
        }
        
        @Override
        public double calculateValue(Game game, AttributeData data) {
            double result = mBaseStat.calculateValue(game, data);
            return result;
        }
        @Override
        public double calculateValue(Game game, GameRound round, AttributeData data) {
            double result = mBaseStat.calculateValue(game, round, data);
            return result;
        }
        @Override
        public boolean acceptGame(Game game, AttributeData data) {
            boolean result = mBaseStat.acceptGame(game, data);
            return result;
        }
        @Override
        public boolean acceptRound(Game game, GameRound round, AttributeData data) {          
            boolean result = mBaseStat.acceptRound(game, round, data);
            return result;
        }
        
        private AttributeData mDataCombined = new AttributeData();
        @Override
        protected AttributeData getData() {
            mDataCombined.mAttributes = getAttributes();
            mDataCombined.mCustomValue = mData.mCustomValue;
            mDataCombined.mTeams = mData.mTeams;
            return mDataCombined;
        }       
        
        @Override
        public StatisticAttribute getBaseAttribute() {
            return mBaseStat;
        }
        
        
        @Override
        public Set<StatisticAttribute> getAttributes() {
            Set<StatisticAttribute> own = super.getAttributes();
            Set<StatisticAttribute> base = mBaseStat.getAttributes();
            if (base.size() == 0) {
                return own;
            } else if (own.size() == 0) {
                return base;
            } else {
                Set<StatisticAttribute> combined = new HashSet<StatisticAttribute>(own);
                combined.addAll(base);
                return combined;
            }
        }
        
        @Override
        public void setTeams(List<AbstractPlayerTeam> teams) {
            super.setTeams(teams);
            mBaseStat.setTeams(teams);
        }
        
        @Override
        public String toString() {
            return super.toString() + " wrapping " + mBaseStat.mIdentifier;
        }
    }
    
    public static class Builder extends StatisticAttribute.Builder {
        private GameStatistic mStat;
        public Builder(GameStatistic stat, String identifier, int gameKey) {
            super(stat, identifier, gameKey);
            mStat = stat;
        }

        public Builder(String identifier, GameStatistic baseStat) {
            mStat = new WrappedGameStatistic(baseStat);
            super.mAttr = mStat;
            if (TextUtils.isEmpty(identifier)) {
                identifier = GameKey.getGameStatisticAttributeManager(baseStat.mGameKey).getUnusedIdentifier(baseStat);
            }
            mStat.mIdentifier = identifier;
            if (TextUtils.isEmpty(identifier) || baseStat == null) {
                throw new IllegalArgumentException("Identifier or base statistic invalid: " + identifier + " / " + baseStat);
            }
            mStat.mGameKey = baseStat.mGameKey;
            setPresentationType(baseStat.mPresentationType);
            setReferenceStatisticIdentifier(baseStat.mReferenceStatisticIdentifier);
            setNameAndDescription(baseStat.mName, baseStat.mDescription);
            setNameAndDescriptionResId(baseStat.mNameResId, baseStat.mDescriptionResId);
            setPriority(baseStat.mPriority);
        }
        public void setPresentationType(int presType) {
            mStat.setPresentationType(presType);
        }
        
        public GameStatistic getStatistic() {
            return mStat;
        }

        public void setReferenceStatisticIdentifier(String identifier) {
            mStat.mReferenceStatisticIdentifier = identifier;
        }
    }
    /**
     * Calculates the statistic value of the given game. Undefined result
     * if the game is not accepted by the statistic.
     * @param game The game to analyze.
     * @return A value describing the statistic of the given game.
     */
    protected abstract double calculateValue(Game game, AttributeData data);
    
    public final double calculateValue(Game game) {
        return calculateValue(game, getData());
    }
    
    /**
     * Calculates the statistic value of the given game round. Undefined result
     * if the game is not accepted by the statistic.
     * @param game The game the round belongs to.
     * @param round The round to analyze.
     * @return A value describing the statistic of the given round.
     */
    protected abstract double calculateValue(Game game, GameRound round, AttributeData data);
    
    public final double calculateValue(Game game, GameRound round) {
        return calculateValue(game, round, getData());
    }
    
    public AcceptorIterator iterator() {
        if (mAllGames == null) {
            throw new IllegalStateException("No games set for statistic " + toString());
        }
        return new AcceptorIterator(this);
    }
    
    public void setGameList(List<Game> games) {
        mAllGames = games;
    }
    
    public void setPresentationType(int type) {
        mPresentationType = type;
        if (mPresentationType == PRESENTATION_TYPE_PERCENTAGE) {
            mIsPercentual = PRESENTATION_TYPE_PERCENTAGE;
        } else if (mPresentationType == PRESENTATION_TYPE_PROPORTION) {
            mIsPercentual = PRESENTATION_TYPE_PROPORTION;
        }
    }
    
    public int nextPresentationType() {
        switch (mPresentationType) {
        case PRESENTATION_TYPE_ABSOLUTE:
            if (mIsPercentual == PRESENTATION_TYPE_PERCENTAGE) {
                return PRESENTATION_TYPE_PERCENTAGE;
            } else {
                return PRESENTATION_TYPE_PROPORTION;
            }
        case PRESENTATION_TYPE_PROPORTION:
        case PRESENTATION_TYPE_PERCENTAGE:
            return PRESENTATION_TYPE_ABSOLUTE;
        default:
            throw new IllegalStateException("Illegal presentation type: " + mPresentationType);
        }
    }
    
    public boolean isPercentual() {
        return mIsPercentual == PRESENTATION_TYPE_PERCENTAGE;
    }
    
    public int getPresentationType() {
        return mPresentationType;
    }
        
    @Override
    public String toString() {
        Set<StatisticAttribute> subAttrs = getAttributes();
        if (subAttrs.size() > 0) {
            return "Statistic " + mIdentifier + " (own: " + mData.mAttributes + ", all: " + subAttrs + ")";
        } else {
            return "Statistic " + mIdentifier;
        }
    }
    
    // methods required for user statistics
    
    @Override
    protected void addSaveData(ContentValues val) {
        val.put(StatisticsContract.Attribute.COLUM_NAME_ATTRIBUTE_TYPE, ATTRIBUTE_TYPE_USER_STATISTIC);
        val.put(StatisticsContract.Attribute.COLUMN_NAME_PRESENTATION_TYPE, mPresentationType);
        val.put(StatisticsContract.Attribute.COLUMN_NAME_REFERENCE_STATISTIC, mReferenceStatisticIdentifier);
    }

    public int getGameKey() {
        return mGameKey;
    }

    public void initCalculation() {
    }

    public String getReference() {
        return mReferenceStatisticIdentifier;
    }
    
    private static final NumberFormat FORMAT_ABS = new DecimalFormat("#.##");
    private static final NumberFormat FORMAT_PERC = new DecimalFormat("#.###'%'");
    private static final NumberFormat FORMAT_PROP = new DecimalFormat("#.###'÷'");

    public final NumberFormat getFormat() {
        switch (mPresentationType) {
        case PRESENTATION_TYPE_ABSOLUTE:
            return FORMAT_ABS;
        case PRESENTATION_TYPE_PROPORTION:
            return FORMAT_PROP;
        case PRESENTATION_TYPE_PERCENTAGE:
            return FORMAT_PERC;
        default:
            throw new IllegalStateException("Illegal presentation type " + mPresentationType);
        }
    }

    public static int getPresTypeTextResId(int presentationType) {
        switch (presentationType) {
        case GameStatistic.PRESENTATION_TYPE_ABSOLUTE:
            return R.string.statistics_menu_pres_type_absolute;
        case GameStatistic.PRESENTATION_TYPE_PERCENTAGE:
            return R.string.statistics_menu_pres_type_percentual;
        case GameStatistic.PRESENTATION_TYPE_PROPORTION:
            return R.string.statistics_menu_pres_type_proportional;
        }
        return 0;
    }
    
    public static int getPresTypeDrawableResId(int presentationType) {
        switch (presentationType) {
        case GameStatistic.PRESENTATION_TYPE_ABSOLUTE:
            return R.drawable.stat_pres_type_abs;
        case GameStatistic.PRESENTATION_TYPE_PERCENTAGE:
            return R.drawable.stat_pres_type_perc;
        case GameStatistic.PRESENTATION_TYPE_PROPORTION:
            return R.drawable.stat_pres_type_prop;
        }
        return 0;
    }

    public void setReferenceStatistic(GameStatistic referenceStat) {
        if (referenceStat == null || referenceStat.equals(NO_STATISTIC)) {
            mReferenceStatisticIdentifier = null;
        } else {
            mReferenceStatisticIdentifier = referenceStat.getIdentifier();
        }
    }
}
