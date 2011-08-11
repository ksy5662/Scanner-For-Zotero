package org.ale.scan2zotero;

import org.ale.scan2zotero.data.Account;
import org.ale.scan2zotero.data.Database;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
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
import android.widget.ViewFlipper;

public class LoginActivity extends Activity {

    //private static final String CLASS_TAG = LoginActivity.class.getCanonicalName();

    public static final String PREFS_NAME = "config";

    public static final String INTENT_EXTRA_CLEAR_FIELDS = "CLEAR_FIELDS";

    public static final String RECREATE_CURRENT_DISPLAY = "CURDISP";
    public static final String RECREATE_ACCOUNT = "ACCT";

    // Subactivity result codes
    public static final int RESULT_APIKEY = 0;

    // Transitions to make on receiving cursor
    public static final int RECV_CURSOR_NOTHING = -1;
    public static final int RECV_CURSOR_PROMPT = 0;
    public static final int RECV_CURSOR_LOGIN = 1;

    // Transient state
    private Account mAccount;

    private boolean mLoggedIn;

    private boolean mRememberMe;

    private Cursor mAcctCursor = null;

    private AlertDialog mAlertDialog = null;

    private int mOnRecvCursor = RECV_CURSOR_NOTHING;

    private Handler mHandler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        mHandler = new Handler();

        // Query the database for saved keys (separate thread)
        getSavedKeys();

        // Load the saved state, fills user/key fields, sets checkboxes etc.
        loadConfig();

        // All listeners are defined at the bottom of this file.
        // Login option buttons:
        findViewById(R.id.login_saved_key).setOnClickListener(loginButtonListener);
        findViewById(R.id.login_by_web).setOnClickListener(loginButtonListener);
        findViewById(R.id.login_manually).setOnClickListener(loginButtonListener);
        findViewById(R.id.login_submit).setOnClickListener(loginButtonListener);
        findViewById(R.id.login_cancel).setOnClickListener(loginButtonListener);

        // The checkbox determines whether the alias field is visible
        // and sets mRememberMe
        ((CheckBox)findViewById(R.id.save_login)).setOnCheckedChangeListener(cbListener);
        ((CheckBox)findViewById(R.id.save_login)).setChecked(mRememberMe);

        // These update mAccount when an edittext changes
        findViewById(R.id.userid_edittext).setOnKeyListener(editableTextListener);
        findViewById(R.id.apikey_edittext).setOnKeyListener(editableTextListener);
        
        // These validate edittext content on focus change
        findViewById(R.id.userid_edittext).setOnFocusChangeListener(focusTextListener);
        findViewById(R.id.apikey_edittext).setOnFocusChangeListener(focusTextListener);

        if (savedInstanceState != null){
            // Set the displayed screen (login options or editables)
            int curView = savedInstanceState.getInt(RECREATE_CURRENT_DISPLAY, 0);
            ((ViewFlipper)findViewById(R.id.login_view_flipper))
                .setDisplayedChild(curView);
            setUserAndKey((Account) savedInstanceState.getParcelable(RECREATE_ACCOUNT));
         }

        // If we're called from Main via "Log out", we need to clear the login info
        // (Main provides an extra telling us this)
        Bundle extras = getIntent().getExtras();
        if(extras != null && extras.getBoolean(INTENT_EXTRA_CLEAR_FIELDS, false)){
            setUserAndKey("","","");
            mLoggedIn = false;
        }
    }

    @Override
    public void onResume(){
        super.onResume();
        if(mLoggedIn){ // Might still be logged in from last session
            ViewFlipper vf = (ViewFlipper)findViewById(R.id.login_view_flipper);
            vf.setDisplayedChild(2);
            if(mAcctCursor != null){
                doLogin();
            }else{
                mOnRecvCursor = RECV_CURSOR_LOGIN;
            }
        }

        // Display any dialogs we were displaying before being destroyed
        switch(Dialogs.displayedDialog) {
        case(Dialogs.DIALOG_NO_DIALOG):
            break;
        case(Dialogs.DIALOG_ZOTERO_LOGIN):
            mAlertDialog = Dialogs.informUserAboutLogin(LoginActivity.this);
            break;
        case(Dialogs.DIALOG_SAVED_KEYS):
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

    @Override
    public void onSaveInstanceState(Bundle state){
        super.onSaveInstanceState(state);
        state.putInt(RECREATE_CURRENT_DISPLAY, 
             ((ViewFlipper)findViewById(R.id.login_view_flipper)).getDisplayedChild());
        state.putParcelable(RECREATE_ACCOUNT, mAccount);
    }

    private void doLogin(){ 
        // mAcctCursor MUST be open before this is called

        boolean validId = validateUserId();       // These have side effects
        boolean validKey = validateApiKey();      // (error flags on textviews)

        if(!(validId && validKey))
            return;

        // Try to find a matching account in the database
        int acctId = Account.NOT_IN_DATABASE;
        if(mAcctCursor.getCount() > 0){
            // Check if key is already in database
            String pKey;
            String keyToInsert = mAccount.getKey();
            mAcctCursor.moveToFirst();
            while(mAcctCursor.isAfterLast() == false){
                pKey = mAcctCursor.getString(Database.ACCOUNT_KEY_INDEX);
                if(TextUtils.equals(pKey, keyToInsert)){
                    acctId = mAcctCursor.getInt(Database.ACCOUNT_ID_INDEX);
                    break;
                }
                mAcctCursor.moveToNext();
            }
        }

        // Insert new key into database
        if(mRememberMe && acctId == Account.NOT_IN_DATABASE){
            // Yes, this blocks the UI thread.
            ContentValues values = mAccount.toContentValues();
            Uri result = getContentResolver().insert(Database.ACCOUNT_URI, values);
            acctId = Integer.parseInt(result.getLastPathSegment());
        }

        mAccount.setDbId(acctId);

        mLoggedIn = mRememberMe; // This is the mLoggedIn value that gets saved
                                 // to prefs. If the user didn't check "Remember Me"
                                 // we won't automatically log them in.

        // Transition to Main activity
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.putExtra(MainActivity.INTENT_EXTRA_ACCOUNT, mAccount);
        LoginActivity.this.startActivity(intent);
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
            public void run() {
                Cursor cursor = getContentResolver().query(Database.ACCOUNT_URI, null, null, null, null);
                gotAccountCursor(cursor);
            }
        }).start();
    }

    private void gotAccountCursor(final Cursor c){
        mHandler.post(new Runnable() {
            public void run() {
                mAcctCursor = c;
                startManagingCursor(mAcctCursor); 

                findViewById(R.id.login_saved_key)
                        .setVisibility((mAcctCursor.getCount() > 0)
                                ? View.VISIBLE : View.GONE);

                switch(mOnRecvCursor){
                // On an activity recreate (following orientation change, etc)
                // we need to immediately call promptToUseSavedKey if the dialog
                // was displayed prior to the activity being destroyed.
                case RECV_CURSOR_PROMPT:
                    mAlertDialog = Dialogs.promptToUseSavedKey(
                                        LoginActivity.this, mAcctCursor);
                    break;
                // And sometimes we're resuming a previous session and just need the
                // cursor to determine the account id
                case RECV_CURSOR_LOGIN:
                    doLogin();
                    break;
                }
                mOnRecvCursor = RECV_CURSOR_NOTHING;
            }
        });
    }

    /* Saved Preferences */
    protected void loadConfig(){
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String alias = prefs.getString(getString(R.string.pref_alias), "");
        String uid = prefs.getString(getString(R.string.pref_userid), "");
        String key = prefs.getString(getString(R.string.pref_apikey), "");
        mRememberMe = prefs.getBoolean(getString(R.string.pref_rememberme), true);
        mLoggedIn = prefs.getBoolean(getString(R.string.pref_loggedin), false);

        setUserAndKey(alias, uid, key);
    }

    protected void saveConfig(){
        SharedPreferences config = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = config.edit();

        editor.putString(getString(R.string.pref_alias), mAccount.getAlias());
        editor.putString(getString(R.string.pref_userid), mAccount.getUid());
        editor.putString(getString(R.string.pref_apikey), mAccount.getKey());

        editor.putBoolean(getString(R.string.pref_rememberme), mRememberMe);

        editor.putBoolean(getString(R.string.pref_loggedin), mLoggedIn);

        editor.commit();
    }

    protected void setUserAndKey(String alias, String uid, String key){
        if(TextUtils.isEmpty(alias))
            alias = "New User";
        setUserAndKey(new Account(alias, uid, key));
    }

    protected void setUserAndKey(Account acct){
        mAccount = acct;
        ((EditText) findViewById(R.id.userid_edittext)).setText(acct.getUid());
        ((EditText) findViewById(R.id.apikey_edittext)).setText(acct.getKey());
        validateUserId();
        validateApiKey();
    }

    /* Input validation */
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
    protected void showPrevious() {
        ViewFlipper vf = (ViewFlipper)findViewById(R.id.login_view_flipper);
        if(vf.getCurrentView().getId() == R.id.login_view_editables){
            vf.setInAnimation(AnimationUtils.loadAnimation(LoginActivity.this, R.anim.slide_in_previous));
            vf.setOutAnimation(AnimationUtils.loadAnimation(LoginActivity.this, R.anim.slide_out_previous));
            vf.showPrevious();
        }
    }

    protected void showNext() {
        ViewFlipper vf = (ViewFlipper)findViewById(R.id.login_view_flipper);
        if(vf.getCurrentView().getId() == R.id.login_view_options){
            vf.setInAnimation(AnimationUtils.loadAnimation(LoginActivity.this, R.anim.slide_in_next));
            vf.setOutAnimation(AnimationUtils.loadAnimation(LoginActivity.this, R.anim.slide_out_next));
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
            Intent intent = new Intent(LoginActivity.this, ManageAccountsActivity.class);
            startActivity(intent);
            return true;
        case R.id.opt_use_saved_key:
            if (mAcctCursor != null) {
                mAlertDialog = Dialogs.promptToUseSavedKey(
                                    LoginActivity.this, mAcctCursor);
            } else {
                mOnRecvCursor = RECV_CURSOR_PROMPT;
            }
            return true;
        case R.id.opt_help:
            mAlertDialog = Dialogs.showLoginHelp(LoginActivity.this);
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
                    mAlertDialog = Dialogs.promptToUseSavedKey(
                                        LoginActivity.this, mAcctCursor);
                }else{
                    mOnRecvCursor = RECV_CURSOR_PROMPT;
                }
                break;

            case R.id.login_by_web:
                mAlertDialog = Dialogs.informUserAboutLogin(LoginActivity.this);
                break;
            
            case R.id.login_manually:
                setUserAndKey("","","");
                showNext();
                break;

            case R.id.login_submit:
                if(mAcctCursor != null){
                    doLogin();
                }else{
                    mOnRecvCursor = RECV_CURSOR_LOGIN;
                }
                break;

            case R.id.login_cancel:
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
            case R.id.userid_edittext:
                mAccount.setUid(((EditText)v).getText().toString());
                if(mAccount.hasValidUserId()) // Doesn't use validateUserId so as
                    ((EditText) v).setError(null); // to avoid setting a new error
                break;
            case R.id.apikey_edittext:
                mAccount.setKey(((EditText)v).getText().toString());
                if(mAccount.hasValidApiKey()) // Same deal
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
            case R.id.userid_edittext:
                mAccount.setUid(((EditText)v).getText().toString());
                validateUserId();
                break;
            case R.id.apikey_edittext:
                mAccount.setKey(((EditText)v).getText().toString());
                validateApiKey();
                break;
            }
        }
    };
    
    private final CheckBox.OnCheckedChangeListener cbListener = new CheckBox.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton checkbox, boolean checked) {
            mRememberMe = checked;
        }
    };
}