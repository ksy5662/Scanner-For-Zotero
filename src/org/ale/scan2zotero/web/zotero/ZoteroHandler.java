package org.ale.scan2zotero.web.zotero;

import org.ale.scan2zotero.data.Access;
import org.ale.scan2zotero.data.Account;
import org.ale.scan2zotero.data.Database;
import org.ale.scan2zotero.data.Group;
import org.ale.scan2zotero.web.APIHandler;
import org.apache.http.StatusLine;

import android.content.ContentResolver;
import android.util.Log;
import android.widget.Toast;

public class ZoteroHandler extends APIHandler {

    // Inherits from APIHandler
    // protected ArrayList<Integer> mResponseTypes;
    // protected ArrayList<APIResponse> mResponses;
    // protected ArrayList<Runnable> mUIThreadEvents;

    private static ZoteroHandler mInstance = null;

    public static ZoteroHandler getInstance(){
        if(mInstance == null){
            mInstance = new ZoteroHandler();
        }
        return mInstance;
    }

    protected void dequeueMessages(){
        for(int i=0; i<mResponses.size(); i++){
            handleMessage(mResponseTypes.get(i).intValue(), mResponses.get(i));
        }
        mResponseTypes.clear();
        mResponses.clear();
    }

    // APIHandler.mActivity is guaranteed to be non-null
    // when the methods below are called
    protected void onStart(String id) {

    }

    protected void onProgress(String id, int percent) {

    }

    protected void onStatusLine(String id, StatusLine status) {
        switch(status.getStatusCode()){
        case 403: // Forbidden
            if(!id.equals(ZoteroAPIClient.PERMISSIONS)) {
                // Maybe the key permissions changed, do a refresh
                APIHandler.mActivity.refreshPermissions();
            }else{
                // This shouldn't happen, but to avoid a permission checking
                // loop in case it does, erase key permissions and log out
                APIHandler.mActivity.erasePermissions();
                APIHandler.mActivity.postAccountPermissions(null);
            }
            break;
        case 404: // Not Found
            if(id.equals(ZoteroAPIClient.PERMISSIONS)){
                APIHandler.mActivity.erasePermissions();
                APIHandler.mActivity.postAccountPermissions(null);
            }
            break;
        case 409: // Conflict (Target library locked)
        case 412: // Precondition failed (X-Zotero-Write-Token duplicate)
        case 413: // Request Entity Too Large
            break;
        case 500: // Internal Server Error
        case 503: // Service Unavailable
            Toast.makeText(
                    APIHandler.mActivity, 
                    "Zotero server error, try again later.",
                    Toast.LENGTH_LONG).show();
            break;
        // We don't actually have to handle any of these, but I'm leaving them
        // here in case of future API changes.
        case 200: // OK
        case 201: // Created
        case 204: // No Content
        case 304: // Not Modified
        case 400: // Bad Request
        case 405: // Method Not Allowed
        default:
            break;
        }
    }

    protected void onException(String id, Exception exc) {
        //if(id.equals(ZoteroAPIClient.ITEMS))
        //    APIHandler.mActivity.postItemResponse(null);
        exc.printStackTrace();
        Toast.makeText(APIHandler.mActivity, id+" Exception", Toast.LENGTH_LONG).show();
    }

    protected void onSuccess(final String id, String resp) {
        if(id.equals(ZoteroAPIClient.ITEMS)){
            //handleItems(resp);
        }else if(id.equals(ZoteroAPIClient.PERMISSIONS)){
            handlePermissions(resp);
        }else if(id.equals(ZoteroAPIClient.GROUPS)){
            handleGroups(resp);
        }/*else if(id.equals(ZoteroAPIClient.COLLECTIONS)){
            handleCollections(resp);
        }*/
    }

    private void handlePermissions(final String xml){
        final ContentResolver cr = APIHandler.mActivity.getContentResolver();
        final Account user = APIHandler.mActivity.getUserAccount();

        new Thread(new Runnable(){
            public void run() {
                final Access perms = ZoteroAPIClient.parsePermissions(xml, user);
                if(perms != null) {
                    cr.delete(Database.ACCESS_URI, Access.COL_ACCT + "=?",
                            new String[] { String.valueOf(user.getDbId()) });
                    perms.writeToDB(cr);

                    checkActivityAndRun(new Runnable(){
                        public void run(){
                            APIHandler.mActivity.postAccountPermissions(perms);
                        }
                    });
                }else{
                    // TODO: Garbled XML? Tell the user
                }
            }
        }).start();
    }

    private void handleGroups(final String xml){
        // Write responses to database and be done with it.
        final ContentResolver cr = APIHandler.mActivity.getContentResolver();
        new Thread(new Runnable(){
            public void run() {
                final Group[] groups = ZoteroAPIClient.parseGroups(xml);
                int howMany = 0;
                if(groups != null){
                    howMany = groups.length;
                    for(int i=0; i<howMany; i++){
                        groups[i].writeToDB(cr);
                    }
                    checkActivityAndRun(new Runnable(){
                        public void run(){
                            APIHandler.mActivity.loadGroups();
                        }
                    });
                }
            }
        }).start();
    }

    /*private void handleItems(final String xml){
        final ContentResolver cr = APIHandler.mActivity.getContentResolver();
        new Thread(new Runnable(){
            public void run() {
                String itemIds = ZoteroAPIClient.parseItems(xml);
            }
        }).start();
    }*/
}
