package org.ale.scan2zotero.data;

import android.provider.BaseColumns;

public class Collection implements BaseColumns {
    public static final String TBL_NAME = "collection";

    public static final String COL_TITLE = "title";
    public static final String COL_PARENT = "parent";
    
    public static final int NO_PARENT = 0;

}