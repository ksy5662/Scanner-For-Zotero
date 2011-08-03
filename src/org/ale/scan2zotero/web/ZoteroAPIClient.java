package org.ale.scan2zotero.web;

import java.net.URI;
import java.util.Random;

import org.ale.scan2zotero.data.Account;
import org.apache.http.impl.client.DefaultHttpClient;

public class ZoteroAPIClient {
    private static final boolean DEBUG = true;

    //private static final String CLASS_TAG = ZoteroAPIClient.class.getCanonicalName();

    private static final String ZOTERO_BASE_URL = DEBUG ? "http://10.13.37.64/" : "https://api.zotero.org/";
    private static final String ZOTERO_USERS_URL = ZOTERO_BASE_URL + "users/";
    private static final String ZOTERO_GROUPS_URL = ZOTERO_BASE_URL + "groups/";

    private Account mAccount;

    private DefaultHttpClient mHttpsClient;

    private RequestQueue mRequestQueue;

    private ZoteroHandler mHandler;

    public ZoteroAPIClient() {
        mHandler = ZoteroHandler.getInstance();
        mHttpsClient = HttpsClient.getHttpsClientInstance();
        mRequestQueue = RequestQueue.getInstance();
    }

    public void setAccount(Account acct){
        mAccount = acct;
    }

    public void addItem(String jsonContent) {
        APIRequest r = new APIRequest(mHandler, mHttpsClient);
        r.setRequestType(APIRequest.POST);
        r.setURI(URI.create(ZOTERO_BASE_URL + "/users/" + mAccount.getUid() + "/items?key=" + mAccount.getKey()));
        r.setContent(jsonContent, "application/json");

        mRequestQueue.enqueue(r);
    }

    public void getUsersGroups() {
        APIRequest r = new APIRequest(mHandler, mHttpsClient);
        r.setRequestType(APIRequest.GET);
        r.setURI(URI.create(ZOTERO_USERS_URL + mAccount.getUid() + "/groups?key=" + mAccount.getKey()));

        mRequestQueue.enqueue(r);
    }

    public static String newWriteToken(){
        // Make 16 hex character write token
        Random rng = new Random();
        return Integer.toHexString(rng.nextInt()) + Integer.toHexString(rng.nextInt());
    }
}
