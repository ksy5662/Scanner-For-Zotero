package org.ale.scan2zotero.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.BaseColumns;
import android.text.TextUtils;

public class Account implements Parcelable, BaseColumns {

    public static final String TBL_NAME = "account";
    public static final String COL_ALIAS = "alias";
    public static final String COL_UID = "uid";
    public static final String COL_KEY = "key";

    private String mAlias;
    private String mUid;
    private String mKey;

    public Account(String alias, String uid, String key){
        setAlias(alias);
        setUid(uid);
        setKey(key);
    }
    
    public Account(Parcel p){
        this(p.readString(), p.readString(), p.readString());
    }

    public boolean hasValidUserId() {
        return (mUid != null && mUid.length() == 6 && TextUtils.isDigitsOnly(mUid));
    }

    public boolean hasValidApiKey(){
        return (mKey != null && mKey.length() == 24);
    }
    
    public ContentValues toContentValues(){
        ContentValues values = new ContentValues();
        values.put(Account.COL_ALIAS, mAlias);
        values.put(Account.COL_UID, mUid);
        values.put(Account.COL_KEY, mKey);
        return values;
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
        String alias = c.getString(S2ZDatabase.ACCOUNT_ALIAS_INDEX);
        String uid = c.getString(S2ZDatabase.ACCOUNT_UID_INDEX);
        String key = c.getString(S2ZDatabase.ACCOUNT_KEY_INDEX);
        return new Account(alias,uid,key);
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
        p.writeString(mAlias);
        p.writeString(mUid);
        p.writeString(mKey);
    }

}
