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

package org.ale.scanner.zotero.web.worldcat;

import java.net.URI;

import org.ale.scanner.zotero.data.CreatorType;
import org.ale.scanner.zotero.data.ItemField;
import org.ale.scanner.zotero.data.ItemType;
import org.ale.scanner.zotero.web.APIHandler;
import org.ale.scanner.zotero.web.APIRequest;
import org.ale.scanner.zotero.web.HttpsClient;
import org.ale.scanner.zotero.web.RequestQueue;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.Bundle;

public class WorldCatAPIClient {

    public static final String BOOK_SEARCH_BASE = "http://xisbn.worldcat.org/webservices/xid/isbn/";
    public static final String BOOK_SEARCH_QUERY = "?method=getMetadata&fl=*&format=json&count=1";

    public static final String EXTRA_ISBN = "ISBN";

    public static final int STATUS_OK = 0;
    public static final int STATUS_NOT_FOUND = 1;
    public static final int STATUS_INVALID = 2;
    public static final int STATUS_OVER_LIMIT = 3;

    private DefaultHttpClient mHttpsClient;

    private RequestQueue mRequestQueue;

    private APIHandler mHandler;

    public WorldCatAPIClient() {
        mHandler = WorldCatHandler.getInstance();
        mHttpsClient = HttpsClient.getInstance();
        mRequestQueue = RequestQueue.getInstance();
    }

    private APIRequest newRequest(){
        return new APIRequest(mHandler, mHttpsClient);
    }

    public void isbnLookup(String isbn) {
        APIRequest r = newRequest();
        r.setHttpMethod(APIRequest.GET);
        r.setURI(URI.create(BOOK_SEARCH_BASE+isbn+BOOK_SEARCH_QUERY));
        Bundle extra = new Bundle();
        extra.putString(WorldCatAPIClient.EXTRA_ISBN, isbn);
        r.setExtra(extra);

        mRequestQueue.enqueue(r);
    }

    public static JSONObject strToJSON(String resp){
        try{
            // Try to create a JSONObject from resp
            return new JSONObject(resp);
        }catch(JSONException e){
            // If that fails, try to indicate that there was an error
            try{
                return new JSONObject().put("stat", "badResponse"); 
            }catch(JSONException e2){
                // Or just return an empty JSONObject
                return new JSONObject();
            }
        }
    }

    public static int getStatus(JSONObject resp){
        try {
            String status = resp.getString("stat");
            if(status.equals("ok")){
                return STATUS_OK;
            }else if(status.equals("unknownId")){
                return STATUS_NOT_FOUND;
            }else if(status.equals("overlimit")){
                return STATUS_OVER_LIMIT;
            }else{
                return STATUS_INVALID;
            }
        } catch (JSONException e) {
            return STATUS_INVALID;
        }
    }

    public static JSONObject translateJsonResponse(String isbn, JSONObject resp){
        /* Example response:
            { "stat":"ok",
             "list":[{
                "url":["http://www.worldcat.org/oclc/177669176?referer=xid"],
                "publisher":"O'Reilly",
                "form":["BA"],
                "lccn":["2004273129"],
                "lang":"eng",
                "city":"Sebastopol, CA",
                "author":"by Mark Lutz and David Ascher.",
                "ed":"2nd ed.",
                "year":"2003",
                "isbn":["0596002815"],
                "title":"Learning Python",
                "oclcnum":["177669176",...,"79871142"]}]}
         */

        JSONObject translation = new JSONObject();
        try {
            JSONObject oItem = resp.getJSONArray("list").getJSONObject(0);

            JSONObject creator = new JSONObject()
                .put(CreatorType.type, "author")
                .put(ItemField.Creator.name, oItem.optString("author"));

            JSONObject tItem = new JSONObject()
                .put(ItemType.type, ItemType.book) /* XXX: Always 'book' */
                .put(ItemField.ISBN, isbn)
                .put(ItemField.title, oItem.optString("title"))
                .put(ItemField.language, oItem.optString("lang"))
                .put(ItemField.creators, new JSONArray())
                .accumulate(ItemField.creators, creator)
                .put(ItemField.place, oItem.optString("city"))
                .put(ItemField.date, oItem.optString("year"))
                .put(ItemField.publisher, oItem.optString("publisher"))
                .put(ItemField.edition, oItem.optString("ed"));

            translation.put("items", new JSONArray());
            translation.accumulate("items", tItem);
        } catch (JSONException e) {
            return null;
        }
        return translation;
    }
}