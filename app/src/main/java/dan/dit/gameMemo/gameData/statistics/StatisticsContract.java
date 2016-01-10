package dan.dit.gameMemo.gameData.statistics;

import android.provider.BaseColumns;

public final  class StatisticsContract implements BaseColumns {
    private static final String TEXT_TYPE = " TEXT";
    private static final String INT_TYPE = " INTEGER";
    private static final String COMMA_SEP = ",";

    public static abstract class Attribute implements BaseColumns {
        // constants of columns' names for attributes
        public static final String COLUMN_NAME_GAMEKEY = "key"; // int, the gamekey the attribute belongs to
        public static final String COLUMN_NAME_ATTRIBUTE_NAME = "name"; // String, the that shortly describes the attribute or the value of the statistic
        public static final String COLUMN_NAME_DESCRIPTION = "descr"; // String, the description of the attribute, can be empty
        public static final String COLUMN_NAME_PRESENTATION_TYPE = "pres_type"; // int, the presentation type of the statistic
        public static final String COLUM_NAME_ATTRIBUTE_TYPE = "attr_type"; // integer, 0 for a general attribute, 1 if this attribute is a statistic
        public static final String COLUMN_NAME_SUB_ATTRIBUTES = "sub_attrs"; // String, compacted list of attributes' or statistics' identifiers
        public static final String COLUMN_NAME_PRIORITY = "prio"; // int, priority that statistic is listed for the user
        public static final String COLUMN_NAME_IDENTIFIER = "ident"; // String, identifies the attribute uniquely for a gamekey within all user and normal attributes
        public static final String COLUMN_NAME_REFERENCE_STATISTIC = "ref"; // String, can be null or empty, the identifier of a reference statistic for percentual presentation
        public static final String COLUMN_NAME_CUSTOM_VALUE = "cvalue"; // String, can contain anything, used by ValueGameStatistic
        public static final String COLUMN_NAME_BASE_ATTRIBUTE = "baseAtt"; // String, the identifier of the base attribute
        
        public static final String[] COLUMNS_ALL = new String[] {_ID, COLUM_NAME_ATTRIBUTE_TYPE, COLUMN_NAME_ATTRIBUTE_NAME, COLUMN_NAME_DESCRIPTION,
            COLUMN_NAME_GAMEKEY, COLUMN_NAME_IDENTIFIER, COLUMN_NAME_PRESENTATION_TYPE, COLUMN_NAME_PRIORITY, COLUMN_NAME_SUB_ATTRIBUTES, COLUMN_NAME_REFERENCE_STATISTIC,
            COLUMN_NAME_CUSTOM_VALUE, COLUMN_NAME_BASE_ATTRIBUTE};
        
        // general constants
        public static final String TABLE_NAME = "attrs"; // the table name that stores all attributes and GameStatistics
        public static final String SQL_CREATE_ENTRIES =
                "CREATE TABLE " + Attribute.TABLE_NAME + " (" +
                Attribute._ID + INT_TYPE + " PRIMARY KEY" + COMMA_SEP +
                Attribute.COLUMN_NAME_IDENTIFIER + TEXT_TYPE + COMMA_SEP +
                Attribute.COLUMN_NAME_GAMEKEY + INT_TYPE + COMMA_SEP +
                Attribute.COLUMN_NAME_ATTRIBUTE_NAME + TEXT_TYPE + COMMA_SEP +
                Attribute.COLUMN_NAME_DESCRIPTION + TEXT_TYPE + COMMA_SEP +
                Attribute.COLUMN_NAME_PRESENTATION_TYPE + INT_TYPE + COMMA_SEP +
                Attribute.COLUM_NAME_ATTRIBUTE_TYPE + INT_TYPE + COMMA_SEP +
                Attribute.COLUMN_NAME_SUB_ATTRIBUTES + TEXT_TYPE + COMMA_SEP +
                Attribute.COLUMN_NAME_PRIORITY + INT_TYPE + COMMA_SEP +
                Attribute.COLUMN_NAME_REFERENCE_STATISTIC + TEXT_TYPE + COMMA_SEP +
                Attribute.COLUMN_NAME_CUSTOM_VALUE + TEXT_TYPE + COMMA_SEP +
                Attribute.COLUMN_NAME_BASE_ATTRIBUTE + TEXT_TYPE + // NO COMMA DANIEL
                " )";
    }
    private StatisticsContract() {}
}
