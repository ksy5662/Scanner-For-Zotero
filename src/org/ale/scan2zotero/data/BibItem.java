package org.ale.scan2zotero.data;

import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.BaseColumns;

public class BibItem implements BaseColumns, Parcelable {

    public static final String TBL_NAME = "bibinfo";

    public static final String COL_DATE = "date";
    public static final String COL_TYPE = "type";
    public static final String COL_JSON = "json";
    public static final String COL_ACCT = "acct";

    public static final int TYPE_ERROR = -1;
    public static final int TYPE_BOOK = 0;
    public static final int TYPE_JOURNAL = 1;
    public static final int TYPE_MAGAZINE = 2;

    public static final int NO_ID = -1;

    private int mId;
    private long mCreationDate;
    private int mType;
    private JSONObject mInfo;
    private int mAcctId;
    private int mSelected;

    public BibItem(int id, long date, int type, JSONObject json, int acct){
        mId = id;
        mCreationDate = date;
        mType = type;
        mInfo = json;
        mSelected = 0;
        mAcctId = acct;
    }

    public BibItem(Parcel p) throws JSONException{
        this(p.readInt(), // _ID
             p.readLong(), // Creation Date
             p.readInt(), // Type
             new JSONObject(p.readString()), //JSON String
             p.readInt()); // Account ID
        mSelected = p.readInt();
    }

    public BibItem(int type, JSONObject json, int acct){
        this(NO_ID, (new Date()).getTime(), type, json, acct);
    }

    public void setId(int id){
        mId = id;
    }

    public int getId(){
        return mId;
    }

    public JSONObject getSelectedInfo(){
        try {
            return mInfo.getJSONArray("items").getJSONObject(mSelected);
        } catch (JSONException e) {
            return new JSONObject();
        }
    }

    public ContentValues toContentValues(){
        ContentValues values = new ContentValues();
        values.put(BibItem.COL_DATE, mCreationDate);
        values.put(BibItem.COL_TYPE, mType);
        values.put(BibItem.COL_JSON, mInfo.toString());
        values.put(BibItem.COL_ACCT, mAcctId);
        return values;
    }

    public static BibItem fromCursor(Cursor c){
        int id = c.getInt(S2ZDatabase.BIBINFO_ID_INDEX);
        long date = c.getLong(S2ZDatabase.BIBINFO_DATE_INDEX);
        int type = c.getInt(S2ZDatabase.BIBINFO_TYPE_INDEX);
        String key = c.getString(S2ZDatabase.BIBINFO_JSON_INDEX);
        int acct = c.getInt(S2ZDatabase.BIBINFO_ACCT_INDEX);
        JSONObject data;
        try {
            data = new JSONObject(key);
        } catch (JSONException e) {
            // XXX: Unparsable data in db, remove it and return null.
            data = new JSONObject();
        }
        return new BibItem(id,date,type,data,acct);
    }

    public static final Creator<BibItem> CREATOR = new Creator<BibItem>() {
        public BibItem createFromParcel(Parcel in) {
            BibItem r;
            try {
                r = new BibItem(in);
            } catch (JSONException e) {
                r = new BibItem(TYPE_ERROR, new JSONObject(), Account.NOT_IN_DATABASE);
            }
            return r;
        }

        public BibItem[] newArray(int size) {
            return new BibItem[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel p, int flags) {
        p.writeInt(mId);
        p.writeLong(mCreationDate);
        p.writeInt(mType);
        p.writeString(mInfo.toString());
        p.writeInt(mAcctId);
        p.writeInt(mSelected); // must be last
    }
}
