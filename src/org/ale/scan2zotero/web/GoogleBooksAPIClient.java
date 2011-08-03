package org.ale.scan2zotero.web;

import java.net.URI;

import org.ale.scan2zotero.Util;
import org.ale.scan2zotero.data.CreatorType;
import org.ale.scan2zotero.data.ItemField;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.Handler;
import android.text.TextUtils;

public class GoogleBooksAPIClient {

    public static final String BOOK_SEARCH_ISBN = "https://www.googleapis.com/books/v1/volumes?prettyPrint=flase&q=isbn:";

    private DefaultHttpClient mHttpsClient;

    private RequestQueue mRequestQueue;

    private Handler mHandler;

    public GoogleBooksAPIClient(Handler handler) {
        mHandler = handler;
        mHttpsClient = HttpsClient.getHttpsClientInstance();
        mRequestQueue = RequestQueue.getInstance();
    }

    public void isbnLookup(String isbn) {
        APIRequest g = new APIRequest(mHandler, mHttpsClient);
        g.setRequestType(APIRequest.GET);
        g.setURI(URI.create(BOOK_SEARCH_ISBN+isbn));
        g.setReturnIdentifier(isbn);

        mRequestQueue.enqueue(g);
    }

    public static JSONObject translateJsonResponse(String isbn, String resp){
        // Returns empty JSONObject on failure.
        JSONObject translation = new JSONObject();
        JSONObject orig = null;
        try {
            orig = new JSONObject(resp);

            // Google search always returns "books#volumes"
            if(!orig.optString("kind").equals("books#volumes")){
                return translation;
            }

            JSONArray origItems = orig.optJSONArray("items");
            JSONArray transItems = new JSONArray();
            for(int i=0; i < origItems.length(); i++){
                JSONObject oItem = origItems.getJSONObject(i);
                JSONObject volInfo = oItem.optJSONObject("volumeInfo");
                JSONObject tItem = new JSONObject();

                /* Get ISBN/ISSN info */
                String bestId = null;
                String bestType = ItemField.ISBN;
                JSONArray identifiers = volInfo.getJSONArray("industryIdentifiers");
                for(int j=0; j<identifiers.length(); j++){
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
                tItem.put(bestType, bestId);

                /* Get title  */
                String subtitle = volInfo.optString("subtitle");
                if(!TextUtils.isEmpty(subtitle)){
                    tItem.put(ItemField.title, 
                            volInfo.optString("title") + ": " + subtitle);
                }else{
                    tItem.put(ItemField.title, volInfo.optString("title"));
                }

                /* Get Creators  */
                JSONArray creators = new JSONArray();
                JSONArray authors = volInfo.getJSONArray("authors");
                for(int j=0; j<authors.length(); j++){
                    JSONObject author = new JSONObject();
                    author.put(CreatorType.type, CreatorType.Book.author);
                    author.put(ItemField.Creator.name, authors.get(j));
                    creators.put(author);
                }
                tItem.put(ItemField.creators, creators);

                /* Get Other info  */
                tItem.put(ItemField.publisher, volInfo.optString("publisher"));
                tItem.put(ItemField.date, volInfo.optString("publishedDate"));
                //tItem.put(ItemField.abstractNote, volInfo.optString("description"));
                tItem.put(ItemField.numPages, volInfo.optString("pageCount"));
                tItem.put(ItemField.language, volInfo.optString("language"));
                transItems.put(tItem);
            }
            if(transItems.length() > 0){
                translation.put("items", transItems);
            } // Otherwise we're returning an empty JSONObject
        } catch (JSONException e) {
            e.printStackTrace();
            return translation;
        }
        return translation;
    }
}