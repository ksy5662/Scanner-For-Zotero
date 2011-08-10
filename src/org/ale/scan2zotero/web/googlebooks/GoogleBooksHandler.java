package org.ale.scan2zotero.web.googlebooks;

import org.ale.scan2zotero.PendingListAdapter;
import org.ale.scan2zotero.web.APIHandler;
import org.apache.http.StatusLine;
import org.json.JSONObject;

public class GoogleBooksHandler extends APIHandler{

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

    public boolean continueAfterStatus(int code){
        return true;
    }

    // APIHandler.mActivity is guaranteed to be non-null
    // when these methods are called
    protected void onStart(String id) {
        
    }

    protected void onProgress(String id, int percent) {
        
    }

    protected void onStatusLine(String id, StatusLine status) {
        int statusCode = status.getStatusCode();
        if(statusCode >= 400) {
            int errReason;
            switch(statusCode){
            case 400:
                errReason = PendingListAdapter.STATUS_BAD_REQUEST;
                break;
            case 403:
                errReason = PendingListAdapter.STATUS_QUOTA_EXCEEDED;
                break;
            case 500:
                errReason = PendingListAdapter.STATUS_SERVER_ERROR;
                break;
            default:
                errReason = PendingListAdapter.STATUS_FAILED;
                break;
            }
            APIHandler.mActivity.itemFailed(id, errReason);
        }
    }

    protected void onException(String id, Exception exc) {
        exc.printStackTrace();
        //TODO: Be more helpful here, might not be a network issue
        APIHandler.mActivity.itemFailed(id, PendingListAdapter.STATUS_NO_NETWORK);
    }

    protected void onSuccess(final String isbn, final String res){
        new Thread(new Runnable() {
            public void run(){
                // Extract bibliographic information from Google's response and
                // put it in a format we can submit to Zotero later.
                final JSONObject translated =
                        GoogleBooksAPIClient.translateJsonResponse(isbn, res);

                // Since that might have taken some time, check that the 
                // activity is still around and post the BibInfo.
                checkActivityAndRun(new Runnable(){
                    public void run(){
                        if(translated == null){
                            APIHandler.mActivity.itemFailed(isbn, PendingListAdapter.STATUS_FAILED);
                        }else{
                            APIHandler.mActivity.gotBibInfo(isbn, translated);
                        }
                    }
                });
            }
        }).start();
    }
}
