package org.ale.scan2zotero;

import org.ale.scan2zotero.data.Account;
import org.ale.scan2zotero.data.S2ZDatabase;

import android.app.Activity;
import android.app.AlertDialog;
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
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.ViewFlipper;

public class S2ZLoginActivity extends Activity {

    // Logging tag
    private static final String CLASS_TAG = S2ZLoginActivity.class.getCanonicalName();

    public static final String PREFS_NAME = "config";
    
    public static final String INTENT_EXTRA_CLEAR_FIELDS = "CLEAR_FIELDS";

    // Subactivity result codes
    public static final int RESULT_APIKEY = 0;

    public static final int GOT_CURSOR = 0;
    // Transitions to make on receiving cursor
    public static final int RECV_CURSOR_NOTHING = -1;
    public static final int RECV_CURSOR_PROMPT = 0;
    public static final int RECV_CURSOR_LOGIN = 1;

    // Transient state
    private Account mAccount;

    private boolean mFirstRun;

    private boolean mLoggedIn;

    private boolean mRememberMe;

    private Cursor mAcctCursor = null;

    private AlertDialog mAlertDialog = null;

    private int mOnRecvCursor = RECV_CURSOR_NOTHING;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        // Load the saved state, fills user/key fields, sets checkboxes etc.
        loadConfig();

        // All listeners are defined at the bottom of this file.
        // Login option buttons:
        findViewById(R.id.login_saved_key).setOnClickListener(loginButtonListener);
        findViewById(R.id.login_by_web).setOnClickListener(loginButtonListener);
        findViewById(R.id.login_manually).setOnClickListener(loginButtonListener);
        findViewById(R.id.register_new_account).setOnClickListener(loginButtonListener);
        //findViewById(R.id.learn_more).setOnClickListener(loginButtonListener);
        findViewById(R.id.login_submit).setOnClickListener(loginButtonListener);
        findViewById(R.id.login_cancel).setOnClickListener(loginButtonListener);

        // The checkbox determines whether the alias field is visible
        // and sets mRememberMe
        ((CheckBox)findViewById(R.id.save_login)).setOnCheckedChangeListener(cbListener);

        // These update mAccount when an edittext changes
        findViewById(R.id.useralias_edittext).setOnKeyListener(editableTextListener);
        findViewById(R.id.userid_edittext).setOnKeyListener(editableTextListener);
        findViewById(R.id.apikey_edittext).setOnKeyListener(editableTextListener);
        
        // These validate edittext content on focus change
        findViewById(R.id.useralias_edittext).setOnFocusChangeListener(focusTextListener);
        findViewById(R.id.userid_edittext).setOnFocusChangeListener(focusTextListener);
        findViewById(R.id.apikey_edittext).setOnFocusChangeListener(focusTextListener);

        // Query the database for saved keys
        getSavedKeys();

        // If we're called from Main via "Log out", we need to clear the login info
        // main provides an extra telling us this.
        Bundle extras = getIntent().getExtras();
        if(extras != null && extras.getBoolean(INTENT_EXTRA_CLEAR_FIELDS, false)){
            setUserAndKey("","","");
            mLoggedIn = false;
        }

        // Set the remember me checkbox
        ((CheckBox)findViewById(R.id.save_login)).setChecked(mRememberMe);

        // Until the user logs in successfully, some instructions are provided.
        if(!mFirstRun){
            findViewById(R.id.login_instructions).setVisibility(View.GONE);
        }
    }

    @Override
    public void onResume(){
        super.onResume();
        if(mLoggedIn){ // Might still be logged in from last session
            if(mAcctCursor != null){
                doLogin();
            }else{
                mOnRecvCursor = RECV_CURSOR_LOGIN;
            }
        }

        // Display any dialogs we were displaying before being destroyed
        switch(S2ZDialogs.displayedDialog) {
        case(S2ZDialogs.DIALOG_NO_DIALOG):
            break;
        case(S2ZDialogs.DIALOG_ZOTERO_REGISTER):
            mAlertDialog = S2ZDialogs.informUserAboutLogin(S2ZLoginActivity.this,
                                GetApiKeyActivity.NEW_ACCOUNT);
            break;
        case(S2ZDialogs.DIALOG_ZOTERO_LOGIN):
            mAlertDialog = S2ZDialogs.informUserAboutLogin(S2ZLoginActivity.this,
                                GetApiKeyActivity.EXISTING_ACCOUNT);
            break;
        case(S2ZDialogs.DIALOG_SAVED_KEYS):
            mOnRecvCursor = RECV_CURSOR_PROMPT;
            break;
        }
    }

    @Override
    public void onPause(){
        super.onPause();
        saveConfig();
        if(mAlertDialog != null){ // Prevent dialog windows from leaking
            mAlertDialog.dismiss();
            mAlertDialog = null;
        }
    }

    private void doLogin(){ 
        // mAcctCursor MUST be open before this is called

        boolean validAlias = validateUserAlias(); // These have side effects
        boolean validId = validateUserId();       //
        boolean validKey = validateApiKey();      //

        if(!(validAlias && validId && validKey))
            return;

        // Try to find a matching account in the database
        int acctId = Account.NOT_IN_DATABASE;
        if(mAcctCursor.getCount() > 0){
            // Check if key is already in database
            String pKey;
            String keyToInsert = mAccount.getKey();
            mAcctCursor.moveToFirst();
            while(mAcctCursor.isAfterLast() == false){
                pKey = mAcctCursor.getString(S2ZDatabase.ACCOUNT_KEY_INDEX);
                if(TextUtils.equals(pKey, keyToInsert)){
                    acctId = mAcctCursor.getInt(S2ZDatabase.ACCOUNT_ID_INDEX);
                    break;
                }
                mAcctCursor.moveToNext();
            }
        }

        // Insert new key into database
        if(mRememberMe && acctId == Account.NOT_IN_DATABASE){
            // Yes, this blocks the UI thread.
            ContentValues values = mAccount.toContentValues();
            Uri result = getContentResolver().insert(S2ZDatabase.ACCOUNT_URI, values);
            acctId = Integer.parseInt(result.getLastPathSegment());
        }

        mAccount.setDbId(acctId);

        mLoggedIn = mRememberMe; // This is the mLoggedIn value that gets saved
                                 // to prefs. If the user didn't check "Remember Me"
                                 // we won't automatically log them in.
        // Transition to Main activity
        Intent intent = new Intent(S2ZLoginActivity.this, S2ZMainActivity.class);
        intent.putExtra(S2ZMainActivity.INTENT_EXTRA_ACCOUNT, mAccount);
        S2ZLoginActivity.this.startActivity(intent);
        finish();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        switch(requestCode){
            case RESULT_APIKEY:
                if (resultCode == RESULT_OK) {
                    Account acct = (Account) intent.getParcelableExtra(GetApiKeyActivity.ACCOUNT);
                    setUserAndKey(acct);
                    showNext();
                }
                break;
        }
    }

    private void getSavedKeys(){
        // Ignore this call if we have an open cursor
        if(mAcctCursor != null && !mAcctCursor.isClosed())
            return;

        // This might create or upgrade the database, so it is
        // run in a separate thread.
        new Thread(new Runnable() {
            @Override
            public void run() {
                mAcctCursor = managedQuery(S2ZDatabase.ACCOUNT_URI, null, null, null, null);

                mCursorHandler.sendMessage(Message.obtain(mCursorHandler, GOT_CURSOR));
            }
        }).start();
    }

    private Handler mCursorHandler = new Handler() {
        public void handleMessage(Message msg){
            if(msg.what != GOT_CURSOR)
                return;
            switch(mOnRecvCursor){
            // On an activity recreate (following orientation change, etc)
            // we need to immediately call promptToUseSavedKey if the dialog
            // was displayed prior to the activity being destroyed.
            case RECV_CURSOR_PROMPT:
                mAlertDialog = S2ZDialogs.promptToUseSavedKey(
                                    S2ZLoginActivity.this, mAcctCursor);
                break;
            // And sometimes we're resuming a previous session and just need the
            // cursor to determine the account id
            case RECV_CURSOR_LOGIN:
                doLogin();
                break;
            }
            mOnRecvCursor = RECV_CURSOR_NOTHING;
        }
    };

    /* Saved Preferences */
    protected void loadConfig(){
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String alias = prefs.getString(getString(R.string.alias_pref_key), "");
        String uid = prefs.getString(getString(R.string.userid_pref_key), "");
        String key = prefs.getString(getString(R.string.apikey_pref_key), "");
        mFirstRun = prefs.getBoolean(getString(R.string.firstrun_pref_key), true);
        mRememberMe = prefs.getBoolean(getString(R.string.rememberme_pref_key), true);
        mLoggedIn = prefs.getBoolean(getString(R.string.logged_in_pref_key), false);

        setUserAndKey(alias, uid, key);
    }

    protected void saveConfig(){
        SharedPreferences config = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = config.edit();

        editor.putString(getString(R.string.alias_pref_key), mAccount.getAlias());
        editor.putString(getString(R.string.userid_pref_key), mAccount.getUid());
        editor.putString(getString(R.string.apikey_pref_key), mAccount.getKey());

        editor.putBoolean(getString(R.string.rememberme_pref_key), mRememberMe);

        editor.putBoolean(getString(R.string.logged_in_pref_key), mLoggedIn);

        // Set firstrun to false the first time the user logs in
        editor.putBoolean(getString(R.string.firstrun_pref_key), mFirstRun && !mLoggedIn);

        editor.commit();
    }

    protected void setUserAndKey(String alias, String uid, String key){
        if(TextUtils.isEmpty(alias))
            alias = "New User";
        setUserAndKey(new Account(alias, uid, key));
    }

    protected void setUserAndKey(Account acct){
        mAccount = acct;
        ((EditText) findViewById(R.id.useralias_edittext)).setText(acct.getAlias());
        ((EditText) findViewById(R.id.userid_edittext)).setText(acct.getUid());
        ((EditText) findViewById(R.id.apikey_edittext)).setText(acct.getKey());
        validateUserId();
        validateApiKey();
    }

    /* Input validation */
    private boolean validateUserAlias(){
        boolean valid = !(mRememberMe && TextUtils.isEmpty(mAccount.getAlias()));
        if(valid || TextUtils.isEmpty(mAccount.getAlias())){
            ((EditText) findViewById(R.id.useralias_edittext)).setError(null);
        }else{
            ((EditText) findViewById(R.id.useralias_edittext)).setError("Alias required");
        }
        return valid;
    }

    private boolean validateUserId(){
        boolean valid = mAccount.hasValidUserId();
        if(valid || TextUtils.isEmpty(mAccount.getUid()))
            ((EditText) findViewById(R.id.userid_edittext)).setError(null);
        else
            ((EditText) findViewById(R.id.userid_edittext)).setError("Invalid user ID");
        return valid;
    }

    private boolean validateApiKey(){
        boolean valid = mAccount.hasValidApiKey();
        if(valid || TextUtils.isEmpty(mAccount.getKey()))
            ((EditText) findViewById(R.id.apikey_edittext)).setError(null);
        else
            ((EditText) findViewById(R.id.apikey_edittext)).setError("Invalid API key");
        return valid;
    }


    /* View Flipping */
    private void showPrevious() {
        ViewFlipper vf = (ViewFlipper)findViewById(R.id.login_view_flipper);
        if(vf.getCurrentView().getId() == R.id.login_view_editables){
            vf.setInAnimation(AnimationUtils.loadAnimation(S2ZLoginActivity.this, R.anim.slide_in_previous));
            vf.setOutAnimation(AnimationUtils.loadAnimation(S2ZLoginActivity.this, R.anim.slide_out_previous));
            vf.showPrevious();
        }
    }

    private void showNext() {
        ViewFlipper vf = (ViewFlipper)findViewById(R.id.login_view_flipper);
        if(vf.getCurrentView().getId() == R.id.login_view_options){
            vf.setInAnimation(AnimationUtils.loadAnimation(S2ZLoginActivity.this, R.anim.slide_in_next));
            vf.setOutAnimation(AnimationUtils.loadAnimation(S2ZLoginActivity.this, R.anim.slide_out_next));
            vf.showNext();
        }
    }

    // Catch back button if we're showing the editable fields
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)  {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            ViewFlipper vf = ((ViewFlipper)findViewById(R.id.login_view_flipper));
            if(vf.getCurrentView().getId() == R.id.login_view_editables){
                showPrevious();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }


    /* Options Menu */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu_login, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.opt_manage_keys:
            Intent intent = new Intent(S2ZLoginActivity.this, ManageAccountsActivity.class);
            startActivity(intent);
            return true;
        case R.id.opt_use_saved_key:
            if (mAcctCursor != null) {
                mAlertDialog = S2ZDialogs.promptToUseSavedKey(
                                    S2ZLoginActivity.this, mAcctCursor);
            } else {
                mOnRecvCursor = RECV_CURSOR_PROMPT;
            }
            return true;
        case R.id.opt_about:
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    /* Interface listeners */
    private final Button.OnClickListener loginButtonListener = new Button.OnClickListener() {
        public void onClick(View v) {
            switch(v.getId()){
            case R.id.login_saved_key:
                if (mAcctCursor != null) {
                    mAlertDialog = S2ZDialogs.promptToUseSavedKey(
                                        S2ZLoginActivity.this, mAcctCursor);
                }else{
                    mOnRecvCursor = RECV_CURSOR_PROMPT;
                }
                break;

            case R.id.login_by_web:
                mAlertDialog = S2ZDialogs.informUserAboutLogin(
                                    S2ZLoginActivity.this,
                                    GetApiKeyActivity.EXISTING_ACCOUNT);
                break;
            
            case R.id.login_manually:
                showNext();
                break;

            case R.id.register_new_account:
                mAlertDialog = S2ZDialogs.informUserAboutLogin(
                                    S2ZLoginActivity.this,
                                    GetApiKeyActivity.NEW_ACCOUNT);
                break;

            case R.id.login_submit:
                if(mAcctCursor != null){
                    doLogin();
                }else{
                    mOnRecvCursor = RECV_CURSOR_LOGIN;
                }
                break;

            case R.id.login_cancel:
                showPrevious();
                setUserAndKey("", "", "");
                break;

            //TODO: case R.id.login_openid:
            }
        }
    };
    
    private final View.OnKeyListener editableTextListener = new View.OnKeyListener() {
        @Override
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            switch(v.getId()){
            case R.id.useralias_edittext:
                mAccount.setAlias(((EditText)v).getText().toString());
                break;
            case R.id.userid_edittext:
                mAccount.setUid(((EditText)v).getText().toString());
                if(!mAccount.hasValidUserId()) // Doesn't use validateUserId so as
                    ((EditText) v).setError(null); // to avoid setting a new error
                break;
            case R.id.apikey_edittext:
                mAccount.setKey(((EditText)v).getText().toString());
                if(!mAccount.hasValidApiKey()) // Same deal
                    ((EditText) v).setError(null);
                break;
            }
            return false;
        }
    };
    private final View.OnFocusChangeListener focusTextListener = new View.OnFocusChangeListener() {

        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            switch(v.getId()){
            case R.id.useralias_edittext:
                validateUserAlias();
            case R.id.userid_edittext:
                validateUserId();
                break;
            case R.id.apikey_edittext:
                validateApiKey();
                break;
            }
        }
    };
    
    private final CheckBox.OnCheckedChangeListener cbListener = new CheckBox.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton checkbox, boolean checked) {
            mRememberMe = checked;
            if(checked){
                findViewById(R.id.useralias_edittext).setVisibility(View.VISIBLE);
                validateUserAlias();
            }else{
                findViewById(R.id.useralias_edittext).setVisibility(View.GONE);
            }
        }
    };
}