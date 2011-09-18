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

import org.ale.scanner.zotero.data.Account;
import org.ale.scanner.zotero.data.Database;

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
import android.widget.Toast;

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
        String[] projection = new String[] { Account._ID,
                Account.COL_ALIAS, Account.COL_UID, Account.COL_KEY };
        mCursor = getContentResolver().query(Database.ACCOUNT_URI, projection,
                null, null, Account._ID + " ASC");
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
        String title = c.getString(Database.ACCOUNT_ALIAS_INDEX);
        menu.setHeaderTitle(title);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = 
            (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        Cursor c = (Cursor) mAdapter.getItem(info.position);
        int row = c.getInt(Database.ACCOUNT_ID_INDEX);
        switch (item.getItemId()) {
        case R.id.ctx_rename:
            String orig = c.getString(Database.ACCOUNT_ALIAS_INDEX);
            mAlertDialog = Dialogs.showRenameKeyDialog(
                                        ManageAccountsActivity.this, orig, row);
            break;
        case R.id.ctx_delete:
            String uid = c.getString(Database.ACCOUNT_UID_INDEX);
            Account.purgeAccount(getContentResolver(), row);
            deleteFile(uid); // Delete the user's shared preferences file

            updateList();
            if(mAdapter.getCount() == 0){
                Toast.makeText(ManageAccountsActivity.this, "No more accounts", Toast.LENGTH_LONG).show();
                finish();
            }
            break;
        }
        return true;
    }
}
