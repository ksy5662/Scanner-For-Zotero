package org.ale.scan2zotero;

import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.net.http.SslCertificate;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

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

    public static final String ALIAS = "ALIAS";
    public static final String USERID = "USERID";
    public static final String APIKEY = "APIKEY";

    private static final int JS_FOUND_KEYS = 0;
    private static final int JS_CHECK_LOGIN = 1;

    private String[] mKeyNames;
    private String[] mKeyURIs;
    private String mUserId;
    private String mApiKey;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.apikeywebview);
        
        WebView wv = (WebView) findViewById(R.id.webView);

        /* WebViewClient must be set BEFORE calling loadUrl! */  
        wv.setWebViewClient(new WebViewClient() {
            // TODO: Block non-zotero.org sites or maybe white-list just the pages
            // we need.
            @Override
            public void onPageFinished(WebView view, String url)  
            {
                if(url.indexOf("zotero.org/user/validate") > 0) {
                    showEmailValidationDialog();
                }else if(url.indexOf("zotero.org/user/login") > 0) {
                    view.loadUrl(JS_LOGGED_IN);
                }else if(url.indexOf("zotero.org/settings/keys") > 0) {
                    view.loadUrl(JS_KEY_SCRAPE);
                }
            }
        });

        Bundle extras = getIntent().getExtras();
        if(extras.getInt(LOGIN_TYPE, EXISTING_ACCOUNT) == EXISTING_ACCOUNT){
            // TODO: extreaHeaders doesn't exist on versions <= 2.1 - try to find workaround
            Map<String, String> extraHeaders = new HashMap<String, String>();
            extraHeaders.put("Referer", "https://zotero.org/settings/keys");
            wv.getSettings().setJavaScriptEnabled(true);
            wv.addJavascriptInterface(new KeyScraper(), "KEYSCRAPE");
            wv.loadUrl("https://zotero.org/user/login/", extraHeaders);
        }else{
            wv.loadUrl("https://zotero.org/user/register/");
        }
    }

    /* Dialogs */
    protected void showEmailValidationDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Email Verification");
        builder.setMessage("You will need to verify your email address before using your new account with Scan2Zotero.");
        builder.setNeutralButton("Ok", new DialogInterface.OnClickListener(){
            public void onClick(DialogInterface dialog, int i){
                GetApiKeyActivity.this.finish();
            }
        });
        builder.show();
    }

    protected void showSSLDialog(SslCertificate cert){
        SslCertificate.DName issuer = cert.getIssuedBy();
        SslCertificate.DName owner = cert.getIssuedTo();

        String msg = (new StringBuilder())
                        .append("Issued to:\n(CN)    ").append(owner.getCName())
                        .append("\n(O)    ").append(owner.getOName())
                        .append("\n(OU)    ").append(owner.getUName())
                        .append("\n\nIssued By:\n(CN)    ").append(issuer.getCName())
                        .append("\n(O)    ").append(issuer.getOName())
                        .append("\n(OU)    ").append(issuer.getUName())
                        .append("\n\nValidity:\nIssued On    ")
                        .append(cert.getValidNotBefore().substring(0, 10))
                        .append("\nExpires On    ")
                        .append(cert.getValidNotAfter().substring(0, 10))
                        .toString();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setMessage(msg.toString());
        builder.setNeutralButton("Done", null);
        builder.show();
    }

    // No keys were found, provide shortcut to key creation page on zotero.org
    // or let the user abort.
    protected void showNoKeysDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        DialogInterface.OnClickListener clickListener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int i) {
                if(i == DialogInterface.BUTTON_POSITIVE){
                    WebView wv = (WebView) findViewById(R.id.webView);
                    wv.loadUrl("https://zotero.org/settings/keys/new");
                }else if(i == DialogInterface.BUTTON_NEGATIVE){
                    setResult(RESULT_CANCELED, null);
                    finish();
                }
                dialog.dismiss();
            }
        };

        builder.setTitle("No API Keys Found");
        builder.setPositiveButton("Create a new key", clickListener);
        builder.setNegativeButton(getString(R.string.cancel), clickListener);
        builder.show();
    }

    // One or more keys were found on the page, present the user with a list,
    // by name, and let them pick the one they want to use.
    protected void showSelectKeyDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        
        DialogInterface.OnClickListener clickListener = new DialogInterface.OnClickListener() {
            private int selected;
            public void onClick(DialogInterface dialog, int i) {
                if(i == DialogInterface.BUTTON_POSITIVE){ 
                    if(parseKeyUri(mKeyURIs[selected])){ // Try to grab key info from uri
                        Intent resultIntent = new Intent();
                        resultIntent.putExtra(ALIAS, mKeyNames[selected]);
                        resultIntent.putExtra(USERID, mUserId);
                        resultIntent.putExtra(APIKEY, mApiKey);
                        setResult(RESULT_OK, resultIntent);
                        finish();
                    }else{ // Problem parsing uri
                        Toast.makeText(GetApiKeyActivity.this, "Could not parse selected key.", Toast.LENGTH_LONG);
                    }
                }else if(i == DialogInterface.BUTTON_NEGATIVE){ // User cancelled dialog
                    dialog.dismiss();
                }else{ // User clicked a key, but did not yet confirm their choice
                    selected = i;
                }
            }
            // Helper function for parsing URIs containing API key info
            private boolean parseKeyUri(String struri) {
                Uri uri;
                try{
                    uri = Uri.parse(struri);
                    mUserId = uri.getPath().split("/")[2];
                    mApiKey = uri.getQueryParameter("key");
                }catch(Exception e){
                    return false;
                }
                if(mUserId.length() == 0 || mApiKey.length() == 0)
                    return false;
                return true;
            }
        };

        builder.setTitle("Found API Keys");
        // Make radio buttons w/ key names and select first key
        builder.setSingleChoiceItems(mKeyNames, 0, clickListener);
        builder.setPositiveButton("Use selected key", clickListener);
        builder.setNegativeButton("None of these", clickListener);
        builder.show();
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
                String[] keyrows = ((String)msg.obj).split(",");
                if(TextUtils.isEmpty(keyrows[0])){
                    showNoKeysDialog();
                    return;
                }
                mKeyNames = new String[keyrows.length];
                mKeyURIs = new String[keyrows.length];
                for (int i=0; i < keyrows.length; i++){
                    int split = keyrows[i].indexOf("|");
                    if(split > 0){
                        mKeyNames[i] = keyrows[i].substring(0, split);
                        mKeyURIs[i] = keyrows[i].substring(split+1);
                    }
                }
                showSelectKeyDialog();
                break;
            case JS_CHECK_LOGIN:
                if(!((String)msg.obj).equals("-1")){
                    ((WebView) findViewById(R.id.webView))
                        .loadUrl("https://zotero.org/settings/keys");
                }
                break;
            }
        }
    };

    /* Options menu -- entirely for SSL at the moment */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.context_menu_webview, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() != R.id.ctx_webview_showssl){
            return super.onOptionsItemSelected(item);
        }
        SslCertificate cert =
            ((WebView) findViewById(R.id.webView)).getCertificate();
        if(cert != null)
            showSSLDialog(cert);
        return true;
    }

}
