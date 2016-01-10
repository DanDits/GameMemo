package dan.dit.gameMemo.gameData.statistics;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.content.ContentValues;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import dan.dit.gameMemo.gameData.game.Game;
import dan.dit.gameMemo.gameData.game.GameKey;
import dan.dit.gameMemo.gameData.game.GameRound;
import dan.dit.gameMemo.gameData.player.AbstractPlayerTeam;
import dan.dit.gameMemo.util.compaction.Compacter;

public abstract class StatisticAttribute implements Comparable<StatisticAttribute> {
    protected long mId = -1;
    protected int mNameResId;
    protected int mDescriptionResId;
    protected int mGameKey;
    protected int mPriority; // -1 means hidden, 0 means not set, a higher value means a lower position in the list
    protected String mIdentifier; // must be made unique for a gamekey
    
    // data required for calculations
    protected AttributeData mData = new AttributeData();
    
    // fields required for user made statistics
    public static final int ATTRIBUTE_TYPE_USER_BASIC = 0;
    public static final int PRIORITY_NONE = 0;
    public static final int PRIORITY_HIDDEN = -1;
    
    protected String mName;
    protected String mDescription = "";
    private boolean mUserCreated;
    
    // protected constructor prevents instantiation without the builder
    protected StatisticAttribute() {}
    
    protected static class WrappedStatisticAttribute extends StatisticAttribute {
        protected StatisticAttribute mBaseAttr;
        private WrappedStatisticAttribute(StatisticAttribute baseAttr) {
            mBaseAttr = baseAttr;
        }
        
        @Override
        public boolean requiresCustomValue() {
            return mBaseAttr.requiresCustomValue();
        }
        
        @Override
        protected void addSaveData(ContentValues val) {
            val.put(StatisticsContract.Attribute.COLUMN_NAME_BASE_ATTRIBUTE, mBaseAttr.mIdentifier);
        }

        @Override
        public boolean acceptGame(Game game, AttributeData data) {
            boolean result = mBaseAttr.acceptGame(game, data);
            return result;
        }

        @Override
        public boolean acceptRound(Game game, GameRound round, AttributeData data) {
            boolean result = mBaseAttr.acceptRound(game, round, data);
            return result;
        }
        
        @Override
        public void setTeams(List<AbstractPlayerTeam> teams) {
            super.setTeams(teams);
            mBaseAttr.setTeams(teams);
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
        public Set<StatisticAttribute> getAttributes() {
            Set<StatisticAttribute> own = super.getAttributes();
            Set<StatisticAttribute> base = mBaseAttr.getAttributes();
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
        public StatisticAttribute getBaseAttribute() {
            return mBaseAttr;
        }
        
        @Override
        public String toString() {
            return super.toString() + " wrapping " + mBaseAttr.mIdentifier;
        }
        
    }
    public static class Builder {
        protected StatisticAttribute mAttr;
        
        // only for child class
        protected Builder() {}
        
        /**
         * Constructor for creating a simple attribute that does not implement any own
         * accepting logic but simply uses its sub attributes logic and can serve as a flag.
         * @param identifier A String that can be based on the name which must be unique within all attributes' identifier for a gamekey
         * @param gameKey The gamekey of this attribute
         */
        public Builder(String identifier, int gameKey) {
            this(new UserStatisticAttribute(), identifier, gameKey); 
        }
        /**
         * Constructer building an attribute out of an existing one.
         * @param identifier The identifier that needs to be unique within the attributes for a gamekey or an empty String if a new identifier
         * should be created.
         * @param baseAttr The basic attribute which's logic is simply copied
         */
        public Builder(String identifier, StatisticAttribute baseAttr) {
            WrappedStatisticAttribute attr = new WrappedStatisticAttribute(baseAttr);
            if (TextUtils.isEmpty(identifier)) {
                identifier = GameKey.getGameStatisticAttributeManager(baseAttr.mGameKey).getUnusedIdentifier(baseAttr);
            }
            attr.mIdentifier = identifier;
            if (TextUtils.isEmpty(identifier) || baseAttr == null) {
                throw new IllegalArgumentException("Identifier or base attribute invalid: " + identifier + " / " + baseAttr);
            }
            mAttr = attr;
            attr.mGameKey = attr.mBaseAttr.mGameKey;
            setNameAndDescription(baseAttr.mName, baseAttr.mDescription);
            setNameAndDescriptionResId(baseAttr.mNameResId, baseAttr.mDescriptionResId);
            setPriority(baseAttr.mPriority);
        }
        
        /**
         * Constructor for predefined attributes. The attribute will not be created by the builder
         * since it is an abstract class but it will be initialized correctly.
         * @param attr The attribute that needs to be build.
         * @param identifier The identifier for the new attribute.
         * @param gameKey The game key for the new attribute.
         */
        public Builder(StatisticAttribute attr, String identifier, int gameKey) {
            mAttr = attr;
            mAttr.mIdentifier = identifier;
            mAttr.mGameKey = gameKey;
        }
      
        public void setUserCreated() {
            mAttr.mUserCreated = true;
        }
        
        public void setNameAndDescription(String name, String descr) {
            mAttr.mName = name;
            mAttr.mDescription = descr;
        }
        
        public void setPriority(int prio) {
            mAttr.mPriority = prio;
        }
        
        public void setNameAndDescriptionResId(int nameResId, int descrResId) {
            mAttr.mNameResId = nameResId;
            mAttr.mDescriptionResId = descrResId;
        }
        
        public StatisticAttribute getAttribute() {
            return mAttr;
        }

        private void setId(long id) {
            mAttr.mId = id;
        }

        public void setCustomValue(String customValue) {
            mAttr.mData.mCustomValue = customValue;
        }
    }        
 
    public boolean canBeAdded(StatisticAttribute attr) {
        // if ancestor: we cant make the ancestor attribut the child of its descendent, it's like your grandpa is the son of your child
        return attr != null && !attr.isAncestorOf(this);
    }
    
    // general methods
    public boolean addAttribute(StatisticAttribute attr) {
        // to prevent infinite loops the attribute - subattribute tree may not contain any cycles
        if (canBeAdded(attr)) {
            return mData.mAttributes.add(attr);
        }
        return false;
    }
    
    private boolean isAncestorOf(StatisticAttribute child) {
        if (this.equals(child)) {
            return true; // you are your own descendent and ancestor, weird
        }
        // recursivly search children , if a child of me is an ancestor of given child, I'm still an ancestor
        for (StatisticAttribute attr : getAttributes()) {
            if (attr.isAncestorOf(child)) {
                return true;
            }
        }
        return false;
    }
    
    public boolean removeAttribute(StatisticAttribute attr) {
        return mData.mAttributes.remove(attr);
    }
    
    protected AttributeData getData() {
        return mData;
    }
    
    protected boolean hasId() {
        return mId != -1;
    }
    
    // methods required for user made statistics
    public final void save(SQLiteDatabase db) {
        if (!isUserAttribute()) {
            throw new UnsupportedOperationException("Cannot save a predefined attribute");
        }
        ContentValues val = new ContentValues();
        val.put(StatisticsContract.Attribute.COLUM_NAME_ATTRIBUTE_TYPE, ATTRIBUTE_TYPE_USER_BASIC);
        val.put(StatisticsContract.Attribute.COLUMN_NAME_GAMEKEY, mGameKey);
        val.put(StatisticsContract.Attribute.COLUMN_NAME_ATTRIBUTE_NAME, mName);
        val.put(StatisticsContract.Attribute.COLUMN_NAME_DESCRIPTION, mDescription);
        val.put(StatisticsContract.Attribute.COLUMN_NAME_PRIORITY, mPriority);
        val.put(StatisticsContract.Attribute.COLUMN_NAME_IDENTIFIER, mIdentifier);
        Compacter cmp = new Compacter(mData.mAttributes.size());
        for (StatisticAttribute attr : mData.mAttributes) {
            cmp.appendData(attr.mIdentifier);
        }
        val.put(StatisticsContract.Attribute.COLUMN_NAME_SUB_ATTRIBUTES, cmp.compact());
        val.put(StatisticsContract.Attribute.COLUMN_NAME_CUSTOM_VALUE, mData.mCustomValue);
        addSaveData(val);
        handleDatabaseOperation(db, val);
    }
    
    /**
     * Loads and builds an attribute from the cursor's current position. Keep in mind that the order of loading
     * can be important as this attribute's base attribute can be a user attribute that is not yet loaded.
     * @param cursor
     * @return
     */
    public static final StatisticAttribute load(Cursor cursor, GameStatisticAttributeManager manager) {
        if (cursor == null || cursor.isAfterLast()) {
            throw new IllegalArgumentException("Invalid cursor: " + cursor);
        }
        int attrType = cursor.getInt(cursor.getColumnIndexOrThrow(StatisticsContract.Attribute.COLUM_NAME_ATTRIBUTE_TYPE));
        String ownIdentifier = cursor.getString(cursor.getColumnIndexOrThrow(StatisticsContract.Attribute.COLUMN_NAME_IDENTIFIER));
        Compacter subAttrs = new Compacter(cursor.getString(cursor.getColumnIndexOrThrow(StatisticsContract.Attribute.COLUMN_NAME_SUB_ATTRIBUTES)));
        String baseStatIdentifier = cursor.getString(cursor.getColumnIndexOrThrow(StatisticsContract.Attribute.COLUMN_NAME_BASE_ATTRIBUTE));
        StatisticAttribute baseAttr = manager.getAttribute(baseStatIdentifier);
        if (baseAttr == null) {
            // cannot load since base attribute is not yet loaded
            return null; 
        }
        StatisticAttribute.Builder builder;
        if (attrType == GameStatistic.ATTRIBUTE_TYPE_USER_STATISTIC) {
            GameStatistic.Builder statBuilder = new GameStatistic.Builder(ownIdentifier, (GameStatistic) baseAttr);
            int presType = cursor.getInt(cursor.getColumnIndexOrThrow(StatisticsContract.Attribute.COLUMN_NAME_PRESENTATION_TYPE));
            statBuilder.setPresentationType(presType);
            statBuilder.setReferenceStatisticIdentifier(cursor.getString(cursor.getColumnIndexOrThrow(StatisticsContract.Attribute.COLUMN_NAME_REFERENCE_STATISTIC)));
            builder = statBuilder;
        } else if (attrType == ATTRIBUTE_TYPE_USER_BASIC){
            builder = new StatisticAttribute.Builder(ownIdentifier, baseAttr);
        } else {
            throw new IllegalArgumentException("Illegal attribute type: " + attrType);
        }
        for (int i = 0; i < subAttrs.getSize(); i++) {
            StatisticAttribute subAttr = manager.getAttribute(subAttrs.getData(i));
            if (subAttr != null) {
                builder.mAttr.addAttribute(subAttr);
            } else {
                // sub attribute not yet loaded, this can happen since the loading order is not precalculated, manager is responsible to add it after loading all
                manager.addMissingSubAttribute(builder.mAttr, subAttrs.getData(i));
            }
        }
        String name = cursor.getString(cursor.getColumnIndexOrThrow(StatisticsContract.Attribute.COLUMN_NAME_ATTRIBUTE_NAME));
        String descr = cursor.getString(cursor.getColumnIndexOrThrow(StatisticsContract.Attribute.COLUMN_NAME_DESCRIPTION));
        int prio = cursor.getInt(cursor.getColumnIndexOrThrow(StatisticsContract.Attribute.COLUMN_NAME_PRIORITY));
        builder.setPriority(prio);
        builder.setNameAndDescription(name, descr);
        builder.setNameAndDescriptionResId(TextUtils.isEmpty(name) ? builder.getAttribute().mNameResId : 0, TextUtils.isEmpty(descr) ? builder.getAttribute().mDescriptionResId : 0);
        builder.setId(cursor.getLong(cursor.getColumnIndexOrThrow(StatisticsContract.Attribute._ID)));
        builder.setCustomValue(cursor.getString(cursor.getColumnIndexOrThrow(StatisticsContract.Attribute.COLUMN_NAME_CUSTOM_VALUE)));
        builder.setUserCreated();
        return builder.getAttribute();
    }
    
    public boolean isUserAttribute() {
        return mUserCreated;
    }

    private void handleDatabaseOperation(SQLiteDatabase db, ContentValues val) {
        if (hasId()) {
            // already has an id, simply update the entry
            db.update(StatisticsContract.Attribute.TABLE_NAME, val, StatisticsContract.Attribute._ID + "=" + mId, null);
        } else {
            mId = db.insert(StatisticsContract.Attribute.TABLE_NAME, null, val);
        }
    }
    
    protected abstract void addSaveData(ContentValues val);
    
    public final boolean isDeleteable() {
        return isUserAttribute() && hasId();
    }
    
    public final void delete(SQLiteDatabase db) {
        if (!isUserAttribute()) {
            throw new UnsupportedOperationException("Cannot delete predefined attribute");
        }
        if (hasId()) {
            db.delete(StatisticsContract.Attribute.TABLE_NAME, StatisticsContract.Attribute._ID + "=" + mId, null);
        } else {
            throw new IllegalStateException("Cannot delete attribute with no id.");
        }
    }
    
    // methods required for calculation
    public abstract boolean acceptGame(Game game, AttributeData data);
    public abstract boolean acceptRound(Game game, GameRound round, AttributeData data);
    
    /**
     * Returns all sub attributes of this attribute.
     */
    public Set<StatisticAttribute> getAttributes() {
        return mData.mAttributes;
    }
    
    /**
     * Returns all own sub attributes of this attribute, excluding those
     * inherited from the base attribute if existant.
     * @return All own subattributes.
     */
    public final Set<StatisticAttribute> getOwnAttributes() {
        return mData.mAttributes;
    }
    
    public void setTeams(List<AbstractPlayerTeam> teams) {
        mData.mTeams = teams;
        for (StatisticAttribute attr : mData.mAttributes) {
            attr.setTeams(teams);
        }
    }
    
    @Override
    public boolean equals(Object other) {
        if (other instanceof StatisticAttribute) {
            return mIdentifier.equals(((StatisticAttribute) other).mIdentifier);
        } else {
            return super.equals(other);
        }
    }
    
    @Override
    public int hashCode() {
        return mIdentifier.hashCode();
    }

    public AbstractPlayerTeam getTeam(int index) {
        return mData.getTeam(index);
    }

    public int getTeamsCount() {
        return mData.getTeamsCount();
    }
    
    public boolean acceptGameAllSubattributes(Game game, AttributeData data) {
        for (StatisticAttribute attr : data.mAttributes) {
            if (!attr.acceptGame(game, attr.getData())) {
                return false;
            }
        }
        return true;
    }
    
    public boolean acceptRoundAllSubattributes(Game game, GameRound round, AttributeData data) {
        for (StatisticAttribute attr : data.mAttributes) {
            if (!attr.acceptRound(game, round, attr.getData())) {
                return false;
            }
        }
        return true;
    }
    
    public boolean acceptGameOneSubattributes(Game game, AttributeData data) {
        for (StatisticAttribute attr : data.mAttributes) {
            if (attr.acceptGame(game, attr.getData())) {
                return true;
            }
        }
        return false;
    }
    
    public boolean acceptRoundOneSubattributes(Game game, GameRound round, AttributeData data) {
        for (StatisticAttribute attr : data.mAttributes) {
            if (attr.acceptRound(game, round, attr.getData())) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public String toString() {
        Set<StatisticAttribute> subAttrs = getAttributes();
        if (subAttrs.size() > 0) {
            return "Attribute " + mIdentifier + " (own: " + mData.mAttributes + ", all: " + subAttrs + ")";
        } else {
            return "Attribute " + mIdentifier;
        }
    }

    public String getIdentifier() {
        return mIdentifier;
    }

    public List<AbstractPlayerTeam> getTeams() {
        return mData.mTeams;
    }

    public CharSequence getName(Resources res) {
        return mNameResId != 0 ? res.getString(mNameResId) : (TextUtils.isEmpty(mName) ? mIdentifier : mName);
    }
    
    public CharSequence getDescription(Resources res) {
        return mDescriptionResId != 0 ? res.getString(mDescriptionResId) : mDescription;
    }
    
    @Override
    public int compareTo(StatisticAttribute other) {
        if (mPriority > other.mPriority) {
            return -1;
        } else if (other.mPriority > mPriority) {
            return 1;
        } else {
            return mIdentifier.compareToIgnoreCase(other.mIdentifier);
        }
    }


    public void setPriority(int prio) {
        mPriority = prio;
    }
    
    /**
     * If the attribute requires a custom value. This signals the UI to allow
     * the user to enter a custom value. Overwrite and return true if required. 
     * @return Defaults to false.
     */
    public boolean requiresCustomValue() {
        return false; 
    }

    public void setName(String name) {
        if (name == null) {
            throw new IllegalArgumentException();
        }
        mName = name;
        mNameResId = 0;
    }
    
    public void setDescription(String descr) {
        if (descr == null) {
            throw new IllegalArgumentException();
        }
        mDescription = descr;
        mDescriptionResId = 0;
    }

    public void setCustomValue(String value) {
        mData.mCustomValue = value;
    }

    public String getCustomValue() {
        return mData.mCustomValue;
    }

    public StatisticAttribute getBaseAttribute() {
        return null;
    }

    public int getPriority() {
        return mPriority;
    }
}
