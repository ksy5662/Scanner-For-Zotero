package org.ale.scan2zotero.web.googlebooks;

import java.net.URI;

import org.ale.scan2zotero.Util;
import org.ale.scan2zotero.data.CreatorType;
import org.ale.scan2zotero.data.ItemField;
import org.ale.scan2zotero.data.ItemType;
import org.ale.scan2zotero.web.APIHandler;
import org.ale.scan2zotero.web.APIRequest;
import org.ale.scan2zotero.web.HttpsClient;
import org.ale.scan2zotero.web.RequestQueue;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.text.TextUtils;

public class GoogleBooksAPIClient {

    public static final String BOOK_SEARCH_ISBN = "https://www.googleapis.com/books/v1/volumes?prettyPrint=flase&q=isbn:";

    private DefaultHttpClient mHttpsClient;

    private RequestQueue mRequestQueue;

    private APIHandler mHandler;
    
    public GoogleBooksAPIClient() {
        mHandler = GoogleBooksHandler.getInstance();
        mHttpsClient = HttpsClient.getInstance();
        mRequestQueue = RequestQueue.getInstance();
    }

    private APIRequest newRequest(){
        return new APIRequest(mHandler, mHttpsClient);
    }

    public void isbnLookup(String isbn) {
        APIRequest r = newRequest();
        r.setRequestType(APIRequest.GET);
        r.setURI(URI.create(BOOK_SEARCH_ISBN+isbn));
        r.setReturnIdentifier(isbn);

        mRequestQueue.enqueue(r);
    }

    public static JSONObject translateJsonResponse(String isbn, String resp){
        // Returns empty JSONObject on failure.
        JSONObject translation = new JSONObject();
        JSONObject jsonResp = null;
        try {
            jsonResp = new JSONObject(resp);

            // Google search always returns "books#volumes"
            String kind = jsonResp.optString("kind");
            JSONArray respItems = jsonResp.optJSONArray("items");
            if(kind == null || !kind.equals("books#volumes") || respItems == null) {
                translation.put(ItemField.ISBN, isbn);
                return translation;
            }

            JSONArray transItems = new JSONArray();
            for(int i=0; i < respItems.length(); i++){
                // oItem is google's result, tItem is our translation of it
                JSONObject orig = respItems.getJSONObject(i);
                JSONObject volInfo = orig.optJSONObject("volumeInfo");
                JSONObject trans = new JSONObject();
                if(volInfo == null){
                    transItems.put(trans);
                    continue;
                }

                /* Set the itemType XXX: Always 'book' */
                trans.put(ItemType.type, ItemType.book);

                /* Get ISBN/ISSN info */
                String bestId = isbn;
                String bestType = ItemField.ISBN;
                JSONArray identifiers = volInfo.optJSONArray("industryIdentifiers");
                for(int j=0; identifiers != null && j<identifiers.length(); j++){
                    JSONObject identifier = identifiers.getJSONObject(j);
                    String idType = identifier.getString("type");

                    String id = identifier.getString("identifier");
                    if(bestId == null){
                        bestId = id;
                        if(idType.equals("ISSN")) bestType = ItemField.ISSN;
                    }
                    if(Util.isbnMatch(id, isbn)){
                        if(bestId != id && bestId.length() < id.length()){
                            bestId = id;
                            if(idType.equals("ISSN")) bestType = ItemField.ISSN;
                        }
                        break;
                    }
                }
                trans.put(bestType, bestId);

                /* Get title  */
                String subtitle = volInfo.optString("subtitle");
                if(!TextUtils.isEmpty(subtitle)){
                    trans.put(ItemField.title, 
                            volInfo.optString("title") + ": " + subtitle);
                }else{
                    trans.put(ItemField.title, volInfo.optString("title"));
                }

                /* Get Creators  */
                JSONArray creators = new JSONArray();
                JSONArray authors = volInfo.optJSONArray("authors");
                for(int j=0; authors != null && j<authors.length(); j++){
                    JSONObject author = new JSONObject();
                    author.put(CreatorType.type, CreatorType.Book.author);
                    author.put(ItemField.Creator.name, authors.get(j));
                    creators.put(author);
                }
                if(creators.length() > 0)
                    trans.put(ItemField.creators, creators);

                /* Get Other info  */
                trans.put(ItemField.publisher, volInfo.optString("publisher"));
                trans.put(ItemField.date, volInfo.optString("publishedDate"));
                //tItem.put(ItemField.abstractNote, volInfo.optString("description"));
                trans.put(ItemField.numPages, volInfo.optString("pageCount"));
                trans.put(ItemField.language, volInfo.optString("language"));
                transItems.put(trans);
            }
            if(transItems.length() > 0){
                translation.put("items", transItems);
            } // Otherwise we're returning an empty JSONObject
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
        return translation;
    }
}