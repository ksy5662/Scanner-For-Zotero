/** 
 * Copyright 2011 John M. Schanck
 * 
 * Scan2Zotero is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Scan2Zotero is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Scan2Zotero.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.ale.scan2zotero;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
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

    public static final String CLASS_TAG = GetApiKeyActivity.class.getCanonicalName();

    public static final String LOGIN_TYPE = "LOGIN_TYPE";

    public static final String ACCOUNT = "ACCOUNT";

    public static final String RECREATE_FOUND_NAMES = "RENAME";
    public static final String RECREATE_FOUND_IDS = "REID";
    public static final String RECREATE_FOUND_KEYS = "REKEY";

    private static final int JS_FOUND_KEYS = 0;

    private AlertDialog mAlertDialog = null;

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

        // The images are barely noticeable during this so we don't need them
        wv.getSettings().setBlockNetworkImage(true);

        // TODO: extraHeaders doesn't exist on versions <= 2.1 - try to find workaround
        Map<String, String> extraHeaders = new HashMap<String, String>();
        extraHeaders.put("Referer", "https://zotero.org/settings/keys");
        wv.getSettings().setJavaScriptEnabled(true);
        wv.addJavascriptInterface(new KeyScraper(), "KEYSCRAPE");
        // Cause dialog to display in onResume
        wv.loadUrl("https://zotero.org/user/login/", extraHeaders);
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
        mAlertDialog = Dialogs.showSSLDialog(GetApiKeyActivity.this);
        return true;
    }

    private WebViewClient getWebViewClient(){
        return new WebViewClient() {
            // TODO: Block non-zotero.org sites or maybe white-list just the pages
            // we need.
            @Override
            public void onPageFinished(WebView view, String url)  
            {
                if(url.indexOf("zotero.org/settings/keys") > 0) {
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
                break;
            }
        }
    };
}
