package org.ale.scan2zotero.web.googlebooks;

import org.ale.scan2zotero.PendingListAdapter;
import org.ale.scan2zotero.web.APIHandler;
import org.ale.scan2zotero.web.APIRequest;
import org.apache.http.StatusLine;
import org.json.JSONObject;

public class GoogleBooksHandler extends APIHandler{

    private static GoogleBooksHandler mInstance = null;

    public static GoogleBooksHandler getInstance(){
        if(mInstance == null)
            mInstance = new GoogleBooksHandler();
        return mInstance;
    }

    // APIHandler.MAIN is guaranteed to be non-null
    // when these methods are called
    protected void onStart(APIRequest req) {
        
    }

    protected void onProgress(APIRequest req, int percent) {
        
    }

    protected void onStatusLine(APIRequest req, StatusLine status) {
        String id = req.getExtra().getString(GoogleBooksAPIClient.EXTRA_ISBN);
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
            APIHandler.MAIN.itemFailed(id, errReason);
        }
    }

    protected void onException(APIRequest req, Exception exc) {
        String id = req.getExtra().getString(GoogleBooksAPIClient.EXTRA_ISBN);
        exc.printStackTrace();
        //TODO: Be more helpful here, might not be a network issue
        APIHandler.MAIN.itemFailed(id, PendingListAdapter.STATUS_NO_NETWORK);
    }

    protected void onSuccess(APIRequest req, final String res){
        final String id = req.getExtra().getString(GoogleBooksAPIClient.EXTRA_ISBN);
        new Thread(new Runnable() {
            public void run(){
                // Extract bibliographic information from Google's response and
                // put it in a format we can submit to Zotero later.
                final JSONObject translated =
                        GoogleBooksAPIClient.translateJsonResponse(id, res);

                // Since that might have taken some time, check that the 
                // activity is still around and post the BibInfo.
                checkActivityAndRun(new Runnable(){
                    public void run(){
                        if(translated == null){
                            APIHandler.MAIN.itemFailed(id, PendingListAdapter.STATUS_FAILED);
                        }else{
                            APIHandler.MAIN.gotBibInfo(id, translated);
                        }
                    }
                });
            }
        }).start();
    }
}
