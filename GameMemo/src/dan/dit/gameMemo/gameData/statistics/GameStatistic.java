package dan.dit.gameMemo.gameData.statistics;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.content.ContentValues;
import android.text.TextUtils;
import dan.dit.gameMemo.gameData.game.Game;
import dan.dit.gameMemo.gameData.game.GameRound;
import dan.dit.gameMemo.gameData.player.AbstractPlayerTeam;

public abstract class GameStatistic extends StatisticAttribute {
    public static final int PRESENTATION_TYPE_ABSOLUTE = 0;
    public static final int PRESENTATION_TYPE_PERCENTAGE = 1;
    public static final int PRESENTATION_TYPE_PROPORTION = 2;
    public static final int PRESENTATION_TYPE_COUNT = 3;
    
    protected int mPresentationType = PRESENTATION_TYPE_ABSOLUTE;
    protected String mReferenceStatisticIdentifier;
    protected List<Game> mAllGames;
    
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
        
        @Override
        protected AttributeData getData() {
            AttributeData data = new AttributeData();
            data.mAttributes = getAttributes();
            data.mCustomValue = mData.mCustomValue;
            data.mTeams = mData.mTeams;
            return data;
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
            mStat.mIdentifier = identifier;
            if (TextUtils.isEmpty(identifier) || baseStat == null) {
                throw new IllegalArgumentException("Identifier or base statistic invalid: " + identifier + " / " + baseStat);
            }
            setPresentationType(baseStat.mPresentationType);
            mStat.mGameKey = baseStat.mGameKey;
        }
        public void setPresentationType(int presType) {
            mStat.mPresentationType = presType;
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

}
