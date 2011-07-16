package org.ale.scan2zotero;

import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class GetApiKeyActivity extends Activity {

    /* This is sinful thought. */  
    private static final String JS_KEY_SCRAPE =
        "javascript:window.KEYSCRAPE.foundKeys(" +
        "(function tostr(table) { " +
        "var res = []; " +
        "for(var i=1, len = table.rows.length; i < len; i++) { " +
        "res.push(table.rows[i].cells[0].innerHTML + '|' + table.rows[i].cells[2].childNodes[0]); " +
        "} return res; })" +
        "(document.getElementById('api-keys-table')).join(','));";
        
    private static final String CLASS_TAG = GetApiKeyActivity.class.getCanonicalName();
    
    public static final String USERID = "USERID";
    public static final String APIKEY = "APIKEY";
    
    private String[] mKeyNames;
    private String[] mKeyURIs;
    private String mUserId;
    private String mApiKey;

    public void onCreate(Bundle state) {
        super.onCreate(state);
        WebView wv = new WebView(this);
        setContentView(wv);

        wv.getSettings().setJavaScriptEnabled(true);
        
        wv.addJavascriptInterface(new KeyScraper(), "KEYSCRAPE");  
          
        /* WebViewClient must be set BEFORE calling loadUrl! */  
        wv.setWebViewClient(new WebViewClient() {  
            @Override  
            public void onPageFinished(WebView view, String url)  
            {  
                if(url.startsWith("https://zotero.org/settings/keys")) {
                    /* This is sinful action. */
                    view.loadUrl(JS_KEY_SCRAPE);
                }
            }
        });
          
        Map<String, String> extraHeaders = new HashMap<String, String>();
        extraHeaders.put("Referer", "https://zotero.org/settings/keys");

        wv.loadUrl("https://zotero.org/user/login/", extraHeaders);
        setResult(RESULT_OK);
    }

    protected void buildKeySelectDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        
        DialogInterface.OnClickListener clickListener = new DialogInterface.OnClickListener() {
            private int selected;
            public void onClick(DialogInterface dialog, int i) {
                if(i == DialogInterface.BUTTON_POSITIVE){
                    if(parseKeyUri(mKeyURIs[selected])){
                        done();
                    }else{
                        Toast.makeText(GetApiKeyActivity.this, "Could not parse selected key.", Toast.LENGTH_LONG);
                    }
                }else if(i == DialogInterface.BUTTON_NEGATIVE){
                    dialog.dismiss();
                }else{
                    selected = i;
                }
            }
        };
        
        builder.setTitle("Select a key");
        builder.setSingleChoiceItems(mKeyNames, 0, clickListener);
        builder.setPositiveButton("Use selected key", clickListener);
        builder.setNegativeButton("None of these", clickListener);
        builder.show();
    }

    private boolean parseKeyUri(String struri) {
        Uri uri;
        try{
            uri = Uri.parse(struri);
            Log.d(CLASS_TAG, uri.getPath());
            mUserId = uri.getPath().split("/")[2];
            Log.d(CLASS_TAG, mUserId);
            mApiKey = uri.getQueryParameter("key");
        }catch(Exception e){
            return false;
        }
        if(mUserId.length() == 0 || mApiKey.length() == 0)
            return false;
        return true;
    }
    
    private void done(){
        Intent resultIntent = new Intent();
        resultIntent.putExtra(USERID, mUserId);
        resultIntent.putExtra(APIKEY, mApiKey);
        setResult(RESULT_OK, resultIntent);
        finish();
    }
    
    private class KeyScraper {  
        @SuppressWarnings("unused")  
        public void foundKeys(String result)  
        {
            String[] keyrows = result.split(",");
            mKeyNames = new String[keyrows.length];
            mKeyURIs = new String[keyrows.length];
            for (int i=0; i < keyrows.length; i++){
                int split = keyrows[i].indexOf("|");
                mKeyNames[i] = keyrows[i].substring(0, split);
                mKeyURIs[i] = keyrows[i].substring(split+1);
            }
            buildKeySelectDialog();
        }
    }
}
