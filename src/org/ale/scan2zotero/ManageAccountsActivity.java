package org.ale.scan2zotero;

import org.ale.scan2zotero.data.Account;
import org.ale.scan2zotero.data.S2ZDatabase;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.database.Cursor;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

public class ManageAccountsActivity extends ListActivity {

    private Cursor mCursor;

    private SimpleCursorAdapter mAdapter = null;

    private AlertDialog mAlertDialog = null;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        registerForContextMenu(getListView());
        
        updateList();
    }

    @Override
    protected void onPause(){
        super.onPause();
        if(mAlertDialog != null){
            mAlertDialog.dismiss();
            mAlertDialog = null;
            Dialogs.displayedDialog = Dialogs.DIALOG_NO_DIALOG;
        }
    }

    protected void updateList(){
        mCursor = getContentResolver().query(S2ZDatabase.ACCOUNT_URI, null, null, null, null);
        startManagingCursor(mCursor);

        if(mAdapter == null){
            mAdapter = new SimpleCursorAdapter(
                    ManageAccountsActivity.this,
                    R.layout.account_row,
                    mCursor,
                    new String[] {Account.COL_ALIAS, Account.COL_UID, Account.COL_KEY},
                    new int[] {R.id.acct_row_alias, R.id.acct_row_id, R.id.acct_row_key});
        }else{
            mAdapter.changeCursor(mCursor);
        }
        setListAdapter(mAdapter);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int pos, long id) {
        v.showContextMenu();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.acct_item_context_menu, menu);
        Cursor c = (Cursor) mAdapter.getItem(
                    ((AdapterView.AdapterContextMenuInfo)menuInfo).position);
        String title = c.getString(S2ZDatabase.ACCOUNT_ALIAS_INDEX);
        menu.setHeaderTitle(title);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = 
            (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        Cursor c = (Cursor) mAdapter.getItem(info.position);
        int row = c.getInt(S2ZDatabase.ACCOUNT_ID_INDEX);
        switch (item.getItemId()) {
        case R.id.ctx_rename:
            String orig = c.getString(S2ZDatabase.ACCOUNT_ALIAS_INDEX);
            mAlertDialog = Dialogs.showRenameKeyDialog(
                                        ManageAccountsActivity.this, orig, row);
            break;
        case R.id.ctx_delete:
            Account.purgeAccount(getContentResolver(), row);
            updateList();
            break;
        }
        return true;
    }
}
