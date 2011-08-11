package org.ale.scan2zotero.web.zotero;

import org.ale.scan2zotero.data.Access;
import org.ale.scan2zotero.data.Account;
import org.ale.scan2zotero.data.BibItem;
import org.ale.scan2zotero.data.Database;
import org.ale.scan2zotero.data.Group;
import org.ale.scan2zotero.web.APIHandler;
import org.ale.scan2zotero.web.APIRequest;
import org.apache.http.StatusLine;

import android.content.ContentResolver;
import android.database.Cursor;
import android.text.TextUtils;
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

    // APIHandler.MAIN is guaranteed to be non-null
    // when the methods below are called
    protected void onStart(APIRequest req) {

    }

    protected void onProgress(APIRequest req, int percent) {

    }

    protected void onStatusLine(APIRequest req, StatusLine status) {
        int rt = req.getExtra().getInt(ZoteroAPIClient.EXTRA_REQ_TYPE);
        switch(status.getStatusCode()){
        case 403: // Forbidden
            if(rt != ZoteroAPIClient.PERMISSIONS) {
                // Maybe the key permissions changed, do a refresh
                APIHandler.MAIN.refreshPermissions();
            }else{
                // This shouldn't happen, but to avoid a permission checking
                // loop in case it does, erase key permissions and log out
                APIHandler.MAIN.erasePermissions();
                APIHandler.MAIN.postAccountPermissions(null);
            }
            break;
        case 404: // Not Found
            if(rt == ZoteroAPIClient.PERMISSIONS) {
                // The key wasn't found, wipe it out.
                APIHandler.MAIN.erasePermissions();
                APIHandler.MAIN.postAccountPermissions(null);
            }
        case 409: // Conflict (Target library locked)
        case 412: // Precondition failed (X-Zotero-Write-Token duplicate)
        case 413: // Request Entity Too Large
            break;
        case 500: // Internal Server Error
        case 503: // Service Unavailable
            Toast.makeText(
                    APIHandler.MAIN, 
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

    protected void onException(APIRequest req, Exception exc) {
        int rt = req.getExtra().getInt(ZoteroAPIClient.EXTRA_REQ_TYPE);
        //if(rt == ZoteroAPIClient.ITEMS)
        //    APIHandler.MAIN.postItemResponse(null);
        exc.printStackTrace();
        Toast.makeText(APIHandler.MAIN, rt+" Exception", Toast.LENGTH_LONG).show();
    }

    protected void onSuccess(APIRequest req, String resp) {
        int rt = req.getExtra().getInt(ZoteroAPIClient.EXTRA_REQ_TYPE);
        switch(rt){
        case ZoteroAPIClient.COLLECTIONS:
            break;
        case ZoteroAPIClient.GROUPS:
            handleGroups(resp);
            break;
        case ZoteroAPIClient.ITEMS:
            int[] rows = req.getExtra().getIntArray(ZoteroAPIClient.EXTRA_ITEM_ROWS);
            handleItems(rows, resp);
            break;
        case ZoteroAPIClient.PERMISSIONS:
            handlePermissions(resp);
            break;
        }
    }

    private void handlePermissions(final String xml){
        final ContentResolver cr = APIHandler.MAIN.getContentResolver();
        final Account user = APIHandler.MAIN.getUserAccount();

        new Thread(new Runnable(){
            public void run() {
                final Access perms = ZoteroAPIClient.parsePermissions(xml, user);
                if(perms != null) {
                    cr.delete(Database.ACCESS_URI, Access.COL_ACCT + "=?",
                            new String[] { String.valueOf(user.getDbId()) });
                    perms.writeToDB(cr);

                    checkActivityAndRun(new Runnable(){
                        public void run(){
                            APIHandler.MAIN.postAccountPermissions(perms);
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
        final ContentResolver cr = APIHandler.MAIN.getContentResolver();
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
                            APIHandler.MAIN.loadGroups();
                        }
                    });
                }
            }
        }).start();
    }

    private void handleItems(final int[] dbrows, final String xml){
        new Thread(new Runnable(){
            public void run() {
                final String itemIds = ZoteroAPIClient.parseItems(xml);
                String[] srows = new String[dbrows.length];
                for(int i=0; i<srows.length; i++){
                    srows[i] = String.valueOf(dbrows[i]);
                }
                checkActivityAndRun(new Runnable(){
                    public void run(){
                        APIHandler.MAIN.itemsUploaded(dbrows);
                    }
                });
            }
        }).start();
    }
}
