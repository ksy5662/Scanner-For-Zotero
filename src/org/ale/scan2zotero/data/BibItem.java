package org.ale.scan2zotero.data;

import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.database.Cursor;
import android.provider.BaseColumns;

public class BibItem implements BaseColumns {

    public static final String TBL_NAME = "bibinfo";

    public static final String COL_DATE = "date";
    public static final String COL_TYPE = "type";
    public static final String COL_JSON = "json";

    public static final int TYPE_BOOK = 0;
    public static final int TYPE_JOURNAL = 1;
    public static final int TYPE_MAGAZINE = 2;

    public static final int NO_ID = -1;

    private int mId;
    private long mCreationDate;
    private int mType;
    private JSONObject mInfo;
    private int mSelected;
    
    public BibItem(int id, long date, int type, JSONObject json){
        mId = id;
        mCreationDate = date;
        mType = type;
        mInfo = json;
        mSelected = 0;
    }

    public BibItem(long date, int type, JSONObject json){
        this(NO_ID, date, type, json);

    }
    
    public BibItem(int type, JSONObject json){
        this(NO_ID, (new Date()).getTime(), type, json);
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
        return values;
    }

    public static BibItem fromCursor(Cursor c){
        int id = c.getInt(S2ZDatabase.BIBINFO_ID_INDEX);
        long date = c.getLong(S2ZDatabase.BIBINFO_DATE_INDEX);
        int type = c.getInt(S2ZDatabase.BIBINFO_TYPE_INDEX);
        String key = c.getString(S2ZDatabase.BIBINFO_JSON_INDEX);
        JSONObject data;
        try {
            data = new JSONObject(key);
        } catch (JSONException e) {
            // XXX: Unparsable data in db, remove it and return null.
            data = new JSONObject();
        }
        return new BibItem(id,date,type,data);
    }
}
