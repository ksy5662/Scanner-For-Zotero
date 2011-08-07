package org.ale.scan2zotero.web;

import java.util.ArrayList;

import org.ale.scan2zotero.PendingListAdapter;
import org.ale.scan2zotero.web.APIRequest.APIResponse;
import org.apache.http.StatusLine;
import org.json.JSONObject;

public class GoogleBooksHandler extends APIHandler{

    protected static ArrayList<Integer> mResponseTypes = new ArrayList<Integer>();
    protected static ArrayList<APIResponse> mResponses = new ArrayList<APIResponse>();

    private static GoogleBooksHandler mInstance = null;

    public static GoogleBooksHandler getInstance(){
        if(mInstance == null)
            mInstance = new GoogleBooksHandler();
        return mInstance;
    }

    protected void dequeueMessages(){
        for(int i=0; i<mResponses.size(); i++){
            handleMessage(mResponseTypes.get(i).intValue(), mResponses.get(i));
        }
        mResponseTypes.clear();
        mResponses.clear();
    }

    // mActivity (from APIHandler) is guaranteed to be non-null
    // when the methods below are called
    protected void onStart(String id) {
        
    }

    protected void onProgress(String id, int percent) {
        
    }

    protected void onFailure(String id, StatusLine reason) {
        APIHandler.mActivity.itemFailed(id, PendingListAdapter.STATUS_FAILED);
    }

    protected void onException(String id, Exception exc) {
        exc.printStackTrace();
        //TODO: Be more helpful here, might not be a network issue
        APIHandler.mActivity.itemFailed(id, PendingListAdapter.STATUS_NO_NETWORK);
    }

    protected void onSuccess(String isbn, String res){
        JSONObject translated = GoogleBooksAPIClient
                                    .translateJsonResponse(isbn, res);
        APIHandler.mActivity.gotBibInfo(isbn, translated);
    }
}
