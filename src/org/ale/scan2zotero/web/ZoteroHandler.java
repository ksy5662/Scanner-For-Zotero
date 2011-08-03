package org.ale.scan2zotero.web;

import org.apache.http.StatusLine;

public class ZoteroHandler extends APIHandler {

    private static ZoteroHandler mInstance = null;

    public static ZoteroHandler getInstance(){
        if(mInstance == null)
            mInstance = new ZoteroHandler();
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

    protected void onSuccess(String id, String res) {

    }
}
