package org.ale.scan2zotero;

import org.ale.scan2zotero.data.Account;
import org.ale.scan2zotero.data.S2ZDatabase;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ViewFlipper;

public class Scan2ZoteroActivity extends Activity {

    protected static final String PREFS_NAME = "config";
    
    protected Account mAccount;
    
    protected boolean mFirstRun;

    protected boolean mLoggedIn;

    // Subactivity result codes
    protected static final int RESULT_SCAN = 0;
    protected static final int RESULT_APIKEY = 1;

    private static final int GOT_SAVED_KEYS = 0;

    private static final int VIEW_LOGIN = 0;
    private static final int VIEW_MAIN = 1;

    // Logging tag
    private static final String CLASS_TAG = Scan2ZoteroActivity.class.getCanonicalName();

    private ZoteroAPIClient mZAPI;

    private Cursor mAcctCursor = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        findViewById(R.id.login_manually).setOnClickListener(loginButtonListener);
        findViewById(R.id.login_by_web).setOnClickListener(loginButtonListener);
        findViewById(R.id.register_new_account).setOnClickListener(loginButtonListener);
        findViewById(R.id.learn_more).setOnClickListener(loginButtonListener);
        findViewById(R.id.login_submit).setOnClickListener(loginButtonListener);
        findViewById(R.id.login_cancel).setOnClickListener(loginButtonListener);

        mLoggedIn = false;

        mZAPI = new ZoteroAPIClient();

        loadConfig();

        if(!mFirstRun){
            findViewById(R.id.login_instructions).setVisibility(View.GONE);
        }
    }

    @Override
    public void onResume(){
        super.onResume();
        if(!mLoggedIn){
            showLoginScreen();
        }
    }

    @Override
    public void onPause(){
        super.onPause();
        saveConfig();
    }

    private void showLoginScreen(){
        ((ViewFlipper)findViewById(R.id.s2z_flipper)).setDisplayedChild(VIEW_LOGIN);

        if(!TextUtils.isEmpty(mAccount.getUid()) && !TextUtils.isEmpty(mAccount.getKey())){
            findViewById(R.id.choose_login_method).setVisibility(View.GONE);
            findViewById(R.id.edit_id_and_key).setVisibility(View.VISIBLE);
        }else{
            // Otherwise display the options and look for saved keys.
            findViewById(R.id.choose_login_method).setVisibility(View.VISIBLE);
            findViewById(R.id.edit_id_and_key).setVisibility(View.GONE);
            if(mAcctCursor == null || mAcctCursor.getCount() == 0){
                getSavedKeys();
            }else if(mAcctCursor.getCount() > 0){
                promptToUseSavedKey(mAcctCursor);
            }
        }
    }

    private void doLogin(){
        boolean validId = mAccount.hasValidUserId();
        boolean validKey = mAccount.hasValidApiKey();

        if(validId && validKey){
            mLoggedIn = true;
            mZAPI.setAccount(mAccount);
            if(((CheckBox)findViewById(R.id.save_login)).isChecked()){
                saveLoginData();
            }else{
                //TODO: forgetKeyIfNecessary();
            }
            ((ViewFlipper)findViewById(R.id.s2z_flipper)).setDisplayedChild(VIEW_MAIN);
        }else{
            if(!validId){
                //TODO: Show id is invalid
            }
            if(!validKey){
                //TODO: Show key is invalid
            }
        }
    }

    private void saveLoginData(){
        if(mAcctCursor != null && mAcctCursor.getCount() > 0){
            // Check if key is already in database
            String pKey;
            String keyToInsert = mAccount.getKey();
            mAcctCursor.moveToFirst();
            while(mAcctCursor.isAfterLast() == false){
                pKey = mAcctCursor.getString(S2ZDatabase.ACCOUNT_KEY_INDEX);
                if(TextUtils.equals(pKey, keyToInsert)){
                    return;
                }
            }
        }else{ // Insert the key
            ContentValues values = mAccount.toContentValues();
            getContentResolver().insert(S2ZDatabase.ACCOUNT_URI, values);
        }
    }

    private final Button.OnClickListener scanIsbn = new Button.OnClickListener() {
        public void onClick(View v) {
            Log.d(CLASS_TAG, "Starting z-scan");
            try{
                Intent intent = new Intent(getString(R.string.zxing_intent_scan));
                intent.setPackage(getString(R.string.zxing_pkg));
                intent.putExtra("SCAN_MODE", "ONE_D_MODE");
                startActivityForResult(intent, RESULT_SCAN);
            } catch (ActivityNotFoundException e) {
                // Ask the user if we should install ZXing scanner
                Util.getZxingScanner(Scan2ZoteroActivity.this);
            }
        }
    };

    /*private final Button.OnClickListener getGroups = new Button.OnClickListener() {
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
    };*/

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        switch(requestCode){
            case RESULT_APIKEY:
                if (resultCode == RESULT_OK) {
                    String alias = intent.getStringExtra(GetApiKeyActivity.ALIAS);
                    String uid = intent.getStringExtra(GetApiKeyActivity.USERID);
                    String key = intent.getStringExtra(GetApiKeyActivity.APIKEY);
                    setUserAndKey(alias, uid, key);
                }
                break;
            case RESULT_SCAN:
                if (resultCode == RESULT_OK) {
                    String contents = intent.getStringExtra("SCAN_RESULT"); // The scanned ISBN
                    String format = intent.getStringExtra("SCAN_RESULT_FORMAT"); // "EAN 13"
                }
                break;
        }
    }

    private final Handler mDBHandler = new Handler() {
        public void handleMessage(Message msg){
            switch(msg.what){
            case GOT_SAVED_KEYS:
                mAcctCursor = (Cursor)msg.obj;
                if(!mLoggedIn && (mAcctCursor.getCount() > 0))
                    promptToUseSavedKey((Cursor)msg.obj);
                break;
            }
        }
    };

    private void getSavedKeys(){
        // This might create or upgrade the database, so it is
        // run in a separate thread.
        Thread task = new Thread(new Runnable() {
            @Override
            public void run() {
                Cursor c = managedQuery(S2ZDatabase.ACCOUNT_URI,null, null, null, null);
                mDBHandler.sendMessage(Message.obtain(mDBHandler, GOT_SAVED_KEYS, c));
            }
        });
        task.start();
    }

    private void promptToUseSavedKey(final Cursor c){
        AlertDialog.Builder downloadDialog =
                    new AlertDialog.Builder(Scan2ZoteroActivity.this);
        DialogInterface.OnClickListener clickListener = 
                    new DialogInterface.OnClickListener() {
            private int selected;
            public void onClick(DialogInterface dialog, int i) {
                if(i == DialogInterface.BUTTON_POSITIVE){ 
                    c.moveToPosition(selected);
                    String alias = c.getString(S2ZDatabase.ACCOUNT_ALIAS_INDEX);
                    String uid = c.getString(S2ZDatabase.ACCOUNT_UID_INDEX);
                    String key = c.getString(S2ZDatabase.ACCOUNT_KEY_INDEX);
                    setUserAndKey(alias, uid, key);
                    showLoginScreen();
                    dialog.dismiss();
                }else if(i == DialogInterface.BUTTON_NEGATIVE){
                    // User cancelled dialog
                    dialog.dismiss();
                }else{
                    // User clicked a key, but did not yet confirm their choice
                    selected = i;
                }
            }
        };
        downloadDialog.setTitle("Login with saved key?");
        downloadDialog.setPositiveButton("Use selected key", clickListener);
        downloadDialog.setNegativeButton("None of these", clickListener);
        downloadDialog.setSingleChoiceItems(c, 0, Account.COL_ALIAS, clickListener);
        downloadDialog.show(); 
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        if(mLoggedIn){
            inflater.inflate(R.menu.context_menu_main, menu);
        }else{
            inflater.inflate(R.menu.context_menu_login, menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(mLoggedIn){
            // Handle item selection
            switch (item.getItemId()) {
            case R.id.ctx_logout:
                setUserAndKey("", "", "");
                mLoggedIn = false;
                showLoginScreen();
                return true;
            case R.id.ctx_settings:
                return true;
            }
        }else{
            switch (item.getItemId()) {
            case R.id.ctx_manage_keys:
                //showEditKeyDialog();
                return true;
            case R.id.ctx_about:
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    protected void loadConfig(){
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String alias = prefs.getString(getString(R.string.alias_pref_key), "");
        String uid = prefs.getString(getString(R.string.userid_pref_key), "");
        String key = prefs.getString(getString(R.string.apikey_pref_key), "");
        mFirstRun = prefs.getBoolean(getString(R.string.firstrun_pref_key), true);
        setUserAndKey(alias, uid, key);
    }

    protected void saveConfig(){
        SharedPreferences config = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = config.edit();

        boolean saveLogin = ((CheckBox)findViewById(R.id.save_login)).isChecked();
        editor.putString(getString(R.string.alias_pref_key),
                saveLogin ? mAccount.getAlias() : "");
        editor.putString(getString(R.string.userid_pref_key),
                saveLogin ? mAccount.getUid() : "");
        editor.putString(getString(R.string.apikey_pref_key),
                saveLogin ? mAccount.getKey() : "");

        editor.putBoolean(getString(R.string.firstrun_pref_key), false);

        editor.commit();
    }

    protected void setUserAndKey(String alias, String uid, String key){
        mAccount = new Account(alias, uid, key);
        ((EditText) findViewById(R.id.userid_edittext)).setText(mAccount.getUid());
        ((EditText) findViewById(R.id.apikey_edittext)).setText(mAccount.getKey());
    }

    private final Button.OnClickListener loginButtonListener = new Button.OnClickListener() {
        public void onClick(View v) {
            switch(v.getId()){

            case R.id.login_manually:
                findViewById(R.id.choose_login_method).setVisibility(View.GONE);
                findViewById(R.id.edit_id_and_key).setVisibility(View.VISIBLE);
                break;

            case R.id.login_by_web:
                Util.informUserAboutLogin(Scan2ZoteroActivity.this,
                                          GetApiKeyActivity.EXISTING_ACCOUNT);
                break;

            case R.id.register_new_account:
                Util.informUserAboutLogin(Scan2ZoteroActivity.this,
                                          GetApiKeyActivity.NEW_ACCOUNT);
                break;

            case R.id.login_submit:
                // validate UserID and APIKey
                mAccount.setUid(((EditText) findViewById(R.id.userid_edittext)).getText().toString());
                mAccount.setKey(((EditText) findViewById(R.id.apikey_edittext)).getText().toString());
                doLogin();
                break;

            case R.id.login_cancel:
                setUserAndKey("", "", "");
                showLoginScreen();
                break;

            case R.id.learn_more: // TODO: Make this more helpful.
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse("http://zotero.org"));
                startActivity(i);
                break;
            //TODO: case R.id.login_openid:
            }
        }
    };
}