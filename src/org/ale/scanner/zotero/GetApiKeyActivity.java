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
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

public class GetApiKeyActivity extends Activity {
    public static final String CLASS_TAG = GetApiKeyActivity.class.getCanonicalName();

    /* Javascript to extract keys from the /settings/keys page */ 
    private static final String JS_KEY_SCRAPE =
        "javascript:window.KEYSCRAPE.foundKeys(" +
        "(function tostr(table) { " +
            "var res = []; " +
            "for(var i=1, len = table.rows.length; i < len; i++) { " +
                "res.push(table.rows[i].cells[0].innerHTML + '|' + " +
                "table.rows[i].cells[2].childNodes[0]); " +
            "} return res; })" +
        "(document.getElementById('api-keys-table')).join(','));";

    private static final String JS_SCROLL =
        "javascript:window.scrollTo" +
                    "(0, document.getElementById('content').offsetTop);";

    private static final String JS_FOCUS_UID = 
        "javascript:document.getElementById('username').focus()";

    private static final String JS_CHECK_WRITE = 
        "javascript:document.getElementById('write_access').checked=true";

    /* Intent extras */
    public static final String ACCOUNT = "ACCOUNT";

    /* Keys for instance state */
    public static final String RECREATE_FOUND_NAMES = "RENAME";
    public static final String RECREATE_FOUND_IDS = "REID";
    public static final String RECREATE_FOUND_KEYS = "REKEY";
    public static final String RECREATE_LOGIN = "RELOG";

    /* Zotero URLs */
    public static final String URL_PROTO = "https://";
    public static final String URL_HOST = "zotero.org";
    public static final String URL_PATH_LOGIN = "/user/login";
    public static final String URL_PATH_LOGOUT = "/user/logout";
    public static final String URL_PATH_LOSTPASS = "/user/lostpassword";
    public static final String URL_PATH_KEYS = "/settings/keys";
    public static final String URL_PATH_REGISTER = "/user/register";
    public static final String URL_PATH_EDIT_KEY = "/settings/keys/edit";
    public static final String URL_PATH_NEW_KEY = "/settings/keys/new";

    /* State */
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
        }

        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setCancelable(false);

        TextView help = (TextView) findViewById(R.id.help_text);
        help.setMovementMethod(LinkMovementMethod.getInstance());

        WebView wv = (WebView) findViewById(R.id.webView);
        wv.setWebViewClient(getWebViewClient());
        wv.setWebChromeClient(getWebChromeClient());
        wv.addJavascriptInterface(new KeyScraper(), "KEYSCRAPE");

        WebSettings settings = wv.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setBlockNetworkImage(true);

        wv.loadUrl(URL_PROTO+URL_HOST+URL_PATH_LOGIN);
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
    }

    @Override
    public void onBackPressed() {
        WebView wv = (WebView) findViewById(R.id.webView);
        Uri target = Uri.parse(wv.getUrl());

        String path = target.getPath();
        boolean isEditKey = path.startsWith(URL_PATH_EDIT_KEY);
        boolean isNewKey = path.startsWith(URL_PATH_NEW_KEY);
        if(isEditKey || isNewKey){
            wv.goBack();
        }else{
            super.onBackPressed();
        }

    }

    @Override
    public void onNewIntent(Intent intent){
        Uri uri = intent.getData();
        String scheme = uri.getScheme();
        String toLoad = uri.toString();
        toLoad = toLoad.replace(scheme, "https");
        WebView wv = (WebView) findViewById(R.id.webView);
        wv.loadUrl(toLoad);
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
        return new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url){
                Uri target = Uri.parse(url);

                // Check if we're on zotero.org (or a subdomain like www.)
                boolean isZotero = target.getHost().endsWith("zotero.org");
                if(!isZotero) {
                    // XXX: Allows user to go to arbitrary pages - we need this
                    // for OpenID, but perhaps we could check if they clicked
                    // the OpenID Login button before allowing them to leave
                    // zotero.org
                    return false;
                }
                
                // Check if the page is white-listed
                String path = target.getPath();
                boolean isWhiteListed = false;
                isWhiteListed |= path.startsWith(URL_PATH_LOGIN);
                isWhiteListed |= path.startsWith(URL_PATH_LOGOUT); 
                isWhiteListed |= path.startsWith(URL_PATH_KEYS);
                // Edit and new are covered by URL_PATH_KEYS
                isWhiteListed |= path.startsWith(URL_PATH_REGISTER);
                isWhiteListed |= path.startsWith(URL_PATH_LOSTPASS);

                // If we're not on a white-listed page, force the user
                // to URL_PATH_KEYS
                if (!isWhiteListed){
                    view.loadUrl(URL_PROTO + URL_HOST + URL_PATH_KEYS);
                    return true;
                }
                return false;
            }

            @Override
            public void onPageFinished(WebView view, String url)  
            {
                TextView help = (TextView)findViewById(R.id.help_text);
                help.setText("");

                Uri target = Uri.parse(url);

                String path = target.getPath();
                boolean isKeyList = path.startsWith(URL_PATH_KEYS);
                boolean isEditKey = path.startsWith(URL_PATH_EDIT_KEY);
                boolean isNewKey = path.startsWith(URL_PATH_NEW_KEY);

                if (isEditKey || isNewKey) {
                    // Editing or creating a key
                    help.setText(Html.fromHtml(getString(R.string.help_edit_key)));

                    // Scroll down and check write access box
                    view.loadUrl(JS_SCROLL);
                    view.loadUrl(JS_CHECK_WRITE);
                }else if(isKeyList){
                    // Looking at list of keys
                    help.setText(Html.fromHtml(getString(R.string.help_choose_key)));

                    // Scroll down and scrape page for available keys
                    view.loadUrl(JS_SCROLL);
                    view.loadUrl(JS_KEY_SCRAPE);
                }else if (path.startsWith(URL_PATH_LOGIN)) {
                    help.setText(Html.fromHtml(getString(R.string.help_login)));

                    // Scroll down to login box
                    view.loadUrl(JS_FOCUS_UID);
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
                    progressDialog.setProgress(0);
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
