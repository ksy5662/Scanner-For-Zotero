package org.ale.scan2zotero.data;

import java.util.ArrayList;

import org.ale.scan2zotero.BibItemListAdapter;

import android.os.Handler;
import android.os.Message;

public class BibItemDBHandler extends Handler {

    public static final int BIBITEM_ACTION_ID = 1000;

    private static BibItemDBHandler mInstance = null;

    public static BibItemDBHandler getInstance(){
        if(mInstance == null)
            mInstance = new BibItemDBHandler();
        return mInstance;
    }

    private BibItemListAdapter mAdapter = null;

    private ArrayList<BibItem> mToInsert = new ArrayList<BibItem>();

    private ArrayList<BibItem> mToRemove = new ArrayList<BibItem>();

    public void registerAdapter(BibItemListAdapter adapter){
        if(mAdapter != null || adapter == null)
            return;
        mAdapter = adapter;
        for(BibItem b : mToInsert)
            mAdapter.finishAddItem(b);
        for(BibItem b : mToRemove)
            mAdapter.finishDeleteItem(b);
        mToInsert.clear();
        mToRemove.clear();
    }

    public void unregisterAdapter(){
        mAdapter = null;
    }

    @SuppressWarnings("unchecked")
    public void handleMessage(Message msg){
        if(mAdapter == null){
            switch(msg.what) {
            case BibItemListAdapter.FOUND_SAVED_ITEMS:
                mToInsert.addAll((ArrayList<BibItem>) msg.obj);
                break;
            case BibItemListAdapter.INSERTED_ITEM:
                mToInsert.add((BibItem) msg.obj);
                break;
            case BibItemListAdapter.REMOVED_ITEM:
                mToRemove.add((BibItem) msg.obj);
                break;
            }
        }else{
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
            }
        }
    }
}
