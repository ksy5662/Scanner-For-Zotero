package org.ale.scan2zotero;

import java.util.ArrayList;

import org.ale.scan2zotero.data.Account;
import org.ale.scan2zotero.data.BibItem;
import org.ale.scan2zotero.web.GoogleBooksAPIClient;
import org.ale.scan2zotero.web.GoogleBooksHandler;
import org.ale.scan2zotero.web.ZoteroAPIClient;
import org.ale.scan2zotero.web.ZoteroHandler;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ExpandableListView;
import android.widget.ListView;

public class S2ZMainActivity extends Activity {

    private static final String CLASS_TAG = S2ZMainActivity.class.getCanonicalName();

    private static final int RESULT_SCAN = 0;

    public static final String INTENT_EXTRA_ACCOUNT = "ACCOUNT";

    public static final String RC_PEND = "PENDING";
    public static final String RC_PEND_STAT = "STATUS";
    public static final String RC_CHECKED = "CHECKED";

    private ZoteroAPIClient mZAPI;
    private GoogleBooksAPIClient mBooksAPI;

    private BibItemListAdapter mItemList;
    private GoogleBooksHandler mGoogleBooksHandler;
    private ZoteroHandler mZoteroHandler;

    private AlertDialog mAlertDialog = null;

    private ArrayList<String> mPendingItems;
    private ArrayList<Integer> mPendingStatus;
    private PendingListAdapter  mPendingAdapter;

    private int mAccountId;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.main);

        findViewById(R.id.scan_isbn).setOnClickListener(scanIsbn);

        // Initialize Google Books API Client
        mBooksAPI = new GoogleBooksAPIClient(mGoogleBooksHandler);

        // Initialize Zotero API Client
        Account acct = (Account) getIntent().getExtras()
                                    .getParcelable(INTENT_EXTRA_ACCOUNT);
        mZAPI = new ZoteroAPIClient();
        mZAPI.setAccount(acct);
        mAccountId = acct.getDbId();

        // Handlers for HTTPS results
        mGoogleBooksHandler = GoogleBooksHandler.getInstance();
        mZoteroHandler = ZoteroHandler.getInstance();


        // BibItem list for fetched but not-yet uploaded items
        ExpandableListView bibItemList =
                               (ExpandableListView)findViewById(R.id.bib_items);
        bibItemList.setChoiceMode(ExpandableListView.CHOICE_MODE_MULTIPLE);

        // Pending item list for items being fetched (resides as header inside
        // BibItem list
        ListView pendingList = (ListView) getLayoutInflater()
                                            .inflate(R.layout.pending_item_list,
                                                     bibItemList, false);
        bibItemList.addHeaderView(pendingList);

        // Initialize list adapters
        mItemList = new BibItemListAdapter(S2ZMainActivity.this);

        if(state == null){
            mPendingItems = new ArrayList<String>(2);
            mPendingStatus = new ArrayList<Integer>(2);
        }else{
            if(state.containsKey(RC_PEND) &&
               state.containsKey(RC_PEND_STAT)) {
                mPendingItems = state.getStringArrayList(RC_PEND);
                mPendingStatus = state.getIntegerArrayList(RC_PEND_STAT);
            }
            if(state.containsKey(RC_CHECKED)) {
                boolean[] checked = state.getBooleanArray(RC_CHECKED);
                for(int c = 0; c < checked.length; c++){
                    ((CheckBox)bibItemList.getChildAt(c)
                            .findViewById(R.id.bib_row_checkbox))
                            .setChecked(checked[c]);
                }
            }
        }

        registerForContextMenu(bibItemList);
        registerForContextMenu(pendingList);

        mPendingAdapter = new PendingListAdapter(S2ZMainActivity.this,
                                                   R.layout.pending_item,
                                                   R.id.pending_item_id,
                                                   mPendingItems,
                                                   mPendingStatus);

        pendingList.setAdapter(mPendingAdapter);
        bibItemList.setAdapter(mItemList);

        mItemList.fillFromDatabase(mAccountId);
    }

    @Override
    public void onPause() {
        super.onPause();

        mGoogleBooksHandler.unregisterActivity();
        mZoteroHandler.unregisterActivity();

        if(mAlertDialog != null){
            mAlertDialog.dismiss();
            mAlertDialog = null;
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        mGoogleBooksHandler.registerActivity(S2ZMainActivity.this);
        mZoteroHandler.registerActivity(S2ZMainActivity.this);

        // Display any dialogs we were displaying before being destroyed
        switch(S2ZDialogs.displayedDialog) {
        case(S2ZDialogs.DIALOG_NO_DIALOG):
            break;
        case(S2ZDialogs.DIALOG_ZXING):
            mAlertDialog = S2ZDialogs.getZxingScanner(S2ZMainActivity.this);
            break;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle state){
        super.onSaveInstanceState(state);
        state.putStringArrayList(RC_PEND, mPendingItems);
        state.putIntegerArrayList(RC_PEND_STAT, mPendingStatus);
        ExpandableListView elv=(ExpandableListView)findViewById(R.id.bib_items);
        boolean[] checked = new boolean[elv.getCount()];
        for(int i=0; i<checked.length; i++){
            checked[i] = ((CheckBox)elv.getChildAt(i).findViewById(R.id.bib_row_checkbox)).isChecked();
            Log.d(CLASS_TAG, checked[i]+"wtfwtfwtf");
        }
        state.putBooleanArray(RC_CHECKED, checked);
    }

    public void logout(){
        Intent intent = new Intent(S2ZMainActivity.this, S2ZLoginActivity.class);
        intent.putExtra(S2ZLoginActivity.INTENT_EXTRA_CLEAR_FIELDS, true);
        S2ZMainActivity.this.startActivity(intent);
        finish();
    }

    public void gotBibInfo(String isbn, JSONObject info){
        // Fill in form from online info.
        BibItem item = new BibItem(BibItem.TYPE_BOOK, info, mAccountId);
        mPendingAdapter.remove(isbn);
        mItemList.addItem(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
        case R.id.ctx_collection:
            return true;
        case R.id.ctx_logout:
            logout();
            return true;
        case R.id.ctx_about:
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        Log.d(CLASS_TAG, "Calling createcontextmenu " + v.getId());
        switch(v.getId()){
        case R.id.pending_item_holder:
            inflater.inflate(R.menu.pending_item_context_menu, menu);
            break;
        case R.id.bib_items:
            inflater.inflate(R.menu.bib_item_context_menu, menu);
            break;
        }
        menu.setHeaderTitle("Options");
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        ExpandableListView.ExpandableListContextMenuInfo info = 
            (ExpandableListView.ExpandableListContextMenuInfo) item.getMenuInfo();

        switch (item.getItemId()) {
        case R.id.ctx_edit:
            BibItem toEdit = (BibItem) mItemList.getGroup((int) info.id);
            Intent intent = new Intent(S2ZMainActivity.this, S2ZEditActivity.class);
            intent.putExtra(S2ZEditActivity.INTENT_EXTRA_BIBITEM, toEdit);
            break;
        case R.id.ctx_delete:
            mItemList.deleteItem((int) info.id);
            break;
        case R.id.ctx_cancel:
            mPendingAdapter.remove(mPendingAdapter.getItem(item.getItemId()));
            break;
        //case R.id.ctx_retry:
        default:
            return super.onContextItemSelected(item);
        }
        return true;
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
            default:
                Log.d(CLASS_TAG, "Scan error");
        }
    }

    private void handleBarcode(String content, String format){
        switch(Util.parseBarcode(content, format)) {
            case(Util.SCAN_PARSE_ISBN):
                Log.d(CLASS_TAG, "Looking up ISBN:"+content);
                mPendingAdapter.add(content);
                mBooksAPI.isbnLookup(content);
                break;
            case(Util.SCAN_PARSE_ISSN):
                Log.d(CLASS_TAG, "Looking up ISSN:"+content);
                break;
            default:
                Log.d(CLASS_TAG, "Could not handle code: "+content+" of type "+format);
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
                S2ZDialogs.getZxingScanner(S2ZMainActivity.this);
            }
        }
    };

    private final Button.OnClickListener getGroups = new Button.OnClickListener() {
        public void onClick(View v) {
            Log.d(CLASS_TAG, "Starting get groups");
            mZAPI.getUsersGroups();
        }
    };
}
