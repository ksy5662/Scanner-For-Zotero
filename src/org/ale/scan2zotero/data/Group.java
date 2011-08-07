package org.ale.scan2zotero.data;

import android.provider.BaseColumns;

public class Group implements BaseColumns {
    public static final String TBL_NAME = "groups";

    public static final String COL_TITLE = "title";

    public static final int NO_GROUP = -1;
    public static final int GROUP_ALL = 0;
    public static final int GROUP_LIBRARY = 1;
}
