package org.ale.scan2zotero.data;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.provider.BaseColumns;

public class Group implements BaseColumns {
    public static final String TBL_NAME = "groups";

    public static final String COL_TITLE = "title";

    public static final int NO_GROUP = -1;
    public static final int GROUP_ALL = 0;
    public static final int GROUP_LIBRARY = 1;

    private int mId;
    private String mTitle;

    public Group(int id, String title){
        mId = id;
        mTitle = title;
    }

    public void writeToDB(ContentResolver cr){
        ContentValues values = new ContentValues();
        values.put(Group._ID, mId);
        values.put(Group.COL_TITLE, mTitle);
        int rowc = cr.update(Database.GROUP_URI, values, null, null);
        if(rowc == 0){
            cr.insert(Database.GROUP_URI, values);
        }
    }
}
