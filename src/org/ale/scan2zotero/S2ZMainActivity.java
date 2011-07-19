package org.ale.scan2zotero;

import org.ale.scan2zotero.data.Account;
import org.ale.scan2zotero.web.APIRequest;
import org.ale.scan2zotero.web.GoogleBooksAPIClient;
import org.ale.scan2zotero.web.ZoteroAPIClient;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

public class S2ZMainActivity extends Activity {

    private static final String CLASS_TAG = S2ZMainActivity.class.getCanonicalName();

    private static final int RESULT_SCAN = 0;

    private ZoteroAPIClient mZAPI;
    private GoogleBooksAPIClient mBooksAPI;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        findViewById(R.id.scan_isbn).setOnClickListener(scanIsbn);

        // Initialize Zotero API Client
        Bundle extras = getIntent().getExtras();
        if(extras != null){
            Account acct = (Account) extras.getParcelable(S2ZLoginActivity.ACCOUNT_EXTRA);
            mZAPI = new ZoteroAPIClient(mZoteroAPIHandler);
            mZAPI.setAccount(acct);
        }else{
            mZAPI = null;
        }
        
        // Initialize Google Books API Client
        mBooksAPI = new GoogleBooksAPIClient(mGoogleBooksHandler);
    }

    @Override
    public void onResume(){
        super.onResume();
        if(mZAPI == null) logout();
    }

    public void logout(){
        Intent intent = new Intent(S2ZMainActivity.this, S2ZLoginActivity.class);
        intent.putExtra(S2ZLoginActivity.CLEAR_FIELDS, true);
        S2ZMainActivity.this.startActivity(intent);
        finish();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.context_menu_main, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
        case R.id.ctx_logout:
            logout();
            return true;
        case R.id.ctx_settings:
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        switch(requestCode){
            case RESULT_SCAN:
                if (resultCode == RESULT_OK) {
                    String contents = intent.getStringExtra("SCAN_RESULT"); // The scanned ISBN
                    String format = intent.getStringExtra("SCAN_RESULT_FORMAT"); // "EAN 13"
                    handleBarcode(contents, format);
                }
                break;
        }
    }

    private void handleBarcode(String content, String format){
        switch(Util.parseBarcode(content, format)) {
            case(Util.SCAN_PARSE_ISBN):
                Log.d(CLASS_TAG, "Looking up ISBN:"+content);
                mBooksAPI.isbnLookup(content);
                break;
            case(Util.SCAN_PARSE_ISSN):
                Log.d(CLASS_TAG, "Looking up ISSN:"+content);
                break;
            default:
                Log.d(CLASS_TAG, "Could not handle code:"+content+" of type "+format);
                // XXX: Report scan failure
                break;
        }
    }

    private final Button.OnClickListener scanIsbn = new Button.OnClickListener() {
        public void onClick(View v) {
            try{
                Intent intent = new Intent(getString(R.string.zxing_intent_scan));
                intent.setPackage(getString(R.string.zxing_pkg));
                intent.putExtra("SCAN_MODE", "ONE_D_MODE");
                startActivityForResult(intent, RESULT_SCAN);
            } catch (ActivityNotFoundException e) {
                // Ask the user if we should install ZXing scanner
                Util.getZxingScanner(S2ZMainActivity.this);
            }
        }
    };

    private final Button.OnClickListener getGroups = new Button.OnClickListener() {
        public void onClick(View v) {
            Log.d(CLASS_TAG, "Starting get groups");
            mZAPI.getUsersGroups();
        }
    };
    

    private final Handler mZoteroAPIHandler = new Handler(){
        ProgressDialog dialog = null;
        public void handleMessage(Message msg){
            switch(msg.what){
            case APIRequest.START:
                dialog = ProgressDialog.show(S2ZMainActivity.this, "", 
                        "Fetching bibliographic information...", true);
                break;
            case APIRequest.EXCEPTION:
                if(dialog != null) dialog.dismiss();
                ((Exception)msg.obj).printStackTrace();
                break;
            case APIRequest.SUCCESS:
                if(dialog != null) dialog.dismiss();
                Log.d(CLASS_TAG, (String)msg.obj);
                break;
            }
        }
    };

    private final Handler mGoogleBooksHandler = new Handler(){
        public void handleMessage(Message msg){
            switch(msg.what){
            case APIRequest.START:
                break;
            case APIRequest.EXCEPTION:
                ((Exception)msg.obj).printStackTrace();
                break;
            case APIRequest.SUCCESS:
                Log.d(CLASS_TAG, (String)msg.obj);
                break;
            }
        }
    };
}
