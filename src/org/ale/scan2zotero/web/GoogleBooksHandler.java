package org.ale.scan2zotero.web;

import org.apache.http.StatusLine;
import org.json.JSONObject;

public class GoogleBooksHandler extends APIHandler{

    private static GoogleBooksHandler mInstance = null;

    public static GoogleBooksHandler getInstance(){
        if(mInstance == null)
            mInstance = new GoogleBooksHandler();
        return mInstance;
    }

    // mActivity (from APIHandler) is guaranteed to be non-null
    // when the methods below are called
    protected void onStart(String id) {
        
    }

    protected void onProgress(String id, int percent) {
        
    }

    protected void onFailure(String id, StatusLine reason) {
        
    }

    protected void onException(String id, Exception exc) {
        exc.printStackTrace();
    }

    protected void onSuccess(String isbn, String res){
        JSONObject translated = GoogleBooksAPIClient
                                    .translateJsonResponse(isbn, res);
        mActivity.gotBibInfo(isbn, translated);
    }
}
