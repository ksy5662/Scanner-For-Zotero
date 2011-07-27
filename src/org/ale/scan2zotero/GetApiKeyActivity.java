package org.ale.scan2zotero;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class GetApiKeyActivity extends Activity {

    private static final String JS_KEY_SCRAPE =
        "javascript:window.KEYSCRAPE.foundKeys(" +
        "(function tostr(table) { " +
            "var res = []; " +
            "for(var i=1, len = table.rows.length; i < len; i++) { " +
                "res.push(table.rows[i].cells[0].innerHTML + '|' + table.rows[i].cells[2].childNodes[0]); " +
            "} return res; })" +
        "(document.getElementById('api-keys-table')).join(','));";

    private static final String JS_LOGGED_IN =
        "javascript:window.KEYSCRAPE.checkLogin(" +
        "document.getElementById('login-links')" +
        ".innerHTML.search('Inbox'));";

    public static final String CLASS_TAG = GetApiKeyActivity.class.getCanonicalName();

    public static final String LOGIN_TYPE = "LOGIN_TYPE";

    public static final int EXISTING_ACCOUNT = 0;
    public static final int NEW_ACCOUNT = 1;

    public static final String ACCOUNT = "ACCOUNT";

    public static final String RECREATE_FOUND_NAMES = "RENAME";
    public static final String RECREATE_FOUND_IDS = "REID";
    public static final String RECREATE_FOUND_KEYS = "REKEY";

    private static final int JS_FOUND_KEYS = 0;
    private static final int JS_CHECK_LOGIN = 1;

    private AlertDialog mAlertDialog = null;

    private static boolean mLoggedIn = false;

    protected ArrayList<String> mFoundNames;
    protected ArrayList<String> mFoundIDs;
    protected ArrayList<String> mFoundKeys;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.apikeywebview);

        WebView wv = (WebView) findViewById(R.id.webView);

        if (state != null){
           wv.restoreState(state);
           mFoundNames = state.getStringArrayList(RECREATE_FOUND_NAMES);
           mFoundIDs = state.getStringArrayList(RECREATE_FOUND_IDS);
           mFoundKeys = state.getStringArrayList(RECREATE_FOUND_KEYS);
        } else {
            wv.setWebViewClient(getWebViewClient());
        }

        Bundle extras = getIntent().getExtras();
        if(extras.getInt(LOGIN_TYPE, EXISTING_ACCOUNT) == EXISTING_ACCOUNT){
            // The images are barely noticeable during this so we don't need them
            wv.getSettings().setBlockNetworkImage(true);

            // TODO: extreaHeaders doesn't exist on versions <= 2.1 - try to find workaround
            Map<String, String> extraHeaders = new HashMap<String, String>();
            extraHeaders.put("Referer", "https://zotero.org/settings/keys");
            wv.getSettings().setJavaScriptEnabled(true);
            wv.addJavascriptInterface(new KeyScraper(), "KEYSCRAPE");
            if(mLoggedIn){
                wv.loadUrl("https://zotero.org/settings/keys");
            }else{
                // Cause dialog to display in onResume
                S2ZDialogs.displayedDialog = S2ZDialogs.DIALOG_CHECKING_LOGIN;
                wv.loadUrl("https://zotero.org/user/login/", extraHeaders);
            }
        }else{
            // Have to do a captcha :(
            wv.getSettings().setBlockNetworkImage(false);

            wv.loadUrl("https://zotero.org/user/register/");
        }
    }

    @Override
    public void onPause(){
        super.onPause();
        if(mAlertDialog != null){
            mAlertDialog.dismiss();
            mAlertDialog = null;
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // Display any dialogs we were displaying before being destroyed
        switch(S2ZDialogs.displayedDialog) {
        case(S2ZDialogs.DIALOG_NO_DIALOG):
            break;
        case(S2ZDialogs.DIALOG_SSL):
            mAlertDialog = S2ZDialogs.showSSLDialog(GetApiKeyActivity.this);
            break;
        case(S2ZDialogs.DIALOG_EMAIL_VERIFY):
            mAlertDialog = S2ZDialogs.showEmailValidationDialog(GetApiKeyActivity.this);
            break;
        case(S2ZDialogs.DIALOG_NO_KEYS):
            mAlertDialog = S2ZDialogs.showNoKeysDialog(GetApiKeyActivity.this);
            break;
        case(S2ZDialogs.DIALOG_FOUND_KEYS):
            if(mFoundNames != null && mFoundIDs != null && mFoundKeys != null){
                mAlertDialog = S2ZDialogs.showSelectKeyDialog(
                                        GetApiKeyActivity.this,
                                        mFoundNames, mFoundIDs, mFoundKeys);
            }
            break;
        case(S2ZDialogs.DIALOG_CHECKING_LOGIN):
            mAlertDialog = ProgressDialog.show(GetApiKeyActivity.this,
                            "Please wait",
                            "Checking for existing session.", false, true);
            break;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //((WebView)findViewById(R.id.webView)).saveState(outState);
        outState.putStringArrayList(RECREATE_FOUND_NAMES, mFoundNames);
        outState.putStringArrayList(RECREATE_FOUND_IDS, mFoundIDs);
        outState.putStringArrayList(RECREATE_FOUND_KEYS, mFoundKeys);
    }

    /* Options menu -- entirely for SSL at the moment */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu_webview, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() != R.id.ctx_webview_showssl){
            return super.onOptionsItemSelected(item);
        }
        mAlertDialog = S2ZDialogs.showSSLDialog(GetApiKeyActivity.this);
        return true;
    }

    private WebViewClient getWebViewClient(){
        return new WebViewClient() {
            // TODO: Block non-zotero.org sites or maybe white-list just the pages
            // we need.
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon){
                if(url.indexOf("zotero.org/settings/keys") > 0) {
                    mLoggedIn = true;
                    if(S2ZDialogs.displayedDialog == S2ZDialogs.DIALOG_CHECKING_LOGIN){
                        S2ZDialogs.displayedDialog = S2ZDialogs.DIALOG_NO_DIALOG;
                        if(mAlertDialog != null){
                            mAlertDialog.dismiss();
                            mAlertDialog = null;
                        }
                    }
                }
            }
            @Override  
            public boolean shouldOverrideUrlLoading(WebView view, String url)  
            {  
                if (url.indexOf("zotero.org/user/logout") > 0) { 
                    mLoggedIn = false;
                    return false; // Let it load
                } else if(url.indexOf("zotero.org/user/validate") > 0) {
                    mAlertDialog = S2ZDialogs.showEmailValidationDialog(GetApiKeyActivity.this);
                    return true;
                }
                return false;  
            }
            @Override
            public void onPageFinished(WebView view, String url)  
            {
                if(url.indexOf("zotero.org/user/login") > 0) {
                    view.loadUrl(JS_LOGGED_IN);
                }else if(url.indexOf("zotero.org/settings/keys") > 0) {
                    view.loadUrl(JS_KEY_SCRAPE);
                }
            }
        };
    }

    /* Javascript Interface */
    private class KeyScraper {
        @SuppressWarnings("unused")
        public void foundKeys(String result) { // This runs outside the main thread!
            mJSHandler.sendMessage(
                    Message.obtain(mJSHandler, JS_FOUND_KEYS, result));
        }
        @SuppressWarnings("unused")
        public void checkLogin(String result) { // This runs outside the main thread!
            mJSHandler.sendMessage(
                    Message.obtain(mJSHandler, JS_CHECK_LOGIN, result));
        }
    }

    private final Handler mJSHandler = new Handler(){
        public void handleMessage(Message msg){
            switch(msg.what){
            case JS_FOUND_KEYS:
                // We're going to display a dialog so dismiss any existing ones
                if(mAlertDialog != null){
                    mAlertDialog.dismiss();
                    mAlertDialog = null;
                }

                // Check if any keys were found
                String[] keyrows = ((String)msg.obj).split(",");
                if(TextUtils.isEmpty(keyrows[0])){
                    mAlertDialog = S2ZDialogs.showNoKeysDialog(GetApiKeyActivity.this);
                    return;
                }

                // Prepare to prompt user to select a key
                mFoundNames = new ArrayList<String>(keyrows.length);
                mFoundIDs = new ArrayList<String>(keyrows.length);
                mFoundKeys = new ArrayList<String>(keyrows.length);
                for (int i=0; i < keyrows.length; i++){
                    int split = keyrows[i].indexOf("|");
                    if(split > 0){
                        String name = keyrows[i].substring(0, split);
                        String struri = keyrows[i].substring(split+1);
                        String userId = null;
                        String apiKey = null;
                        try{
                            Uri uri = Uri.parse(struri);
                            userId = uri.getPath().split("/")[2];
                            apiKey = uri.getQueryParameter("key");
                        }catch(Exception e) {
                            continue;
                        }
                        mFoundNames.add(name);
                        mFoundIDs.add(userId);
                        mFoundKeys.add(apiKey);
                    }
                }
                mAlertDialog = S2ZDialogs.showSelectKeyDialog(GetApiKeyActivity.this,
                                            mFoundNames, mFoundIDs, mFoundKeys);
                break;
            case JS_CHECK_LOGIN:
                if(!((String)msg.obj).equals("-1")){
                    ((WebView) findViewById(R.id.webView))
                        .loadUrl("https://zotero.org/settings/keys");
                } else {
                    if(S2ZDialogs.displayedDialog == S2ZDialogs.DIALOG_CHECKING_LOGIN){
                        S2ZDialogs.displayedDialog = S2ZDialogs.DIALOG_NO_DIALOG;
                        if(mAlertDialog != null){
                            mAlertDialog.dismiss();
                            mAlertDialog = null;
                        }
                    }
                }
                break;
            }
        }
    };
}
