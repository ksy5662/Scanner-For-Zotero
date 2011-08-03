package org.ale.scan2zotero;

import org.ale.scan2zotero.data.Account;
import org.ale.scan2zotero.data.S2ZDatabase;

import android.app.ListActivity;
import android.database.Cursor;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.SimpleCursorAdapter;

public class ManageAccountsActivity extends ListActivity {

    private Cursor mCursor;
    
    private SimpleCursorAdapter mAdapter = null;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        registerForContextMenu(getListView());
        
        updateList();
    }
    
    private void updateList(){
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
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.acct_item_context_menu, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = 
            (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        switch (item.getItemId()) {
        case R.id.ctx_delete:
            Cursor c = (Cursor) mAdapter.getItem(info.position);
            int idx = c.getInt(S2ZDatabase.ACCOUNT_ID_INDEX);
            getContentResolver().delete(
                    S2ZDatabase.ACCOUNT_URI, Account._ID+"="+idx, null);
            updateList();
            return true;
        default:
            return super.onContextItemSelected(item);
        }
    }
}
