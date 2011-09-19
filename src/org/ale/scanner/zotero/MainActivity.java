/** 
 * Copyright 2011 John M. Schanck
 * 
 * ScannerForZotero is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * ScannerForZotero is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with ScannerForZotero.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.ale.scanner.zotero;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Set;

import org.ale.scanner.zotero.PString;
import org.ale.scanner.zotero.data.Access;
import org.ale.scanner.zotero.data.Account;
import org.ale.scanner.zotero.data.BibItem;
import org.ale.scanner.zotero.data.BibItemDBHandler;
import org.ale.scanner.zotero.data.Database;
import org.ale.scanner.zotero.data.Group;
import org.ale.scanner.zotero.web.APIHandler;
import org.ale.scanner.zotero.web.googlebooks.GoogleBooksAPIClient;
import org.ale.scanner.zotero.web.worldcat.WorldCatAPIClient;
import org.ale.scanner.zotero.web.zotero.ZoteroAPIClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.view.animation.Animation;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.animation.AnimationUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;

public class MainActivity extends Activity {

    private static final String CLASS_TAG = MainActivity.class.getCanonicalName();

    private static final int RESULT_SCAN = 0;
    private static final int RESULT_EDIT = 1;

    private static final int UPLOAD_STATE_WAIT = 0;
    private static final int UPLOAD_STATE_PENDING = 1;
    private static final int UPLOAD_STATE_FAILURE = 2;

    private static final int SERVICE_GOOGLE = 0;
    private static final int SERVICE_WORLDCAT = 1;

    public static final String INTENT_EXTRA_ACCOUNT = "ACCOUNT";

    public static final String RC_PEND = "PENDING";
    public static final String RC_PEND_STAT = "STATUS";
    public static final String RC_CHECKED = "CHECKED";
    public static final String RC_ACCESS = "ACCESS";
    public static final String RC_GROUPS = "GROUPS";
    public static final String RC_UPLOADING = "UPLOADING";

    public static final String PREF_GROUP = "GROUP";
    public static final String PREF_SERVICE = "SERVICE";

    private ZoteroAPIClient mZAPI;
    private GoogleBooksAPIClient mGoogleBooksAPI;
    private WorldCatAPIClient mWorldCatAPI;

    private BibItemListAdapter mItemAdapter;

    private AlertDialog mAlertDialog = null;

    private ArrayList<String> mPendingItems;
    private ArrayList<Integer> mPendingStatus;
    private PendingListAdapter  mPendingAdapter;
    private ListView mPendingList;

    private Animation[] mAnimations;

    private Account mAccount;

    private Access mAccountAccess;

    public Handler mUIThreadHandler;

    private int mUploadState;

    private SparseArray<PString> mGroups;
    private int mSelectedGroup;
    private int mISBNService;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.main);
        Bundle extras = getIntent().getExtras();

        mUIThreadHandler = new Handler();

        // Get the account we're logged in as
        mAccount = (Account) extras.getParcelable(INTENT_EXTRA_ACCOUNT);

        // Load preferences
        SharedPreferences prefs = getSharedPreferences(mAccount.getUid(), MODE_PRIVATE);
        // The group we'll upload to (default to user's personal library)
        mSelectedGroup = prefs.getInt(PREF_GROUP, Group.GROUP_LIBRARY);
        mISBNService = prefs.getInt(PREF_SERVICE, SERVICE_GOOGLE);

        // Initialize Clients
        mGoogleBooksAPI = new GoogleBooksAPIClient();
        mWorldCatAPI = new WorldCatAPIClient();
        mZAPI = new ZoteroAPIClient();
        mZAPI.setAccount(mAccount);

        // BibItem list
        ExpandableListView bibItemList = (ExpandableListView) findViewById(R.id.bib_items);

        // Pending item list
        View pendingListHolder = getLayoutInflater().inflate(
                R.layout.pending_item_list, bibItemList, false);
        bibItemList.addHeaderView(pendingListHolder);

        mPendingList = (ListView) pendingListHolder.findViewById(R.id.pending_item_list);

        int[] checked;
        if(state == null){ // Fresh activity
            mAccountAccess = null; // will check for permissions in onResume
            mPendingItems = new ArrayList<String>(2); // RC_PEND
            mPendingStatus = new ArrayList<Integer>(2); // RC_PEND_STAT
            checked = new int[0];
            mUploadState = UPLOAD_STATE_WAIT;
            mGroups = new SparseArray<PString>();
        }else{ // Recreating activity
            // Rebuild pending list
            mAccountAccess = state.getParcelable(RC_ACCESS);
            mPendingItems = state.getStringArrayList(RC_PEND);
            mPendingStatus = state.getIntegerArrayList(RC_PEND_STAT);
            // Set checked items
            checked = state.getIntArray(RC_CHECKED);

            mUploadState = state.getInt(RC_UPLOADING);
            mGroups = state.getSparseParcelableArray(RC_GROUPS);
        }

        // Initialize list adapters
        mItemAdapter = new BibItemListAdapter(MainActivity.this);
        mItemAdapter.setChecked(checked);
        bibItemList.setAdapter(mItemAdapter);
        registerForContextMenu(bibItemList);

        mPendingAdapter = new PendingListAdapter(MainActivity.this,
                R.layout.pending_item, R.id.pending_item_id, mPendingItems,
                mPendingStatus);
        mPendingList.setAdapter(mPendingAdapter);
        registerForContextMenu(mPendingList);

        // Listeners
        findViewById(R.id.scan_isbn).setOnClickListener(scanIsbn);
        findViewById(R.id.upload).setOnClickListener(uploadSelected);

        // Load animations
        mAnimations = new Animation[]{
                AnimationUtils.loadAnimation(MainActivity.this, R.anim.slide_in_next),
                AnimationUtils.loadAnimation(MainActivity.this, R.anim.slide_out_next),
                AnimationUtils.loadAnimation(MainActivity.this, R.anim.slide_in_previous),
                AnimationUtils.loadAnimation(MainActivity.this, R.anim.slide_out_previous)
                };
        
        // Upload Bar
        findViewById(R.id.upload_progress).setOnClickListener(dismissUploadStatus);
    }

    @Override
    public void onPause() {
        super.onPause();

        APIHandler.globalUnbindActivity();
        BibItemDBHandler.getInstance().unbindAdapter();

        if(mAlertDialog != null){
            mAlertDialog.dismiss();
            mAlertDialog = null;
        }

        // Commit preferences
        SharedPreferences config = getSharedPreferences(mAccount.getUid(), MODE_PRIVATE);
        SharedPreferences.Editor editor = config.edit();
        editor.putInt(PREF_GROUP, mSelectedGroup);
        editor.putInt(PREF_SERVICE, mISBNService);
        editor.commit();
    }

    @Override
    public void onResume() {
        super.onResume();

        APIHandler.globalBindActivity(MainActivity.this);
        BibItemDBHandler.getInstance().bindAdapter(mItemAdapter);

        // Try filling the item list from the database if it is currently
        // empty. This is an easy workaround against double-loading the list,
        // or failing to load it at all.
        if(mItemAdapter.getGroupCount() == 0) {
            mItemAdapter.fillFromDatabase(mAccount.getDbId());
        }

        // Show the correct upload bar
        if(mUploadState == UPLOAD_STATE_PENDING){
            showUploadInProgress();
        }else{
            resetUploadStatus();
        }

        // Lookup account authorizations if necessary
        if(mAccountAccess == null
                && (Dialogs.displayedDialog != Dialogs.DIALOG_NO_PERMS)){
            Dialogs.displayedDialog = Dialogs.DIALOG_CREDENTIALS;
        }

        // Draw the pending list
        int pendVis = mPendingAdapter.getCount() > 0 ? View.VISIBLE : View.GONE;
        mPendingList.setVisibility(pendVis);
        redrawPendingList();

        // Display any dialogs we were displaying before being destroyed
        switch(Dialogs.displayedDialog) {
        case(Dialogs.DIALOG_ZXING):
            mAlertDialog = Dialogs.getZxingScanner(MainActivity.this);
            break;
        case(Dialogs.DIALOG_CREDENTIALS):
            lookupAuthorizations();
            break;
        case(Dialogs.DIALOG_NO_PERMS):
            mAlertDialog = Dialogs.showNoPermissionsDialog(MainActivity.this);
            break;
        case(Dialogs.DIALOG_MANUAL_ENTRY):
            mAlertDialog = Dialogs.showManualEntryDialog(MainActivity.this);
            break;
        case(Dialogs.DIALOG_SELECT_LIBRARY):
            mAlertDialog = Dialogs.showSelectLibraryDialog(MainActivity.this,
                    mGroups, mSelectedGroup);
            break;
        case(Dialogs.DIALOG_SEARCH_ENGINE):
            mAlertDialog = Dialogs.showSearchEngineDialog(MainActivity.this,
                    mISBNService);
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
        state.putIntArray(RC_CHECKED, mItemAdapter.getChecked());
        state.putParcelable(RC_ACCESS, mAccountAccess);
        state.putInt(RC_UPLOADING, mUploadState);
        state.putSparseParcelableArray(RC_GROUPS, mGroups);
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
        }).start();
    }

    public void lookupAuthorizations() {
        final MainActivity parent = this;
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
                    postToUIThread(new Runnable(){
                        public void run() {
                            mAlertDialog = Dialogs.showCheckingCredentialsDialog(parent);
                        }
                    });
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
        if(mAccountAccess.getGroupCount() == 0
                && mAccountAccess.canWriteLibrary()) {
            mGroups = new SparseArray<PString>();
            mGroups.put(Group.GROUP_LIBRARY, new PString(getString(R.string.my_library)));
            return;
        }
        // Check that we have all the group titles
        new Thread(new Runnable(){
            public void run(){
                final SparseArray<PString> newGroupList = new SparseArray<PString>();
                Set<Integer> groups = mAccountAccess.getGroupIds();
                if(mAccountAccess.canWriteLibrary()){
                    newGroupList.put(Group.GROUP_LIBRARY, new PString(getString(R.string.my_library)));
                }
                String[] selection = new String[] {TextUtils.join(",", groups)};
                Cursor c = getContentResolver()
                            .query(Database.GROUP_URI,
                                    new String[]{Group._ID, Group.COL_TITLE}, 
                                    Group._ID+" IN (?)",
                                    selection,
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
                // Update the group list
                mUIThreadHandler.post(new Runnable(){
                    public void run(){
                        mGroups = newGroupList;
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

    public void bibFetchSuccess(final String isbn, final JSONObject info){
        if(!mPendingAdapter.hasItem(isbn)){
            // Item was deleted while pending
            return;
        }
        BibItem item = new BibItem(BibItem.TYPE_BOOK, info, mAccount.getDbId());
        mPendingAdapter.remove(isbn);
        if(mPendingAdapter.getCount() == 0) {
            mPendingList.setVisibility(View.GONE);
        }
        mItemAdapter.addItem(item);
        redrawPendingList();
    }

    public void bibFetchFailure(String isbn, Integer status){
        mPendingAdapter.setStatus(isbn, status);
    }

    public void uploadSuccess(int[] dbrows){
        mItemAdapter.setChecked(new int[0]);
        mItemAdapter.deleteItemsWithRowIds(dbrows);
        Toast.makeText(MainActivity.this,
                       "Items added successfully",
                       Toast.LENGTH_LONG).show();
        showUploadButton();
    }

    public void uploadFailure(Integer reason) {
        mUploadState = UPLOAD_STATE_FAILURE;
        ProgressBar prog = (ProgressBar) findViewById(R.id.upload_progress_bar);
        prog.setVisibility(View.GONE);

        TextView error = (TextView) findViewById(R.id.upload_error);
        error.setVisibility(View.VISIBLE);

        TextView output = (TextView) findViewById(R.id.upload_output);
        output.setText(getText(reason));
    }

    public void resetUploadStatus() {
        ProgressBar prog = (ProgressBar) findViewById(R.id.upload_progress_bar);
        prog.setVisibility(View.VISIBLE);

        TextView error = (TextView) findViewById(R.id.upload_error);
        error.setVisibility(View.GONE);

        TextView output = (TextView) findViewById(R.id.upload_output);
        output.setText(getText(ZoteroAPIClient.UPLOADING));
    }

    public void showUploadInProgress() {
        mUploadState = UPLOAD_STATE_PENDING;
        TextView output = (TextView) findViewById(R.id.upload_output);
        output.setText(getText(ZoteroAPIClient.UPLOADING));

        SafeViewFlipper vf = (SafeViewFlipper)findViewById(R.id.upload_flipper);
        if(vf.getCurrentView().getId() == R.id.upload){
            vf.setInAnimation(mAnimations[0]); // Slide in previous
            vf.setOutAnimation(mAnimations[1]); // slide out previous
            vf.showNext();
        }
    }

    public void showUploadButton() {
        mUploadState = UPLOAD_STATE_WAIT;
        resetUploadStatus();
        SafeViewFlipper vf = (SafeViewFlipper)findViewById(R.id.upload_flipper);
        if(vf.getCurrentView().getId() == R.id.upload_progress){
            vf.setInAnimation(mAnimations[2]); // slide in next
            vf.setOutAnimation(mAnimations[3]); // slide out next
            vf.showPrevious();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu_main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu){
        super.onPrepareOptionsMenu(menu);
        MenuItem check_all = (MenuItem) menu.findItem(R.id.ctx_check_all);

        int numChecked = mItemAdapter.getChecked().length;
        int groupCount = mItemAdapter.getGroupCount();

        String title = getString(R.string.check_all);
        // Set to "Uncheck All" if all are checked, or we're
        // waiting for db to return bibitems. In the latter case we
        // just assume all items are checked.
        if((groupCount > 0 && numChecked == groupCount)
                || (groupCount == 0 && numChecked > 0)) {
            title = getString(R.string.uncheck_all);
        }

        check_all.setTitle(title);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
        case R.id.ctx_manual:
            mAlertDialog = Dialogs.showManualEntryDialog(MainActivity.this);
            break;
        case R.id.ctx_check_all:
            if(item.getTitle().equals(getString(R.string.uncheck_all))){
                // Uncheck All
                mItemAdapter.setChecked(new int[0]);
            }else{
                // Check All
                int[] all = new int[mItemAdapter.getGroupCount()];
                for(int i=0; i<all.length; i++)
                    all[i] = i;
                mItemAdapter.setChecked(all);
            }
            mItemAdapter.notifyDataSetChanged();
            break;
        case R.id.ctx_library:
            mAlertDialog = Dialogs.showSelectLibraryDialog(MainActivity.this,
                    mGroups, mSelectedGroup);
            break;
        case R.id.ctx_engine:
            mAlertDialog = Dialogs.showSearchEngineDialog(MainActivity.this,
                    mISBNService);
            break;
        case R.id.ctx_logout:
            logout();
            break;
        default:
            return super.onOptionsItemSelected(item);
        }
        return true;
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

            int index = (int) einfo.id;
            BibItem toEdit = (BibItem) mItemAdapter.getGroup(index);
            Intent intent = new Intent(MainActivity.this, EditItemActivity.class);
            intent.putExtra(EditItemActivity.INTENT_EXTRA_BIBITEM, toEdit);
            intent.putExtra(EditItemActivity.INTENT_EXTRA_INDEX, index);
            startActivityForResult(intent, RESULT_EDIT);
            break;
        case R.id.ctx_delete:
            ExpandableListContextMenuInfo dinfo = (ExpandableListContextMenuInfo) item.getMenuInfo();
            mItemAdapter.deleteItem((BibItem) mItemAdapter.getGroup((int) dinfo.id));
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
                lookupISBN(ident);
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
                    String content = intent.getStringExtra("SCAN_RESULT"); // The scanned ISBN
                    String format = intent.getStringExtra("SCAN_RESULT_FORMAT"); // "EAN 13"
                    addToPendingList(content);
                    handleBarcode(content, format);
                }
                break;
            case RESULT_EDIT:
                if (resultCode == RESULT_OK) {
                    Bundle extras = intent.getExtras();
                    int index = extras
                        .getInt(EditItemActivity.INTENT_EXTRA_INDEX);
                    BibItem replacement = extras
                        .getParcelable(EditItemActivity.INTENT_EXTRA_BIBITEM);
                    mItemAdapter.replaceItem(index, replacement);
                }
                break;
            default:
                Log.d(CLASS_TAG, "Scan error");
        }
    }

    protected void addToPendingList(String content){
        if(mPendingAdapter.hasItem(content)){
            Toast.makeText(
                    MainActivity.this,
                    "This item is already in your list.",
                    Toast.LENGTH_LONG).show();
        }else{
            mPendingAdapter.add(content);
            mPendingList.setVisibility(View.VISIBLE);
            redrawPendingList();
        }
    }

    protected void handleBarcode(String content, String format){
        switch(Util.parseBarcode(content, format)) {
        case(Util.SCAN_PARSE_ISBN):
            lookupISBN(content);
            break;
        case(Util.SCAN_PARSE_ISSN):
            mPendingAdapter.setStatus(content, PendingListAdapter.STATUS_UNKNOWN_TYPE);
            break;
        default:
            mPendingAdapter.setStatus(content, PendingListAdapter.STATUS_UNKNOWN_TYPE);
            break;
        }
        redrawPendingList();
    }

    protected void lookupISBN(String isbn){
        switch(mISBNService){
        case SERVICE_GOOGLE:
            mGoogleBooksAPI.isbnLookup(isbn);
            break;
        case SERVICE_WORLDCAT:
            mWorldCatAPI.isbnLookup(isbn);
            break;
        }
    }

    protected void setISBNService(int sid) {
        mISBNService = sid;
    }

    protected void setSelectedGroup(int gid) {
        mSelectedGroup = gid;
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
                Dialogs.getZxingScanner(MainActivity.this);
            }
        }
    };

    private final Button.OnClickListener uploadSelected = new Button.OnClickListener() {
        public void onClick(View v) {
            int[] checked = mItemAdapter.getChecked();
            if(checked.length == 0) {
                Toast.makeText(MainActivity.this, "No items selected",
                        Toast.LENGTH_LONG).show();
                return;
            }

            int dest = (mSelectedGroup == Group.GROUP_LIBRARY) ?
                    Integer.parseInt(mAccount.getUid()) : mSelectedGroup;

            int[] rows = new int[Math.min(checked.length, ZoteroAPIClient.MAX_UPLOAD_CNT)];
            JSONObject items = new JSONObject();
            try {
                items.put("items", new JSONArray());
                int row_indx = 0;
                for(int b=0; b<checked.length; b++){
                    BibItem bib = (BibItem)mItemAdapter.getGroup(checked[b]);
                    rows[row_indx] = bib.getId();
                    items.accumulate("items", bib.getSelectedInfo());
                    row_indx++;
                    if(row_indx == ZoteroAPIClient.MAX_UPLOAD_CNT) {
                        // Upload the batch
                        mZAPI.addItems(items, rows, dest);
                        showUploadInProgress();
                        row_indx = 0;
                        items = new JSONObject();
                    }
                }
                // As long as the batch size isn't perfectly divisible by 50, we
                // need to upload the remaining items
                if (checked.length % 50 != 0) {
                    mZAPI.addItems(items, rows, dest);
                    showUploadInProgress();
                }
            } catch (JSONException e) {
                // TODO Prompt about failure
                e.printStackTrace();
                // Clear the selection
                mItemAdapter.setChecked(new int[0]);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
    };

    private final Button.OnClickListener dismissUploadStatus = new Button.OnClickListener() {
        public void onClick(View v) {
            if(mUploadState == UPLOAD_STATE_FAILURE){
                showUploadButton();
            }
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
