package org.ale.scan2zotero;

import java.util.ArrayList;
import java.util.Set;

import org.ale.scan2zotero.PString;
import org.ale.scan2zotero.data.Access;
import org.ale.scan2zotero.data.Account;
import org.ale.scan2zotero.data.BibItem;
import org.ale.scan2zotero.data.BibItemDBHandler;
import org.ale.scan2zotero.data.Group;
import org.ale.scan2zotero.data.Database;
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
import android.util.SparseArray;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.widget.RelativeLayout.LayoutParams;

public class MainActivity extends Activity {

    private static final String CLASS_TAG = MainActivity.class.getCanonicalName();

    private static final int RESULT_SCAN = 0;

    public static final String INTENT_EXTRA_ACCOUNT = "ACCOUNT";

    public static final String RC_PEND = "PENDING";
    public static final String RC_PEND_STAT = "STATUS";
    public static final String RC_CHECKED = "CHECKED";
    public static final String RC_ACCESS = "ACCESS";
    public static final String RC_NEW_KEY = "NEWKEY";
    public static final String RC_GROUPS = "GROUPS";

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

    public Handler mUIThreadHandler;

    private boolean mNewKey;
    
    private SparseParcelableArrayAdapter<PString> mGroupAdapter;
    //private SparseParcelableArrayAdapter mCollectionAdapter;
    private int mSelectedGroup;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.main);
        Bundle extras = getIntent().getExtras();

        mUIThreadHandler = new Handler();

        // Logged in account
        mAccount = (Account) extras.getParcelable(INTENT_EXTRA_ACCOUNT);

        // Initialize Clients
        mBooksAPI = new GoogleBooksAPIClient();
        mZAPI = new ZoteroAPIClient();
        mZAPI.setAccount(mAccount);

        // BibItem list
        ExpandableListView bibItemList = (ExpandableListView) findViewById(R.id.bib_items);

        // Pending item list
        View pendingListHolder = getLayoutInflater().inflate(
                R.layout.pending_item_list, bibItemList, false);

        mPendingList = (ListView) pendingListHolder.findViewById(R.id.pending_item_list);
        bibItemList.addHeaderView(pendingListHolder);
        Spinner groupList = (Spinner) findViewById(R.id.upload_group);

        boolean[] checked;
        SparseArray<PString> groups;
        if(state == null){ // Fresh activity
            mAccountAccess = null; // will check for permissions in onResume
            mPendingItems = new ArrayList<String>(2); // RC_PEND
            mPendingStatus = new ArrayList<Integer>(2); // RC_PEND_STAT
            checked = new boolean[0];
            mNewKey = true;
            groups = new SparseArray<PString>();
        }else{ // Recreating activity
            // Rebuild pending list
            mAccountAccess = state.getParcelable(RC_ACCESS);
            mPendingItems = state.getStringArrayList(RC_PEND);
            mPendingStatus = state.getIntegerArrayList(RC_PEND_STAT);
            // Set checked items
            checked = state.getBooleanArray(RC_CHECKED);

            mNewKey = state.getBoolean(RC_NEW_KEY);
            groups = state.getSparseParcelableArray(RC_GROUPS);
        }

        // Initialize list adapters
        mItemAdapter = new BibItemListAdapter(MainActivity.this);
        mItemAdapter.setChecked(checked);

        mGroupAdapter = new SparseParcelableArrayAdapter<PString>(
                MainActivity.this, groups);
        mPendingAdapter = new PendingListAdapter(MainActivity.this,
                R.layout.pending_item, R.id.pending_item_id, mPendingItems,
                mPendingStatus);

        bibItemList.setAdapter(mItemAdapter);
        groupList.setAdapter(mGroupAdapter);
        mPendingList.setAdapter(mPendingAdapter);

        mItemAdapter.fillFromDatabase(mAccount.getDbId());

        registerForContextMenu(bibItemList);
        registerForContextMenu(mPendingList);

        // Listeners
        groupList.setOnItemSelectedListener(spinnerListener);
        findViewById(R.id.scan_isbn).setOnClickListener(scanIsbn);
        findViewById(R.id.upload).setOnClickListener(uploadSelected);
    }

    @Override
    public void onPause() {
        super.onPause();

        GoogleBooksHandler.getInstance().unregisterActivity();
        ZoteroHandler.getInstance().unregisterActivity();
        BibItemDBHandler.getInstance().unregisterAdapter();

        if(mAlertDialog != null){
            mAlertDialog.dismiss();
            mAlertDialog = null;
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        GoogleBooksHandler.getInstance().registerActivity(MainActivity.this);
        ZoteroHandler.getInstance().registerActivity(MainActivity.this);
        BibItemDBHandler.getInstance().registerAdapter(mItemAdapter);

        if(mAccountAccess == null
                && Dialogs.displayedDialog != Dialogs.DIALOG_NO_PERMS){
            Dialogs.displayedDialog = Dialogs.DIALOG_CREDENTIALS;
            lookupAuthorizations();
        }

        int pendVis = mPendingAdapter.getCount() > 0 ? View.VISIBLE : View.GONE;
        mPendingList.setVisibility(pendVis);
        redrawPendingList();

        showOrHideUploadButton();

        // Display any dialogs we were displaying before being destroyed
        switch(Dialogs.displayedDialog) {
        case(Dialogs.DIALOG_ZXING):
            mAlertDialog = Dialogs.getZxingScanner(MainActivity.this);
            break;
        case(Dialogs.DIALOG_CREDENTIALS):
            mAlertDialog = Dialogs.showCheckingCredentialsDialog(MainActivity.this);
            break;
        case(Dialogs.DIALOG_NO_PERMS):
            mAlertDialog = Dialogs.showNoPermissionsDialog(MainActivity.this);
            break;
        }
    }

    public Account getUserAccount(){
        // hack for ZoteroHandler, which needs to know which user
        // is logged in so as to create Access objects. :/
        return mAccount;
    }

    @Override
    public void onSaveInstanceState(Bundle state){
        super.onSaveInstanceState(state);
        state.putStringArrayList(RC_PEND, mPendingItems);
        state.putIntegerArrayList(RC_PEND_STAT, mPendingStatus);
        state.putBooleanArray(RC_CHECKED, mItemAdapter.getChecked());
        state.putParcelable(RC_ACCESS, mAccountAccess);
        state.putBoolean(RC_NEW_KEY, mNewKey);
        state.putSparseParcelableArray(RC_GROUPS, mGroupAdapter.getData());
    }

    public void postToUIThread(Runnable r) {
        mUIThreadHandler.post(r);
    }
    public void logout(){
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.putExtra(LoginActivity.INTENT_EXTRA_CLEAR_FIELDS, true);
        MainActivity.this.startActivity(intent);
        finish();
    }

    public void refreshPermissions() {
        mZAPI.getPermissions();
    }
    
    public void erasePermissions(){
        final int keyid = mAccount.getDbId();
        new Thread(new Runnable(){
            public void run(){
                getContentResolver().delete(Database.ACCESS_URI,
                        Access.COL_ACCT + "=?",
                        new String[] { String.valueOf(keyid) });
            }
        });
    }

    public void lookupAuthorizations() {
        new Thread(new Runnable(){
            @Override
            public void run() {
                Cursor c = getContentResolver()
                            .query(Database.ACCESS_URI,
                                    new String[]{Access.COL_GROUP, Access.COL_PERMISSION}, 
                                    Access.COL_ACCT+"=?",
                                    new String[] {String.valueOf(mAccount.getDbId())},
                                    null);
                if(c.getCount() == 0) { // Found no permissions
                    // Will call postAccountPermissions in ZoteroHandler if successful
                    mZAPI.getPermissions();
                }else{
                    Access access = Access.fromCursor(c, mAccount.getDbId());
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
        mUIThreadHandler.post(new Runnable() {
        public void run() {
            if(Dialogs.displayedDialog == Dialogs.DIALOG_CREDENTIALS){
                if(mAlertDialog != null)
                    mAlertDialog.dismiss();
                Dialogs.displayedDialog = Dialogs.DIALOG_NO_DIALOG;
            }

            if(perms == null || !perms.canWrite()){
                // Tell the user they don't have sufficient permission
                // and log them out
                mAlertDialog = Dialogs.showNoPermissionsDialog(MainActivity.this);
            }else{
                // User should be ready to go, lookup their groups and collections
                // in the background.
                mAccountAccess = perms;
                loadGroups();
            }
        }
        });
    }

    public void loadGroups(){
        final SparseArray<PString> newGroupList = new SparseArray<PString>();
        if(mAccountAccess.getGroupCount() == 0
                && mAccountAccess.canWriteLibrary()) {
            newGroupList.put(Group.GROUP_LIBRARY, new PString(getString(R.string.my_library)));
            mGroupAdapter.replaceData(newGroupList);
            ((Spinner)findViewById(R.id.upload_group)).invalidate();
            return;
        }
        // Check that we have all the group titles
        new Thread(new Runnable(){
            public void run(){
                Set<Integer> groups = mAccountAccess.getGroupIds();
                if(mAccountAccess.canWriteLibrary()){
                    newGroupList.put(Group.GROUP_LIBRARY, new PString(getString(R.string.my_library)));
                }
                String selection = TextUtils.join(",", groups);
                Cursor c = getContentResolver()
                            .query(Database.GROUP_URI,
                                    new String[]{Group._ID, Group.COL_TITLE}, 
                                    Group._ID+" IN (?)",
                                    new String[] {selection},
                                    null);

                // Figure out which groups we don't have
                c.moveToFirst();
                while(!c.isAfterLast()){
                    int haveGroupId = c.getInt(0);
                    groups.remove(haveGroupId);
                    newGroupList.put(haveGroupId, new PString(c.getString(1)));
                    c.moveToNext();
                }
                c.close();
                // Update the spinner
                mUIThreadHandler.post(new Runnable(){
                    public void run(){
                        mGroupAdapter.replaceData(newGroupList);
                    }
                });
                // If we have any unknown groups, do a group lookup.
                if(groups.size() > 0){
                    // Make new database entries for new groups. Mapping each
                    // id to "<Group ID>" temporarily.
                    ContentValues[] values = new ContentValues[groups.size()];
                    int i = 0;
                    for(Integer gid : groups){
                        values[i] = new ContentValues();
                        values[i].put(Group._ID, gid);
                        values[i].put(Group.COL_TITLE, "<"+gid+">");
                        i++;
                    }
                    getContentResolver().bulkInsert(Database.GROUP_URI, values);
                    mZAPI.getGroups();
                }
            }
        }).start();
    }

    public void gotBibInfo(final String isbn, final JSONObject info){
        mUIThreadHandler.post(new Runnable() {
            public void run(){
                BibItem item = new BibItem(BibItem.TYPE_BOOK, info, mAccount.getDbId());
                mPendingAdapter.remove(isbn);
                if(mPendingAdapter.getCount() == 0)
                    mPendingList.setVisibility(View.GONE);
                if(mItemAdapter.getGroupCount() > 0)
                    findViewById(R.id.upload).setVisibility(View.VISIBLE);
                mItemAdapter.addItem(item);
                redrawPendingList();
            }
        });
    }

    public void itemFailed(String isbn, Integer status){
        mPendingAdapter.setStatus(isbn, status);
    }

    public void showOrHideUploadButton(){
       int vis = (mItemAdapter.getGroupCount() > 0) ? View.VISIBLE : View.GONE;
       findViewById(R.id.upload_bar).setVisibility(vis);
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
            refreshPermissions();
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
            Intent intent = new Intent(MainActivity.this, EditItemActivity.class);
            intent.putExtra(EditItemActivity.INTENT_EXTRA_BIBITEM, toEdit);
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
            redrawPendingList();
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
        if(mPendingAdapter.hasItem(content)){
            Toast.makeText(
                    MainActivity.this,
                    "This item is already in your list.",
                    Toast.LENGTH_LONG).show();
            return;
        }
        switch(Util.parseBarcode(content, format)) {
            case(Util.SCAN_PARSE_ISBN):
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
                break;
            default:
                mPendingAdapter.add(content);
                mPendingAdapter.setStatus(content, PendingListAdapter.STATUS_UNKNOWN_TYPE);
                break;
        }
        redrawPendingList();
    }

    private final AdapterView.OnItemSelectedListener spinnerListener = new AdapterView.OnItemSelectedListener(){
        @Override
        public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
            if((int)id == Group.GROUP_LIBRARY){
                mSelectedGroup = Integer.parseInt(mAccount.getUid());
            }else{
                mSelectedGroup = (int)id;
            }
        }
        @Override
        public void onNothingSelected(AdapterView<?> parent) {
            
        }
    };

    private final Button.OnClickListener scanIsbn = new Button.OnClickListener() {
        public void onClick(View v) {
            try{
                Intent intent = new Intent(getString(R.string.zxing_intent_scan));
                intent.setPackage(getString(R.string.zxing_pkg));
                intent.putExtra("SCAN_MODE", "ONE_D_MODE");
                startActivityForResult(intent, RESULT_SCAN);
            } catch (ActivityNotFoundException e) {
                // Ask the user if we should install ZXing scanner
                Dialogs.getZxingScanner(MainActivity.this);
            }
        }
    };

    private final Button.OnClickListener uploadSelected = new Button.OnClickListener() {
        public void onClick(View v) {
            ((Button)v).setClickable(false);
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
            mZAPI.addItems(items, mSelectedGroup);
        }
    };

    private void redrawPendingList(){
        /* Pretty terrible hack, Android doesn't like my ListView inside a
         * relative layout as a header in an expandable list view. Go figure.
         * It wasn't getting the height of said element correctly. */
        int count = mPendingAdapter.getCount();
        RelativeLayout r = ((RelativeLayout)findViewById(R.id.pending_item_holder));
        AbsListView.LayoutParams params = (AbsListView.LayoutParams) r.getLayoutParams();
        params.height = count * mPendingAdapter._hack_childSize;
        r.setLayoutParams(params);
    }
}
