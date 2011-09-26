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

package org.ale.scanner.zotero.data;

import java.util.ArrayList;

import org.ale.scanner.zotero.BibItemListAdapter;

import android.os.Handler;
import android.os.Message;

/**
 * BibItemDBHandler is for those cases where a database operation is made, the
 * parent activity which initiated it is paused (orientation change, etc),
 * and the parent activity is resumed before the db operation is finished.
 *
 * This happens a lot.
 */
public class BibItemDBHandler extends Handler {

    public static final int BIBITEM_ACTION_ID = 1000;

    private static BibItemDBHandler mInstance = null;

    public static BibItemDBHandler getInstance(){
        if(mInstance == null)
            mInstance = new BibItemDBHandler();
        return mInstance;
    }

    private BibItemListAdapter mAdapter = null;

    public void bindAdapter(BibItemListAdapter adapter){
        if(mAdapter != null || adapter == null)
            return;
        mAdapter = adapter;
    }

    public void unbindAdapter(){
        mAdapter = null;
    }

    @SuppressWarnings("unchecked")
    public void handleMessage(Message msg){
        if(mAdapter != null){
            switch(msg.what) {
            case BibItemListAdapter.FOUND_SAVED_ITEMS:
                mAdapter.finishAddItems((ArrayList<BibItem>) msg.obj);
                break;
            case BibItemListAdapter.INSERTED_ITEM:
                mAdapter.finishAddItem((BibItem) msg.obj);
                break;
            case BibItemListAdapter.REMOVED_ITEM:
                mAdapter.finishDeleteItem((BibItem) msg.obj);
                break;
            case BibItemListAdapter.REPLACED_ITEM:
                mAdapter.finishReplaceItem((BibItem) msg.obj);
                break;
            }
        }
    }
}
