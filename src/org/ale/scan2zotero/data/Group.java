/** 
 * Copyright 2011 John M. Schanck
 * 
 * Scan2Zotero is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Scan2Zotero is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Scan2Zotero.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.ale.scan2zotero.data;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.provider.BaseColumns;

public class Group implements BaseColumns {
    public static final String TBL_NAME = "groups";

    public static final String COL_TITLE = "title";

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
