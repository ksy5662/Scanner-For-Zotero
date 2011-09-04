/** 
 * Copyright 2011 John M. Schanck
 * 
 * ScannerForZotero is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * ScannerForZotero is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with ScannerForZotero.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.ale.scanner.zotero.data;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.BaseColumns;

public class Account implements Parcelable, BaseColumns {

    public static final String TBL_NAME = "account";
    public static final String COL_ALIAS = "alias";
    public static final String COL_UID = "uid";
    public static final String COL_KEY = "key";

    public static final int NOT_IN_DATABASE = -1;

    private int mDbId;
    private String mAlias;
    private String mUid;
    private String mKey;

    public Account(int dbid, String alias, String uid, String key){
        mDbId = dbid;
        mAlias = alias;
        mUid = uid;
        mKey = key;
    }

    public Account(String alias, String uid, String key){
        this(-1, alias, uid, key);
    }

    public Account(Parcel p){
        this(p.readInt(), p.readString(), p.readString(), p.readString());
    }

    public boolean hasValidUserId() {
        return (mUid != null && 
                mUid.matches("[0-9]+"));
    }

    public boolean hasValidApiKey(){
        return (mKey != null && 
                mKey.matches("[A-Za-z0-9]{24}"));
    }

    public ContentValues toContentValues(){
        ContentValues values = new ContentValues();
        values.put(Account.COL_ALIAS, mAlias);
        values.put(Account.COL_UID, mUid);
        values.put(Account.COL_KEY, mKey);
        return values;
    }

    public void setDbId(int id) {
        mDbId = id;
    }

    public int getDbId() {
        return mDbId;
    }

    public void setAlias(String mAlias) {
        this.mAlias = mAlias;
    }

    public String getAlias() {
        return mAlias;
    }

    public void setUid(String mUid) {
        this.mUid = mUid;
    }

    public String getUid() {
        return mUid;
    }

    public void setKey(String mKey) {
        this.mKey = mKey;
    }

    public String getKey() {
        return mKey;
    }

    public static Account fromCursor(Cursor c){
        int dbid = c.getInt(Database.ACCOUNT_ID_INDEX);
        String alias = c.getString(Database.ACCOUNT_ALIAS_INDEX);
        String uid = c.getString(Database.ACCOUNT_UID_INDEX);
        String key = c.getString(Database.ACCOUNT_KEY_INDEX);
        return new Account(dbid, alias,uid,key);
    }

    public static final Creator<Account> CREATOR = new Creator<Account>() {
        public Account createFromParcel(Parcel in) {
            return new Account(in);
        }

        public Account[] newArray(int size) {
            return new Account[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel p, int flags) {
        p.writeInt(mDbId);
        p.writeString(mAlias);
        p.writeString(mUid);
        p.writeString(mKey);
    }

    public static void purgeAccount(ContentResolver cr, int row){
        String[] selection = new String[]{String.valueOf(row)};
        cr.delete(Database.ACCOUNT_URI,Account._ID+"=?", selection);
        cr.delete(Database.ACCESS_URI, Access.COL_ACCT+"=?", selection);
        cr.delete(Database.BIBINFO_URI, BibItem.COL_ACCT+"=?", selection);
    }

    public static void renameAccount(ContentResolver cr, int row, String name){
        ContentValues values = new ContentValues();
        values.put(Account.COL_ALIAS, name);
        String[] selection = new String[]{String.valueOf(row)};
        cr.update(Database.ACCOUNT_URI, values, Account._ID+"=?", selection);
    }
}
