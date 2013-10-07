package dan.dit.gameMemo.gameData.statistics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import dan.dit.gameMemo.R;
import dan.dit.gameMemo.appCore.gameSetup.TeamSetupTeamsController;
import dan.dit.gameMemo.gameData.game.Game;
import dan.dit.gameMemo.gameData.game.GameRound;

public abstract class GameStatisticAttributeManager {
    private int mGameKey;
    private Map<String, StatisticAttribute> mAttrs = new HashMap<String, StatisticAttribute>();
    private Map<String, GameStatistic> mStats = new HashMap<String, GameStatistic>();
    private Map<String, LinkedList<String>> mMissingSubAttrs;
    
    public GameStatisticAttributeManager(int gameKey) {
        mGameKey = gameKey;
    }
    
    /**
     * This should always be invoked by a GameStatistic's acceptGame method to check if all teams are contained
     * in the given game. The teams are set to filter games with these combinations. StatisticAttributes can but are not required
     * to check this as they only filter out statistics more.
     * @param pGame The game to check.
     * @param data The attribute which's teams must be contained in the given game. An empty or non existing team is considered to be contained.
     * @return <code>true</code> if all teams are completely or partly contained in the teams of the given game.
     */
    protected abstract boolean containsAllTeams(Game pGame, AttributeData data);
    
    protected static final boolean hasAttribute(AttributeData data, String identifier) {
        for (StatisticAttribute attr : data.mAttributes) {
            if (attr.getIdentifier().equals(identifier)) {
                return true;
            }
        }
        return false;
    }
    
    public boolean delete(String identifier, SQLiteDatabase db) {
        StatisticAttribute toDelete = getAttribute(identifier);
        // do not check for id here, this is only required for database deletion
        if (toDelete != null && toDelete.isUserAttribute()) { 
            List<StatisticAttribute> deletionList = new LinkedList<StatisticAttribute>();
            for (StatisticAttribute attr : getAllAttributes()) {
                attr.removeAttribute(toDelete);
                StatisticAttribute baseAttr = null;
                if (attr instanceof StatisticAttribute.WrappedStatisticAttribute) {
                    baseAttr = ((StatisticAttribute.WrappedStatisticAttribute) attr).mBaseAttr;
                } else if (attr instanceof GameStatistic.WrappedGameStatistic) {
                    baseAttr = ((GameStatistic.WrappedGameStatistic) attr).mBaseStat;
                }
                if (baseAttr != null && baseAttr.equals(toDelete)) {
                    deletionList.add(attr);
                }
            }
            if (toDelete.hasId()) {
                toDelete.delete(db);
            }
            mAttrs.remove(identifier);
            mStats.remove(identifier);
            for (StatisticAttribute attr : deletionList) {
                delete(attr.mIdentifier, db);
            }
            return true;
        }
        return false;
    }
    
    public void saveUserAttributes(SQLiteDatabase db) {
        for (StatisticAttribute attr : getAllAttributes()) {
            if (attr.isUserAttribute()) {
                attr.save(db);
            }
        }
    }
    
    public void init(Context applicationContext) {
        StatisticsDbHelper.makeInstance(applicationContext);
        mMissingSubAttrs = new HashMap<String, LinkedList<String>>();
        mAttrs.clear();
        mStats.clear();
        addPredefinedAttributes();
        loadUserAttributes();
        handleMissingSubAttributes();
    }
    
    private void addPredefinedAttributes() {
        Collection<StatisticAttribute> attrs = createPredefinedAttributes();;
        for (StatisticAttribute attr : attrs) {
            putAttributeInMap(attr);
        }
    }
    
    protected abstract Collection<StatisticAttribute> createPredefinedAttributes();

    private void loadUserAttributes() {
        StatisticsDbHelper dbHelper = StatisticsDbHelper.getInstance();
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        Cursor data = db.query(StatisticsContract.Attribute.TABLE_NAME, StatisticsContract.Attribute.COLUMNS_ALL,
                StatisticsContract.Attribute.COLUMN_NAME_GAMEKEY + " = " + mGameKey, null, null, null, null);
        LinkedList<Integer> positionsToLoad = new LinkedList<Integer>(); // positions in the cursor that could not be loaded because of missing dependencies
        if (data.moveToFirst()) {
            // O(n), I would need to load the sub attribute column anyways for all to analyze the best loading order
            do {
                StatisticAttribute attr = StatisticAttribute.load(data, this);
                if (attr == null) {
                    positionsToLoad.add(data.getPosition());
                } else {
                    Log.d("GameMemo", "Loaded user attribute (first try)" + attr);
                    putAttributeInMap(attr);
                }
                data.moveToNext();
            } while (!data.isAfterLast());
            while (!positionsToLoad.isEmpty()) {
                Integer curr = positionsToLoad.getFirst();
                data.moveToPosition(curr);
                StatisticAttribute attr = StatisticAttribute.load(data, this);
                if (attr != null) {
                    Log.d("GameMemo", "Loaded user attribute (second try)" + attr);
                    putAttributeInMap(attr);
                    positionsToLoad.remove(curr);
                } else {
                    throw new IllegalStateException("Could not load attribute on second try, infinite loop detected for " + positionsToLoad + " in " + data);
                }
            }
        }
    }
    
    public void putAttributeInMap(StatisticAttribute attr) {
        if (attr == null || attr.mGameKey != mGameKey) {
            throw new IllegalArgumentException("Cannot add " + attr + " to this manager.");
        }
        if (attr instanceof GameStatistic) {
            mStats.put(attr.mIdentifier, (GameStatistic) attr);
        } else {
            mAttrs.put(attr.mIdentifier, attr);
        }
    }
    
    public List<StatisticAttribute> getAllAttributes() {
        ArrayList<StatisticAttribute> all = new ArrayList<StatisticAttribute>(mStats.size() + mAttrs.size());
        all.addAll(mStats.values());
        all.addAll(mAttrs.values());
        Collections.sort(all);
        return all;
    }
    
    public List<GameStatistic> getStatistics() {
        ArrayList<GameStatistic> all = new ArrayList<GameStatistic>(mStats.values());
        Collections.sort(all);
        return all;
    }
    
    public StatisticAttribute getAttribute(String identifier) {
        if (identifier == null) {
            return null;
        }
        StatisticAttribute attr = mAttrs.get(identifier);
        if (attr == null) {
            return mStats.get(identifier);
        }
        return attr;
    }
    
    public GameStatistic getStatistic(String identifier) {
        if (identifier == null) {
            return null;
        }
        return mStats.get(identifier);
    }

    public void addMissingSubAttribute(StatisticAttribute attr, String subAttributeIdentifier) {
        LinkedList<String> missingList = mMissingSubAttrs.get(attr.mIdentifier);
        if (missingList == null) {
            missingList = new LinkedList<String>();
            mMissingSubAttrs.put(attr.mIdentifier, missingList);
        }
        missingList.add(subAttributeIdentifier);
    }
    
    private void handleMissingSubAttributes() {
        if (mMissingSubAttrs == null) {
            throw new IllegalStateException("Missing sub attributes already handled.");
        }
        for (String identifier : mMissingSubAttrs.keySet()) {
            for (String missingIdentifier : mMissingSubAttrs.get(identifier)) {
                getAttribute(identifier).addAttribute(getAttribute(missingIdentifier));
            }
        }
        mMissingSubAttrs = null;
    }
    
    protected static void addAndCheck(StatisticAttribute attr, Map<String, StatisticAttribute> attrs) {
        if (attrs.containsKey(attr.getIdentifier())) {
            throw new IllegalArgumentException("Attribute already contained! Wrong identifier: " + attr  + " all: " + attrs);
        }
        attrs.put(attr.getIdentifier(), attr);
    }

    public static final String IDENTIFIER_ATT_INVERT_RESULT = "invert_result";
    public static final String IDENTIFIER_STAT_LONGEST_TIME_BETWEEN_GAMES = "longest_time_between_games";
    
    protected Collection<StatisticAttribute> getGeneralPredefinedAttributes() {
        Map<String, StatisticAttribute> attrs = new HashMap<String, StatisticAttribute>();
        GameStatistic.Builder statBuilder;
        StatisticAttribute.Builder attBuilder;
        StatisticAttribute att;
        GameStatistic stat = new GameStatistic() {
            private long mLastGameStarttime;
            private double mLastGameValue;
            @Override public void initCalculation() {mLastGameStarttime = -1; mLastGameValue = 0;}
            @Override public boolean acceptGame(Game game, AttributeData data) {return acceptGameAllSubattributes(game) && containsAllTeams(game, data);}
            @Override public boolean acceptRound(Game game, GameRound round, AttributeData data) { return false;}

            @Override
            public double calculateValue(Game game, GameRound rounde, AttributeData data) {return Double.NaN;}
            
            @Override
            public double calculateValue(Game game, AttributeData data) {
                if (mLastGameStarttime == -1) {
                    mLastGameValue  = 0.0;
                    mLastGameStarttime = game.getStartTime();
                    return 0;
                } else {
                    double currDiff = game.getStartTime() - mLastGameStarttime;
                    mLastGameStarttime = game.getStartTime();
                    if (currDiff > mLastGameValue) {
                        double old = mLastGameValue;
                        mLastGameValue = currDiff; //maximize the difference
                        return (currDiff - old) / (1000 * 60 * 60); // in hours
                    }
                    return 0;
                }
            }

        };
        statBuilder = new GameStatistic.Builder(stat, IDENTIFIER_STAT_LONGEST_TIME_BETWEEN_GAMES, mGameKey);
        statBuilder.setNameAndDescriptionResId(R.string.statistic_general_longest_time_between_games_name, R.string.statistic_general_longest_time_between_games_descr);
        statBuilder.setPriority(10);
        statBuilder.setPresentationType(GameStatistic.PRESENTATION_TYPE_ABSOLUTE);
        addAndCheck(statBuilder.getAttribute(), attrs);
        
        // invert result
        att = new UserStatisticAttribute() {
            @Override
            public boolean acceptGame(Game game, AttributeData data) {
               return !super.acceptGame(game, data);
            }
            
            @Override
            public boolean acceptRound(Game game, GameRound round, AttributeData data) {
                return !super.acceptRound(game, round, data);
            }
        };
        attBuilder = new StatisticAttribute.Builder(att, IDENTIFIER_ATT_INVERT_RESULT, mGameKey);
        attBuilder.setPriority(100);
        attBuilder.setNameAndDescriptionResId(R.string.attribute_general_invert_name, R.string.attribute_general_invert_descr);
        addAndCheck(attBuilder.getAttribute(), attrs);
        return attrs.values();
    }

    public abstract Bundle applyMode(int mode, TeamSetupTeamsController ctr, Resources res);

    public void setHighestPriority(String identifier) {
        StatisticAttribute att = getAttribute(identifier);
        if (att != null) {
            int currPrio = 1;
            List<StatisticAttribute> all = getAllAttributes();
            Collections.sort(all);
            for (StatisticAttribute sa : all) {
                if (sa.isUserAttribute()) {
                    if (sa.equals(att)) {
                        sa.setPriority(0); // 0 is highest priority
                    } else {
                        sa.setPriority(currPrio++);
                    }
                }
            }
        }
    }
    
}
