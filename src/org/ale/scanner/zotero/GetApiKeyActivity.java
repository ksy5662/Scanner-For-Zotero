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

package org.ale.scanner.zotero;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class GetApiKeyActivity extends Activity {
    public static final String CLASS_TAG = GetApiKeyActivity.class.getCanonicalName();

    /* Javascript to extract keys from the /settings/keys page */ 
    private static final String JS_KEY_SCRAPE =
        "javascript:window.KEYSCRAPE.foundKeys(" +
        "(function tostr(table) { " +
            "var res = []; " +
            "for(var i=1, len = table.rows.length; i < len; i++) { " +
                "res.push(table.rows[i].cells[0].innerHTML + '|' + table.rows[i].cells[2].childNodes[0]); " +
            "} return res; })" +
        "(document.getElementById('api-keys-table')).join(','));";

    /* Intent extras */
    public static final String ACCOUNT = "ACCOUNT";

    /* Keys for instance state */
    public static final String RECREATE_FOUND_NAMES = "RENAME";
    public static final String RECREATE_FOUND_IDS = "REID";
    public static final String RECREATE_FOUND_KEYS = "REKEY";
    public static final String RECREATE_LOGIN = "RELOG";

    /* Zotero URLs */
    public static final String URL_LOGIN = "zotero.org/user/login/";
    public static final String URL_KEYS = "zotero.org/settings/keys";

    /* State */
    private boolean mLoggedIn = false;

    protected ArrayList<String> mFoundNames;
    protected ArrayList<String> mFoundIDs;
    protected ArrayList<String> mFoundKeys;
    
    /* Dialogs */
    private AlertDialog mAlertDialog = null;
    private ProgressDialog mProgressDialog = null;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.apikeywebview);

        if (state != null){
           mFoundNames = state.getStringArrayList(RECREATE_FOUND_NAMES);
           mFoundIDs = state.getStringArrayList(RECREATE_FOUND_IDS);
           mFoundKeys = state.getStringArrayList(RECREATE_FOUND_KEYS);
           mLoggedIn = state.getBoolean(RECREATE_LOGIN);
        }

        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setCancelable(false);

        WebView wv = (WebView) findViewById(R.id.webView);
        wv.setWebViewClient(getWebViewClient());
        wv.setWebChromeClient(getWebChromeClient());
        wv.addJavascriptInterface(new KeyScraper(), "KEYSCRAPE");

        WebSettings settings = wv.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setBlockNetworkImage(true);

        if(!mLoggedIn){
            wv.loadUrl("https://"+URL_LOGIN);
        }else{
            wv.loadUrl("https://"+URL_KEYS);
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

        if(mAlertDialog == null){
            // Display any dialogs we were displaying before being destroyed
            switch(Dialogs.displayedDialog) {
            case(Dialogs.DIALOG_NO_DIALOG):
                break;
            case(Dialogs.DIALOG_SSL):
                mAlertDialog = Dialogs.showSSLDialog(GetApiKeyActivity.this);
                break;
            case(Dialogs.DIALOG_NO_KEYS):
                mAlertDialog = Dialogs.showNoKeysDialog(GetApiKeyActivity.this);
                break;
            case(Dialogs.DIALOG_FOUND_KEYS):
                if(mFoundNames != null && mFoundIDs != null && mFoundKeys != null){
                    mAlertDialog = Dialogs.showSelectKeyDialog(
                                            GetApiKeyActivity.this,
                                            mFoundNames, mFoundIDs, mFoundKeys);
                }
                break;
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putStringArrayList(RECREATE_FOUND_NAMES, mFoundNames);
        outState.putStringArrayList(RECREATE_FOUND_IDS, mFoundIDs);
        outState.putStringArrayList(RECREATE_FOUND_KEYS, mFoundKeys);
        outState.putBoolean(RECREATE_LOGIN, mLoggedIn);
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
        mAlertDialog = Dialogs.showSSLDialog(GetApiKeyActivity.this);
        return true;
    }

    /* Web view and web chrome client initialization */
    private WebViewClient getWebViewClient(){
        final GetApiKeyActivity parent = this;

        return new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url){
                boolean isLoginPage = url.indexOf(URL_LOGIN) > 0;
                boolean isKeyPage = url.indexOf(URL_KEYS) > 0;
                boolean isZotero = url.indexOf("zotero.org") > 0;

                if(!isZotero) { // Keep them from leaving the Zotero site.
                    Toast.makeText(parent,
                            "This browser session is restricted to Zotero.org",
                            Toast.LENGTH_LONG).show();
                    return true;
                }else if(!mLoggedIn && !isLoginPage){
                    mLoggedIn = true;
                    if(!isKeyPage) {
                        view.loadUrl("https://"+URL_KEYS);
                        return true;
                    }
                }
                return false;
            }

            @Override
            public void onPageFinished(WebView view, String url)  
            {
                if(url.indexOf(URL_KEYS) > 0) {
                    view.loadUrl("javascript:window.scrollTo(0, document.getElementById('content').offsetTop);");
                    // XXX: condition causes js to load on key edit page as well  
                    view.loadUrl(JS_KEY_SCRAPE);
                }else if(url.indexOf(URL_LOGIN) > 0) {
                    mLoggedIn = false;
                    view.loadUrl("javascript:document.getElementById('username').focus()");
                }
            }
        };
    }
    
    private WebChromeClient getWebChromeClient(){
        final ProgressDialog progressDialog = mProgressDialog;
        return new WebChromeClient() {
            public void onProgressChanged(WebView view, int progress) {
                progressDialog.setProgress(progress);

                if(!progressDialog.isShowing()){
                    progressDialog.show();
                }else if(progress == 100){
                    progressDialog.dismiss();
                }
            }
        };
    }

    /* Javascript Interface */
    private class KeyScraper {
        @SuppressWarnings("unused")
        public void foundKeys(String result) { // This runs outside the main thread!
            mJSHandler.sendMessage(
                    Message.obtain(mJSHandler, 0, result));
        }
    }

    private final Handler mJSHandler = new Handler(){
        public void handleMessage(Message msg){
            // We're going to display a dialog so dismiss any existing ones
            if(mAlertDialog != null){
                mAlertDialog.dismiss();
                mAlertDialog = null;
            }

            // Check if any keys were found
            String[] keyrows = ((String)msg.obj).split(",");
            if(TextUtils.isEmpty(keyrows[0])){
                mAlertDialog = Dialogs.showNoKeysDialog(GetApiKeyActivity.this);
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
            mAlertDialog = Dialogs.showSelectKeyDialog(GetApiKeyActivity.this,
                                        mFoundNames, mFoundIDs, mFoundKeys);
        }
    };
}
