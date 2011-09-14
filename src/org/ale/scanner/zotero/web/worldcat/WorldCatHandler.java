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

import org.ale.scanner.zotero.PendingListAdapter;
import org.ale.scanner.zotero.web.APIHandler;
import org.ale.scanner.zotero.web.APIRequest;
import org.apache.http.StatusLine;
import org.json.JSONObject;

public class WorldCatHandler extends APIHandler{

    private static WorldCatHandler mInstance = null;

    public static WorldCatHandler getInstance(){
        if(mInstance == null){
            mInstance = new WorldCatHandler();
            APIHandler.HANDLERS.add(mInstance);
        }
        return mInstance;
    }

    // APIHandler.MAIN is guaranteed to be non-null
    // when these methods are called
    protected void onStart(APIRequest req) {
        
    }

    protected void onProgress(APIRequest req, int percent) {
        
    }

    protected void onStatusLine(APIRequest req, StatusLine status) {
        String id = req.getExtra().getString(WorldCatAPIClient.EXTRA_ISBN);
        int statusCode = status.getStatusCode();
        if(statusCode >= 400) {
            int errReason;
            switch(statusCode){
            case 400:
                errReason = PendingListAdapter.STATUS_BAD_REQUEST;
                break;
            case 500:
                errReason = PendingListAdapter.STATUS_SERVER_ERROR;
                break;
            default:
                errReason = PendingListAdapter.STATUS_FAILED;
                break;
            }
            APIHandler.MAIN.bibFetchFailure(id, errReason);
        }
    }

    protected void onException(APIRequest req, Exception exc) {
        String id = req.getExtra().getString(WorldCatAPIClient.EXTRA_ISBN);
        exc.printStackTrace();
        //TODO: Be more helpful here, might not be a network issue
        APIHandler.MAIN.bibFetchFailure(id, PendingListAdapter.STATUS_NO_NETWORK);
    }

    protected void onSuccess(APIRequest req, final String resp){
        final String id = req.getExtra().getString(WorldCatAPIClient.EXTRA_ISBN);
        new Thread(new Runnable() {
            public void run(){
                JSONObject jsonresp = WorldCatAPIClient.strToJSON(resp);
                int status = WorldCatAPIClient.getStatus(jsonresp);
                JSONObject tmpTrans = WorldCatAPIClient.translateJsonResponse(id, jsonresp);
                boolean failed = (status != WorldCatAPIClient.STATUS_OK)
                                    && (tmpTrans != null);
                
                Runnable toPost;
                // Since that might have taken some time, check that the 
                // activity is still around and post the BibInfo.
                if(failed){
                    final int reason;
                    switch(status){
                    case WorldCatAPIClient.STATUS_OVER_LIMIT:
                        reason = PendingListAdapter.STATUS_QUOTA_EXCEEDED;
                        break;
                    case WorldCatAPIClient.STATUS_INVALID:
                        reason = PendingListAdapter.STATUS_BAD_REQUEST;
                        break;
                    case WorldCatAPIClient.STATUS_NOT_FOUND:
                        reason = PendingListAdapter.STATUS_NOT_FOUND;
                        break;
                    default:
                        reason = PendingListAdapter.STATUS_FAILED;
                        break;
                    }
                    toPost = new Runnable(){
                                 public void run(){
                                     APIHandler.MAIN.bibFetchFailure(id, reason);
                                 }
                             };
                }else{
                    final JSONObject translated = tmpTrans;
                    toPost = new Runnable(){
                                 public void run(){
                                     APIHandler.MAIN.bibFetchSuccess(id, translated);
                                 }
                             };
                }
                checkActivityAndRun(toPost);
            }
        }).start();
    }
}
