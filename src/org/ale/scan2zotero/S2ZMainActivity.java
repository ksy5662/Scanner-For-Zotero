package org.ale.scan2zotero;

import java.util.ArrayList;
import java.util.Set;

import org.ale.scan2zotero.data.Access;
import org.ale.scan2zotero.data.Account;
import org.ale.scan2zotero.data.BibItem;
import org.ale.scan2zotero.data.Group;
import org.ale.scan2zotero.data.S2ZDatabase;
import org.ale.scan2zotero.web.googlebooks.GoogleBooksAPIClient;
import org.ale.scan2zotero.web.googlebooks.GoogleBooksHandler;
import org.ale.scan2zotero.web.zotero.ZoteroAPIClient;
import org.ale.scan2zotero.web.zotero.ZoteroHandler;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;

public class S2ZMainActivity extends Activity {

    private static final String CLASS_TAG = S2ZMainActivity.class.getCanonicalName();

    private static final int RESULT_SCAN = 0;

    public static final String INTENT_EXTRA_ACCOUNT = "ACCOUNT";

    public static final String RC_PEND = "PENDING";
    public static final String RC_PEND_STAT = "STATUS";
    public static final String RC_CHECKED = "CHECKED";
    public static final String RC_ACCESS = "ACCESS";
    public static final String RC_NUM_GROUPS = "GROUPS";

    private ZoteroAPIClient mZAPI;
    private GoogleBooksAPIClient mBooksAPI;

    private BibItemListAdapter mItemAdapter;

    private AlertDialog mAlertDialog = null;

    private ArrayList<String> mPendingItems;
    private ArrayList<Integer> mPendingStatus;
    private PendingListAdapter  mPendingAdapter;
    private ListView mPendingList;

    private Account mAccount;

    private Access mAccountAccess;

    private Handler mHandler;

    private int mNumGroups;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.main);

        findViewById(R.id.scan_isbn).setOnClickListener(scanIsbn);
        findViewById(R.id.upload).setOnClickListener(uploadSelected);

        mHandler = new Handler();

        // Initialize Google Books API Client
        mBooksAPI = new GoogleBooksAPIClient();

        // Initialize Zotero API Client
        mAccount = (Account) getIntent().getExtras()
                                    .getParcelable(INTENT_EXTRA_ACCOUNT);
        mZAPI = new ZoteroAPIClient();
        mZAPI.setAccount(mAccount);

        // BibItem list for fetched but not-yet uploaded items
        ExpandableListView bibItemList =
                               (ExpandableListView)findViewById(R.id.bib_items);
        bibItemList.setChoiceMode(ExpandableListView.CHOICE_MODE_MULTIPLE);

        // Pending item list for items being fetched (resides as header inside
        // BibItem list)
        RelativeLayout pendingListHolder = (RelativeLayout) getLayoutInflater()
                                            .inflate(R.layout.pending_item_list,
                                                     bibItemList, false);

        mPendingList = (ListView) pendingListHolder.findViewById(R.id.pending_item_list);
        bibItemList.addHeaderView(pendingListHolder);

        // Initialize list adapters
        mItemAdapter = new BibItemListAdapter(S2ZMainActivity.this);

        if(state == null){ // Fresh activity
            mPendingItems = new ArrayList<String>(2);
            mPendingStatus = new ArrayList<Integer>(2);
            mAccountAccess = null; // will check for permissions in onResume
            mNumGroups = -1;
        }else{ // Recreating activity
            // Rebuild pending list
            mAccountAccess = state.getParcelable(RC_ACCESS);
            if(state.containsKey(RC_PEND) &&
               state.containsKey(RC_PEND_STAT)) {
                mPendingItems = state.getStringArrayList(RC_PEND);
                mPendingStatus = state.getIntegerArrayList(RC_PEND_STAT);
            }
            
            // Set checked items
            if(state.containsKey(RC_CHECKED)) {
                boolean[] checked = state.getBooleanArray(RC_CHECKED);
                mItemAdapter.setChecked(checked);
            }

            mNumGroups = state.getInt(RC_NUM_GROUPS);
        }

        registerForContextMenu(bibItemList);
        registerForContextMenu(mPendingList);

        mPendingAdapter = new PendingListAdapter(S2ZMainActivity.this,
                                                   R.layout.pending_item,
                                                   R.id.pending_item_id,
                                                   mPendingItems,
                                                   mPendingStatus);

        mPendingList.setAdapter(mPendingAdapter);
        bibItemList.setAdapter(mItemAdapter);

        mItemAdapter.fillFromDatabase(mAccount.getDbId());
    }

    @Override
    public void onPause() {
        super.onPause();

        GoogleBooksHandler.getInstance().unregisterActivity();
        ZoteroHandler.getInstance().unregisterActivity();

        mItemAdapter.prepForDestruction();

        if(mAlertDialog != null){
            mAlertDialog.dismiss();
            mAlertDialog = null;
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        GoogleBooksHandler.getInstance().registerActivity(S2ZMainActivity.this);
        ZoteroHandler.getInstance().registerActivity(S2ZMainActivity.this);

        mItemAdapter.readyToGo();

        if(mAccountAccess == null
                && S2ZDialogs.displayedDialog != S2ZDialogs.DIALOG_NO_PERMS){
            S2ZDialogs.displayedDialog = S2ZDialogs.DIALOG_CREDENTIALS;
            lookupAuthorizations();
        }

        int pendVis = mPendingAdapter.getCount() > 0 ? View.VISIBLE : View.GONE;
        mPendingList.setVisibility(pendVis);

        showOrHideUploadButton();

        // Display any dialogs we were displaying before being destroyed
        switch(S2ZDialogs.displayedDialog) {
        case(S2ZDialogs.DIALOG_ZXING):
            mAlertDialog = S2ZDialogs.getZxingScanner(S2ZMainActivity.this);
            break;
        case(S2ZDialogs.DIALOG_CREDENTIALS):
            mAlertDialog = S2ZDialogs.showCheckingCredentialsDialog(S2ZMainActivity.this);
            break;
        case(S2ZDialogs.DIALOG_NO_PERMS):
            mAlertDialog = S2ZDialogs.showNoPermissionsDialog(S2ZMainActivity.this);
            break;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle state){
        super.onSaveInstanceState(state);
        state.putStringArrayList(RC_PEND, mPendingItems);
        state.putIntegerArrayList(RC_PEND_STAT, mPendingStatus);
        state.putBooleanArray(RC_CHECKED, mItemAdapter.getChecked());
        state.putParcelable(RC_ACCESS, mAccountAccess);
        state.putInt(RC_NUM_GROUPS, mNumGroups);
    }

    public void logout(){
        Intent intent = new Intent(S2ZMainActivity.this, S2ZLoginActivity.class);
        intent.putExtra(S2ZLoginActivity.INTENT_EXTRA_CLEAR_FIELDS, true);
        S2ZMainActivity.this.startActivity(intent);
        finish();
    }

    public void lookupAuthorizations() {
        new Thread(new Runnable(){
            @Override
            public void run() {
                Cursor c = getContentResolver()
                            .query(S2ZDatabase.ACCESS_URI,
                                    new String[]{Access.COL_GROUP, Access.COL_PERMISSION}, 
                                    Access.COL_KEY+"=?",
                                    new String[] {String.valueOf(mAccount.getDbId())},
                                    null);
                if(c.getCount() == 0) { // Found no permissions
                    // Will call postAccountPermissions in ZoteroHandler if successful
                    mZAPI.getPermissions();
                    mNumGroups = -1; // Force group fetching
                }else{
                    Access access = Access.fromCursor(c, mAccount.getDbId());
                    mNumGroups = access.getGroupCount();
                    postAccountPermissions(access);
                }
                c.close();
            }
        }).start();
    }

    public void postAccountPermissions(final Access perms){
        // Access perms is always returned from a background thread, so here
        // we save the permissions and launch new threads to fetch group titles
        // and collections.
        mHandler.post(new Runnable() {
        public void run() {
            if(S2ZDialogs.displayedDialog == S2ZDialogs.DIALOG_CREDENTIALS){
                if(mAlertDialog != null)
                    mAlertDialog.dismiss();
                S2ZDialogs.displayedDialog = S2ZDialogs.DIALOG_NO_DIALOG;
            }

            if(perms == null || !perms.canWrite()){
                // Results in user logging out
                mAlertDialog = S2ZDialogs.showNoPermissionsDialog(S2ZMainActivity.this);
            }else{
                // User should be ready to go, lookup their groups and collections
                // in the background.
                mAccountAccess = perms;
                lookupGroups();
            }
        }
        });
    }

    public void lookupGroups() {
        new Thread(new Runnable(){
            public void run() {
                if(mNumGroups < 0){ // Brand new key
                    mZAPI.getGroups();
                }else if(mNumGroups > 0){ // Check if we have all the group titles
                    Set<Integer> groups = mAccountAccess.getGroupIds();
                    String selection = TextUtils.join(",", groups);
                    Cursor c = getContentResolver()
                                .query(S2ZDatabase.GROUP_URI,
                                        new String[]{Group._ID}, 
                                        Access.COL_KEY+" IN (?)",
                                        new String[] {selection},
                                        null);

                    // Figure out which groups we don't have
                    c.moveToFirst();
                    while(!c.isAfterLast()){
                        int haveGroupId = c.getInt(0);
                        groups.remove(haveGroupId);
                        c.moveToNext();
                    }
                    if(groups.size() > 0){
                        // Make new database entries for any new groups. Mapping each
                        // id to "<unknown>" temporarily.
                        ContentValues[] values = new ContentValues[groups.size()];
                        int i = 0;
                        for(Integer gid : groups){
                            values[i] = new ContentValues();
                            values[i].put(Group._ID, gid);
                            values[i].put(Group.COL_TITLE, "<unknown>");
                            i++;
                        }
                        getContentResolver().bulkInsert(S2ZDatabase.GROUP_URI, values);
                        mZAPI.getGroups();
                    }
                    c.close();
                }
            }
        }).start();
    }

    public void gotBibInfo(String isbn, JSONObject info){
        // Fill in form from online info.
        BibItem item = new BibItem(BibItem.TYPE_BOOK, info, mAccount.getDbId());
        mPendingAdapter.remove(isbn);
        if(mPendingAdapter.getCount() == 0)
            mPendingList.setVisibility(View.GONE);
        if(mItemAdapter.getGroupCount() > 0)
            findViewById(R.id.upload).setVisibility(View.VISIBLE);
        mItemAdapter.addItem(item);
    }
    
    public void itemFailed(String isbn, Integer status){
        mPendingAdapter.setStatus(isbn, status);
    }

    public void showOrHideUploadButton(){
       if(mItemAdapter.getGroupCount() > 0)
           findViewById(R.id.upload).setVisibility(View.VISIBLE);
       else
           findViewById(R.id.upload).setVisibility(View.GONE);
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
            mZAPI.newCollection("Temp", "");
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
        Log.d("CreateContextMenu", " "+menu+" "+v+" "+menuInfo);
        if(menuInfo instanceof ExpandableListContextMenuInfo){
            ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) menuInfo;
            int type = ExpandableListView.getPackedPositionType(info.packedPosition);
            int group = ExpandableListView.getPackedPositionGroup(info.packedPosition);
            if(type != ExpandableListView.PACKED_POSITION_TYPE_NULL){
                // It's not in the header
                inflater.inflate(R.menu.bib_item_context_menu, menu);

                menu.setHeaderTitle(mItemAdapter.getTitleOfGroup(group));
            }
        }else if(menuInfo instanceof AdapterContextMenuInfo){
            if(v.getId() != R.id.pending_item_list){
                AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
                inflater.inflate(R.menu.pending_item_context_menu, menu);
                menu.setHeaderTitle(mPendingAdapter.getItem(info.position));
            }
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {        
        switch (item.getItemId()) {
        case R.id.ctx_edit:
            ExpandableListContextMenuInfo einfo = (ExpandableListContextMenuInfo) item.getMenuInfo();

            BibItem toEdit = (BibItem) mItemAdapter.getGroup((int) einfo.id);
            Intent intent = new Intent(S2ZMainActivity.this, S2ZEditActivity.class);
            intent.putExtra(S2ZEditActivity.INTENT_EXTRA_BIBITEM, toEdit);
            break;
        case R.id.ctx_delete:
            ExpandableListContextMenuInfo dinfo = (ExpandableListContextMenuInfo) item.getMenuInfo();

            mItemAdapter.deleteItem((int) dinfo.id);
            if(mItemAdapter.getGroupCount() == 0)
                findViewById(R.id.upload).setVisibility(View.GONE);
            break;
        case R.id.ctx_cancel:
            AdapterContextMenuInfo cinfo = (AdapterContextMenuInfo) item.getMenuInfo();

            mPendingAdapter.remove(mPendingAdapter.getItem(cinfo.position));
            if(mPendingAdapter.getCount() == 0)
                mPendingList.setVisibility(View.GONE);
            break;
        case R.id.ctx_retry:
            AdapterContextMenuInfo rinfo = (AdapterContextMenuInfo) item.getMenuInfo();
            String ident = mPendingAdapter.getItem(rinfo.position);
            if(mPendingAdapter.getStatus(ident) != PendingListAdapter.STATUS_LOADING){
                mBooksAPI.isbnLookup(ident);
                mPendingAdapter.setStatus(ident, PendingListAdapter.STATUS_LOADING);
            }
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
                // Don't do anything if we're still loading the item
                // Otherwise, try looking it up again.
                if(mPendingAdapter.hasItem(content)){
                    if(mPendingAdapter.getStatus(content) == 
                            PendingListAdapter.STATUS_LOADING)
                    {
                        break;
                    }
                }else{
                    mPendingAdapter.add(content);
                    mPendingList.setVisibility(View.VISIBLE); 
                }
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

    private final Button.OnClickListener uploadSelected = new Button.OnClickListener() {
        public void onClick(View v) {
            boolean[] checked = mItemAdapter.getChecked();
            JSONObject items = new JSONObject();
            JSONObject nxt;
            try {
                items.put("items", new JSONArray());
                for(int b=0; b<checked.length; b++){
                    if(!checked[b]) continue;

                    nxt = ((BibItem)mItemAdapter.getGroup(b)).getSelectedInfo();
                    items.accumulate("items", nxt);
                }
            } catch (JSONException e) {
                // TODO Prompt about failure
                e.printStackTrace();
                // Clear the selection
                mItemAdapter.setChecked(new boolean[0]);
            }
            mZAPI.addItems(items);
        }
    };
}
