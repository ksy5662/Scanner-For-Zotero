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

import java.util.ArrayList;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.BaseColumns;
import android.text.TextUtils;

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
    
    private String mCachedCreatorLabel = null;
    private String mCachedCreatorValue = null;
    private String mCachedTitleValue = null;

    public BibItem(int id, long date, int type, JSONObject json, int acct){
        mId = id;
        mCreationDate = date;
        mType = type;
        mInfo = json;
        mSelected = 0;
        mAcctId = acct;
    }

    public BibItem(int type, JSONObject json, int acct){
        this(NO_ID, (new Date()).getTime(), type, json, acct);
    }

    public BibItem(Parcel p) throws JSONException{
        this(p.readInt(), // _ID
             p.readLong(), // Creation Date
             p.readInt(), // Type
             new JSONObject(p.readString()), //JSON String
             p.readInt());  // Account ID
        mSelected = p.readInt();
    }

    public static BibItem fromCursor(Cursor c){
        int id = c.getInt(Database.BIBINFO_ID_INDEX);
        long date = c.getLong(Database.BIBINFO_DATE_INDEX);
        int type = c.getInt(Database.BIBINFO_TYPE_INDEX);
        String json = c.getString(Database.BIBINFO_JSON_INDEX);
        int acct = c.getInt(Database.BIBINFO_ACCT_INDEX);

        JSONObject data;
        try {
            data = new JSONObject(json);
        } catch (JSONException e) {
            return null;
        }
        
        return new BibItem(id,date,type,data,acct);
    }

    /* Parceling */
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

    /* Data access */
    public void setId(int id){
        mId = id;
    }

    public int getId(){
        return mId;
    }

    public void setSelected(int sel){
        mSelected = sel;
    }

    public JSONObject getSelectedInfo(){
        try {
            return mInfo.getJSONArray("items").getJSONObject(mSelected);
        } catch (JSONException e) {
            return new JSONObject();
        }
    }

    public BibItem copy() {
        JSONObject jsoncopy;
        try {
            jsoncopy = new JSONObject(mInfo.toString());
        } catch (JSONException e) {
            jsoncopy = new JSONObject();
        }
        BibItem ret = new BibItem(mId, mCreationDate, mType, jsoncopy, mAcctId);
        ret.setSelected(mSelected);
        return ret;
    }

    /* Database */
    public ContentValues toContentValues(){
        ContentValues values = new ContentValues();
        values.put(BibItem.COL_DATE, mCreationDate);
        values.put(BibItem.COL_TYPE, mType);
        values.put(BibItem.COL_JSON, mInfo.toString());
        values.put(BibItem.COL_ACCT, mAcctId);
        return values;
    }

    public void writeToDB(ContentResolver cr){
        ContentValues values = toContentValues();
        if(mId == NO_ID) {
            Uri row = cr.insert(Database.BIBINFO_URI, values);
            int id = Integer.parseInt(row.getLastPathSegment());
            setId(id);
        } else {
            cr.update(Database.BIBINFO_URI,
                      values, BibItem._ID+"=?",
                      new String[]{String.valueOf(mId)});
        }
    }

    /* Caching for textviews */
    public boolean hasCachedValues(){
        return (mCachedTitleValue != null)
            && (mCachedCreatorLabel != null)
            && (mCachedCreatorValue != null);
    }

    public void cacheForViews() {
        JSONObject data = getSelectedInfo();
        mCachedTitleValue = data.optString(ItemField.title);
        mCachedCreatorLabel = null;

        JSONArray creators = data.optJSONArray(ItemField.creators);
        if(creators != null && creators.length() > 0){
            // Choose the creator label based on the first creator type
            // then accumulate all creators with that type to be displayed
            JSONObject jobj;
            ArrayList<String> creatorNames = new ArrayList<String>();
            for(int i=0; i<creators.length(); i++){
                jobj = (JSONObject) creators.opt(i);
                if(jobj == null) continue;
                String type = jobj.optString(CreatorType.type);
                if(TextUtils.isEmpty(mCachedCreatorLabel)){
                    mCachedCreatorLabel = type;
                }else if(!type.equals(mCachedCreatorLabel)){
                    break;
                }
                String name = jobj.optString(ItemField.Creator.name);
                if(!TextUtils.isEmpty(name))
                    creatorNames.add(name);
            }
            int indx = CreatorType.Book.indexOf(mCachedCreatorLabel);
            mCachedCreatorLabel = CreatorType.LocalizedBook.get(indx < 0 ? 0 : indx);
            mCachedCreatorValue = TextUtils.join(", ", creatorNames);
        }else{
            mCachedCreatorLabel = CreatorType.LocalizedBook.get(0);
            mCachedCreatorValue = "";
        }
    }
    public void clearCache(){
        mCachedCreatorLabel = null;
        mCachedCreatorValue = null;
        mCachedTitleValue = null;
    }
    public String getCachedCreatorLabel(){
        return mCachedCreatorLabel;
    }
    public String getCachedCreatorValue(){
        return mCachedCreatorValue;
    }
    public String getCachedTitleString(){
        return mCachedTitleValue;
    }
}
