package org.ale.scan2zotero;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class Scan2ZoteroActivity extends Activity {

    private static final String ZXING_PKG = "com.google.zxing.client.android";
    private static final String ZXING_URI = "market://search?q=pname:"+ZXING_PKG;
    
    protected static final String PREFS_NAME = "config";
    
    protected static String mUserId;
    
    protected static String mApiKey;

    // Subactivity result codes
    private static final int RESULT_SCAN = 0;
    private static final int RESULT_APIKEY = 1;

    // Logging tag
    private static final String CLASS_TAG = Scan2ZoteroActivity.class.getCanonicalName();

    private ZoteroAPIClient mZAPI;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        loadConfig();
        findViewById(R.id.login).setOnClickListener(loginButtonListener);
        findViewById(R.id.get_login_from_web).setOnClickListener(loginButtonListener);
        findViewById(R.id.scan_isbn).setOnClickListener(scanIsbn);
        findViewById(R.id.get_groups).setOnClickListener(getGroups);

        updateLoginInterface();
        mZAPI = new ZoteroAPIClient(mUserId, mApiKey);
    }

    @Override
    public void onStop(){
        super.onStop();
        saveConfig();
    }

    private void updateLoginInterface(){
        boolean validId = Util.validateUserId(mUserId);
        boolean validKey = Util.validateApiKey(mApiKey);

        if(!validId || !validKey){
            ((EditText) findViewById(R.id.apikey_edittext))
                            .setText(mUserId, TextView.BufferType.EDITABLE);
            ((EditText) findViewById(R.id.apikey_edittext))
                            .setText(mApiKey, TextView.BufferType.EDITABLE);
        }else{
            findViewById(R.id.login).setVisibility(View.GONE);
            mZAPI.setUserAndKey(mUserId, mApiKey);
        }
    }

    private final Button.OnClickListener loginButtonListener = new Button.OnClickListener() {
        public void onClick(View v) {
            switch(v.getId()){
            case R.id.login:
                // validate UserID and APIKey
                mUserId = ((EditText) findViewById(R.id.userid_edittext)).getText().toString();
                mApiKey = ((EditText) findViewById(R.id.apikey_edittext)).getText().toString();
                break;
            case R.id.get_login_from_web:
                informUserAboutLogin();
                break;
            //TODO:
            //case R.id.login_openid
            }
        }
    };

    private void informUserAboutLogin(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        DialogInterface.OnClickListener clickListener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int i) {
                if(i == DialogInterface.BUTTON_POSITIVE){
                    // Launch the GetApiKey Activity
                    Intent intent = new Intent(Scan2ZoteroActivity.this, GetApiKeyActivity.class);
                    startActivityForResult(intent, RESULT_APIKEY);
                }else{
                    dialog.dismiss();
                }
            }
        };

        builder.setTitle("Redirecting you to Zotero.org");
        builder.setMessage(getString(R.string.redirect));
        builder.setPositiveButton(getString(R.string.proceed), clickListener);
        builder.setNegativeButton(getString(R.string.cancel), clickListener);
        builder.show();
    }

    private final Button.OnClickListener scanIsbn = new Button.OnClickListener() {
        public void onClick(View v) {
            Log.d(CLASS_TAG, "Starting z-scan");
            try{
                Intent intent = new Intent("com.google.zxing.client.android.SCAN");
                intent.setPackage(ZXING_PKG);
                intent.putExtra("SCAN_MODE", "ONE_D_MODE");
                startActivityForResult(intent, RESULT_SCAN);
            } catch (ActivityNotFoundException e) {
                // Ask the user if we should install ZXing scanner
                getZxingScanner();
            }
        }
    };

    private final Button.OnClickListener getGroups = new Button.OnClickListener() {
        public void onClick(View v) {
            Log.d(CLASS_TAG, "Starting get groups");
            mZAPI.getUsersGroups(mGroupsHandler);
        }

        private Handler mGroupsHandler = new Handler(){
            public void handleMessage(Message msg){
                switch(msg.what){
                case ZoteroAPIRequest.START:
                    Log.d(CLASS_TAG, "I started a get");
                    break;
                case ZoteroAPIRequest.EXCEPTION:
                    ((Exception)msg.obj).printStackTrace();
                    break;
                case ZoteroAPIRequest.SUCCESS:
                    Log.d(CLASS_TAG, "SUCCESS!");
                    Log.d(CLASS_TAG, (String)msg.obj);
                    break;
                }
            }
        };
    };

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        switch(requestCode){
            case RESULT_SCAN:
                if (resultCode == RESULT_OK) {
                    String contents = intent.getStringExtra("SCAN_RESULT"); // The scanned ISBN
                    String format = intent.getStringExtra("SCAN_RESULT_FORMAT"); // "EAN 13"
                    Log.d(CLASS_TAG, contents + "\t" + format);
                } else if (resultCode == RESULT_CANCELED) {
                    Log.d(CLASS_TAG, "Cancelled");
                }
                break;
            case RESULT_APIKEY:
                mUserId = intent.getStringExtra(GetApiKeyActivity.USERID);
                mApiKey = intent.getStringExtra(GetApiKeyActivity.APIKEY);
                updateLoginInterface();
                break;
        }
    }

    protected void loadConfig(){
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        mUserId = prefs.getString(getString(R.string.userid_pref_key), "");
        mApiKey = prefs.getString(getString(R.string.apikey_pref_key), "");
    }

    protected void saveConfig(){
        SharedPreferences config = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = config.edit();
        editor.putString(getString(R.string.userid_pref_key), mUserId);
        editor.putString(getString(R.string.apikey_pref_key), mApiKey);
        editor.commit();
    }

    private AlertDialog getZxingScanner() {
        AlertDialog.Builder downloadDialog = new AlertDialog.Builder(Scan2ZoteroActivity.this);
        downloadDialog.setTitle(getString(R.string.install_bs_title));
        downloadDialog.setMessage(getString(R.string.install_bs_msg));
        downloadDialog.setPositiveButton(getString(R.string.install), new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialogInterface, int i) {
            Uri uri = Uri.parse(ZXING_URI);
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            Scan2ZoteroActivity.this.startActivity(intent);
          }
        });
        downloadDialog.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialogInterface, int i) {
              dialogInterface.dismiss();
          }
        });
        return downloadDialog.show();        
    }
}