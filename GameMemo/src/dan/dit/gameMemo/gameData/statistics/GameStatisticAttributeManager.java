package dan.dit.gameMemo.gameData.statistics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.TextUtils;
import dan.dit.gameMemo.R;
import dan.dit.gameMemo.gameData.game.Game;
import dan.dit.gameMemo.gameData.game.GameKey;
import dan.dit.gameMemo.gameData.game.GameRound;
import dan.dit.gameMemo.gameData.player.AbstractPlayerTeam;
import dan.dit.gameMemo.gameData.player.PlayerPool;
import dan.dit.gameMemo.gameData.player.PlayerTeam;

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
            for (StatisticAttribute attr : getAllAttributes(true)) {
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
        for (StatisticAttribute attr : getAllAttributes(true)) {
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
                    putAttributeInMap(attr);
                }
                data.moveToNext();
            } while (!data.isAfterLast());
            while (!positionsToLoad.isEmpty()) {
                Integer curr = positionsToLoad.getFirst();
                data.moveToPosition(curr);
                StatisticAttribute attr = StatisticAttribute.load(data, this);
                if (attr != null) {
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
    
    public List<StatisticAttribute> getAllAttributes(boolean includeHidden) {
        ArrayList<StatisticAttribute> all = new ArrayList<StatisticAttribute>(mStats.size() + mAttrs.size());
        all.addAll(mStats.values());
        all.addAll(mAttrs.values());
        if (!includeHidden) {
            Iterator<StatisticAttribute> it = all.iterator();
            while (it.hasNext()) {
                if (it.next().getPriority() == StatisticAttribute.PRIORITY_HIDDEN) {
                    it.remove();
                }
            }
        }
        Collections.sort(all);
        return all;
    }
    
    public List<GameStatistic> getStatistics(boolean includeHidden) {
        ArrayList<GameStatistic> all = new ArrayList<GameStatistic>(mStats.values());
        Collections.sort(all);
        if (!includeHidden) {
            Iterator<GameStatistic> it = all.iterator();
            while (it.hasNext()) {
                if (it.next().getPriority() == StatisticAttribute.PRIORITY_HIDDEN) {
                    it.remove();
                }
            }
        }
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
    
    protected static void addAndCheck(StatisticAttribute attr, Map<String, StatisticAttribute> attrs, boolean copyAttribute) {
        if (attrs.containsKey(attr.getIdentifier())) {
            throw new IllegalArgumentException("Attribute already contained! Wrong identifier: " + attr  + " all: " + attrs);
        }
        int prio = attr.getPriority();
        attrs.put(attr.getIdentifier(), attr);
        if (copyAttribute && prio != StatisticAttribute.PRIORITY_HIDDEN) {
            attr.setPriority(StatisticAttribute.PRIORITY_HIDDEN);
            if (attr instanceof GameStatistic) {
                GameStatistic.Builder copy = new GameStatistic.Builder(null, (GameStatistic) attr);
                copy.setUserCreated();
                copy.setPriority(prio);
                StatisticAttribute copyAttr = copy.getAttribute();
                attrs.put(copyAttr.getIdentifier(), copyAttr);
            } else {
                StatisticAttribute.Builder copy = new StatisticAttribute.Builder(null, attr);
                copy.setUserCreated();
                copy.setPriority(prio);
                StatisticAttribute copyAttr = copy.getAttribute();
                attrs.put(copyAttr.getIdentifier(), copyAttr);
            }
        }
    }

    public static final String IDENTIFIER_ATT_INVERT_RESULT = "invert_result";
    public static final String IDENTIFIER_STAT_LONGEST_TIME_BETWEEN_GAMES = "longest_time_between_games";
    public static final String IDENTIFIER_STAT_GAME_LENGTH = "game_length";
    public static final String IDENTIFIER_STAT_AND_SUMMER = "stat_and_summer";
    public static final String IDENTIFIER_STAT_OR_SUMMER = "stat_or_summer";
    public static final String IDENTIFIER_ATT_OR = "or";
    public static final String IDENTIFIER_ATT_GAME_FINISHED = "game_finished";
    public static final String IDENTIFIER_ATT_CONTAINS_TEAM= "contains_team";
    
    protected Collection<StatisticAttribute> getGeneralPredefinedAttributes() {
        Map<String, StatisticAttribute> attrs = new HashMap<String, StatisticAttribute>();
        GameStatistic.Builder statBuilder;
        StatisticAttribute.Builder attBuilder;
        StatisticAttribute att;
        GameStatistic stat = new GameStatistic() {
            private long mLastGameStarttime;
            private double mLastGameValue;
            @Override public void initCalculation() {mLastGameStarttime = -1; mLastGameValue = 0;}
            @Override public boolean acceptGame(Game game, AttributeData data) {return acceptGameAllSubattributes(game, data) && containsAllTeams(game, data);}
            @Override public boolean acceptRound(Game game, GameRound round, AttributeData data) { return false;}

            @Override
            public double calculateValue(Game game, GameRound round, AttributeData data) {return Double.NaN;}
            
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
        statBuilder.setPriority(StatisticAttribute.PRIORITY_NONE);
        statBuilder.setPresentationType(GameStatistic.PRESENTATION_TYPE_ABSOLUTE);
        addAndCheck(statBuilder.getAttribute(), attrs, false);
        
        // game length
        stat = new GameStatistic() {
            @Override public boolean acceptGame(Game game, AttributeData data) {return acceptGameAllSubattributes(game, data) && containsAllTeams(game, data);}
            @Override public boolean acceptRound(Game game, GameRound round, AttributeData data) { return false;}

            @Override
            public double calculateValue(Game game, GameRound round, AttributeData data) {return Double.NaN;}
            
            @Override
            public double calculateValue(Game game, AttributeData data) {
                return game.getRunningTime() / (1000 * 60);
            }

        };
        statBuilder = new GameStatistic.Builder(stat, IDENTIFIER_STAT_GAME_LENGTH, mGameKey);
        statBuilder.setNameAndDescriptionResId(R.string.statistic_general_game_runtime_name, R.string.statistic_general_game_runtime_descr);
        statBuilder.setPriority(StatisticAttribute.PRIORITY_NONE);
        statBuilder.setPresentationType(GameStatistic.PRESENTATION_TYPE_PROPORTION);
        addAndCheck(statBuilder.getAttribute(), attrs, false);
        
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
        attBuilder.setPriority(StatisticAttribute.PRIORITY_NONE);
        attBuilder.setNameAndDescriptionResId(R.string.attribute_general_invert_name, R.string.attribute_general_invert_descr);
        addAndCheck(attBuilder.getAttribute(), attrs, false);
       
        // logical or
        att = new UserStatisticAttribute() {
            @Override
            public boolean acceptGame(Game game, AttributeData data) {
               return acceptGameOneSubattributes(game, data);
            }
            
            @Override
            public boolean acceptRound(Game game, GameRound round, AttributeData data) {
                return acceptRoundOneSubattributes(game, round, data);
            }
        };
        attBuilder = new StatisticAttribute.Builder(att, IDENTIFIER_ATT_OR, mGameKey);
        attBuilder.setPriority(StatisticAttribute.PRIORITY_NONE);
        attBuilder.setNameAndDescriptionResId(R.string.attribute_general_or_name, R.string.attribute_general_or_descr);
        addAndCheck(attBuilder.getAttribute(), attrs, false);
       
        // game finished
        att = new UserStatisticAttribute() {
            @Override
            public boolean acceptGame(Game game, AttributeData data) {
               return super.acceptGame(game, data) && containsAllTeams(game, data) && game.isFinished();
            }
        };
        attBuilder = new StatisticAttribute.Builder(att, IDENTIFIER_ATT_GAME_FINISHED, mGameKey);
        attBuilder.setPriority(StatisticAttribute.PRIORITY_NONE);
        attBuilder.setNameAndDescriptionResId(R.string.attribute_general_game_finished_name, R.string.attribute_general_game_finished_descr);
        addAndCheck(attBuilder.getAttribute(), attrs, false);
        
        // stat and summer
        stat = new GameStatistic() {
            @Override public boolean acceptGame(Game game, AttributeData data) {return acceptGameAllSubattributes(game, data) && containsAllTeams(game, data);}
            @Override public boolean acceptRound(Game game, GameRound round, AttributeData data) { return acceptRoundAllSubattributes(game, round, data);}

            @Override
            public double calculateValue(Game game, GameRound round, AttributeData data) {
                double sum = 0;
                for (StatisticAttribute attr : data.mAttributes) {
                    if (attr instanceof GameStatistic) {
                        sum += ((GameStatistic) attr).calculateValue(game, round, attr.getData());
                    }
                }
                return sum;
            }
            
            @Override
            public double calculateValue(Game game, AttributeData data) {
                double sum = 0;
                for (StatisticAttribute attr : data.mAttributes) {
                    if (attr instanceof GameStatistic) {
                        sum += ((GameStatistic) attr).calculateValue(game, attr.getData());
                    }
                }
                return sum;
            }

        };
        statBuilder = new GameStatistic.Builder(stat, IDENTIFIER_STAT_AND_SUMMER, mGameKey);
        statBuilder.setNameAndDescriptionResId(R.string.statistic_general_summer_and_name, R.string.statistic_general_summer_and_descr);
        statBuilder.setPriority(StatisticAttribute.PRIORITY_HIDDEN);
        statBuilder.setPresentationType(GameStatistic.PRESENTATION_TYPE_ABSOLUTE);
        addAndCheck(statBuilder.getAttribute(), attrs, false);
        
        // stat or summer
        stat = new GameStatistic() {
            @Override public boolean acceptGame(Game game, AttributeData data) {return acceptGameOneSubattributes(game, data) && containsAllTeams(game, data);}
            @Override public boolean acceptRound(Game game, GameRound round, AttributeData data) { return acceptRoundOneSubattributes(game, round, data);}

            @Override
            public double calculateValue(Game game, GameRound round, AttributeData data) {
                double sum = 0;
                for (StatisticAttribute attr : data.mAttributes) {
                    AttributeData ownData = attr.getData();
                    if (attr instanceof GameStatistic && attr.acceptRound(game, round, ownData)) {
                        sum += ((GameStatistic) attr).calculateValue(game, round, ownData);
                    }
                }
                return sum;
            }
            
            @Override
            public double calculateValue(Game game, AttributeData data) {
                double sum = 0;
                for (StatisticAttribute attr : data.mAttributes) {
                    AttributeData ownData = attr.getData();
                    if (attr instanceof GameStatistic && attr.acceptGame(game, ownData)) {
                        sum += ((GameStatistic) attr).calculateValue(game, ownData);
                    }
                }
                return sum;
            }

        };
        statBuilder = new GameStatistic.Builder(stat, IDENTIFIER_STAT_OR_SUMMER, mGameKey);
        statBuilder.setNameAndDescriptionResId(R.string.statistic_general_summer_or_name, R.string.statistic_general_summer_or_descr);
        statBuilder.setPriority(StatisticAttribute.PRIORITY_HIDDEN);
        statBuilder.setPresentationType(GameStatistic.PRESENTATION_TYPE_ABSOLUTE);
        addAndCheck(statBuilder.getAttribute(), attrs, false);
        
        // contains team
        att = new UserStatisticAttribute() {
            private AttributeData customTeam = new AttributeData();
            private List<AbstractPlayerTeam> teamList = new LinkedList<AbstractPlayerTeam>();
            private String currCustomValueTeam;
            @Override public boolean requiresCustomValue() {return true;}
            @Override
            public boolean acceptGame(Game game, AttributeData data) {
                customTeam.mTeams = teamList;
                if ((currCustomValueTeam == null && !TextUtils.isEmpty(data.mCustomValue)) 
                        || (currCustomValueTeam != null && !currCustomValueTeam.equals(data.mCustomValue))) {
                    currCustomValueTeam = data.mCustomValue;
                    teamList.clear();
                    if (!TextUtils.isEmpty(currCustomValueTeam)) {
                        PlayerTeam team = new PlayerTeam();
                        teamList.add(team);
                        String[] split = currCustomValueTeam.split("\\+");
                        PlayerPool pool = GameKey.getPool(mGameKey);
                        for (String name : split) {
                            if (pool.contains(name)) {
                                team.addPlayer(pool.populatePlayer(name));
                            }
                        }
                    }
                }
               return super.acceptGame(game, data) && containsAllTeams(game, customTeam);
            }
            
        };
        attBuilder = new StatisticAttribute.Builder(att, IDENTIFIER_ATT_CONTAINS_TEAM, mGameKey);
        attBuilder.setPriority(StatisticAttribute.PRIORITY_NONE);
        attBuilder.setNameAndDescriptionResId(R.string.attribute_general_contains_team_name, R.string.attribute_general_contains_team_descr);
        addAndCheck(attBuilder.getAttribute(), attrs, false);
        return attrs.values();
    }

    public abstract Bundle applyMode(int mode, Resources res);

    public String getUnusedIdentifier(StatisticAttribute baseAttr) {
        int index = 0;
        String base = baseAttr.mIdentifier;
        boolean notContained = false;
        while (!notContained) {
            String curr = base + index;
            if (!mStats.containsKey(curr) && !mAttrs.containsKey(curr)) {
                notContained = true;
                return curr;
            }
            index++;
        }
        // cannot happen
        return null; 
    }

    public boolean isInitialized() {
        return !mStats.isEmpty() || !mAttrs.isEmpty();
    }
    
}
