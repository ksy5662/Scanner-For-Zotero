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

import java.util.ArrayList;

import org.ale.scanner.zotero.data.BibItem;
import org.ale.scanner.zotero.data.BibItemDBHandler;
import org.ale.scanner.zotero.data.Database;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Message;
import android.text.TextUtils;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

public class BibItemListAdapter extends BaseExpandableListAdapter {

    private static final int ACTION_ID = BibItemDBHandler.BIBITEM_ACTION_ID;
    public static final int FOUND_SAVED_ITEMS = ACTION_ID + 0;
    public static final int INSERTED_ITEM = ACTION_ID + 1;
    public static final int REMOVED_ITEM = ACTION_ID + 2;
    public static final int REPLACED_ITEM = ACTION_ID + 3;

    private ArrayList<BibItem> mItems;
    private ArrayList<BibDetailJSONAdapter> mAdapters;
    private SparseBooleanArray mChecked;

    private BibItemDBHandler mHandler;

    private final Context mContext;
    private final Resources mResources;
    private final LayoutInflater mInflater;

    private CheckBox.OnClickListener CHECK_LISTENER = new CheckBox.OnClickListener(){
        public void onClick(View cb) {
            boolean checked = ((CheckBox)cb).isChecked();
            int key = ((Integer)cb.getTag()).intValue();
            if(checked){
                mChecked.put(key, true);
            }else{
                mChecked.delete(key);
            }
        }
    };

    public BibItemListAdapter(Context context) {
        mContext = context;
        mResources = mContext.getResources();
        mInflater = 
            (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mItems = new ArrayList<BibItem>();
        mAdapters = new ArrayList<BibDetailJSONAdapter>();
        mChecked = new SparseBooleanArray();

        mHandler = BibItemDBHandler.getInstance();
    }

    public void fillFromDatabase(final int acctId){
        new Thread(new Runnable() {
            public void run(){
                Cursor c = mContext.getContentResolver()
                                   .query(Database.BIBINFO_URI,
                                           null, 
                                           BibItem.COL_ACCT+"=?",
                                           new String[] {String.valueOf(acctId)},
                                           BibItem.COL_DATE + " DESC");
                if(c.getCount() > 0) {
                    ArrayList<BibItem> toadd = new ArrayList<BibItem>(c.getCount());
                    c.moveToFirst();
                    while(!c.isAfterLast()){
                        BibItem fc = BibItem.fromCursor(c);
                        if(fc != null)
                            toadd.add(fc);
                        c.moveToNext();
                    }
                    mHandler.sendMessage(Message.obtain(mHandler, 
                                           BibItemListAdapter.FOUND_SAVED_ITEMS, toadd));
                }
                c.close();
            }
        }).start();
    }

    public void addItem(final BibItem item){
        new Thread(new Runnable() {
            public void run(){
                item.writeToDB(mContext.getContentResolver());
                mHandler.sendMessage(Message.obtain(mHandler, 
                                       BibItemListAdapter.INSERTED_ITEM, item));
            }
        }).start();
    }

    public void deleteItem(final BibItem item){
        if(item.getId() == BibItem.NO_ID) // Item isn't in database
            return;
        new Thread(new Runnable() {
            public void run(){
                mContext.getContentResolver().delete(
                        Database.BIBINFO_URI, BibItem._ID+"="+item.getId(), null); 
                mHandler.sendMessage(Message.obtain(mHandler,
                                    BibItemListAdapter.REMOVED_ITEM, item));
            }
        }).start();
    }

    public void replaceItem(final BibItem replacement){
        final ContentResolver cr = mContext.getContentResolver();
        new Thread(new Runnable() {
            public void run(){
                replacement.writeToDB(cr);
                mHandler.sendMessage(Message.obtain(mHandler,
                       BibItemListAdapter.REPLACED_ITEM, replacement));
            }
        }).start();
        //finishReplaceItem(replacement);
    }

    public void deleteItemsWithRowIds(int[] dbid){
        ArrayList<BibItem> toDelete = new ArrayList<BibItem>(dbid.length);
        for(int i=0;i<mItems.size(); i++){
            for(int j=0; j<dbid.length; j++){
                if(mItems.get(i).getId() == dbid[j]){
                    toDelete.add(mItems.get(i));
                }
            }
        }
        for(BibItem item : toDelete){
            deleteItem(item);
        }
    }

    public void clear(){
        mItems.clear();
        mAdapters.clear();
        mChecked.clear();
        notifyDataSetChanged();
    }

    public void finishAddItem(BibItem item){
        // If the initial write of an item occurs during an orientation change,
        // it's possible that the write succeeds, and that the new activity loads
        // its bibitem list from the DB before finishAddItem is called. In this
        // case, while the database only contains one copy of the bibitem, the
        // list on screen will momentarily (until the next pause) display two.
        // The bug is avoided here by comparing the ID of the item to add and
        // that of the top item on the list, and ignoring the add if they match.
        if((mItems.size() > 0) && (mItems.get(0).getId() == item.getId()))
            return;

        shiftUpSelections(0);
        mItems.add(0, item);
        mAdapters.add(0, new BibDetailJSONAdapter(
                              mContext, item.getSelectedInfo()));
        notifyDataSetChanged();
    }

    public void finishAddItems(ArrayList<BibItem> items){
        mItems.addAll(items);
        for(BibItem b : items){
            mAdapters.add(new BibDetailJSONAdapter(mContext, b.getSelectedInfo()));
        }
        notifyDataSetChanged();
    }

    public void finishDeleteItem(BibItem item){
        int indx = mItems.indexOf(item);
        if(indx < 0)
            return;

        shiftDownSelections(indx);
        mItems.remove(indx);
        mAdapters.remove(indx);
        notifyDataSetChanged();
    }

    public void finishReplaceItem(BibItem item){
        for(int indx=0; indx<mItems.size(); indx++){
            if(mItems.get(indx).getId() == item.getId()){
                mItems.set(indx, item);
                mAdapters.set(indx, 
                     new BibDetailJSONAdapter(mContext, item.getSelectedInfo()));
                notifyDataSetChanged();
                break;
            }
        }
    }

    @Override
    public View getChildView(
            int group, int child, boolean last, View convert, ViewGroup parent){
        if(convert == null){
            convert = mInflater.inflate(R.layout.expandable_bib_child, parent, false);
        }
        convert.setBackgroundDrawable(getRowDrawable(group));
        BibDetailJSONAdapter adapter = mAdapters.get(group);
        adapter.fillLinearLayout((LinearLayout) convert);
        return convert;
    }

    @Override
    public View getGroupView(
            final int group, boolean expanded, View convert, ViewGroup parent) {

        ViewHolder vtag;
        if(convert == null){ // Create a new view
            convert = mInflater.inflate(R.layout.expandable_bib_item, parent, false);
            // Avoid a bunch of calls to findViewById by tagging this view w/
            // references to its children
            vtag = new ViewHolder();
            vtag.tv_checkbox = (CheckBox) convert.findViewById(R.id.bib_row_checkbox);
            vtag.tv_author_lbl = (TextView) convert.findViewById(R.id.bib_author_lbl);
            vtag.tv_author = (TextView) convert.findViewById(R.id.bib_author);
            vtag.tv_title = (TextView) convert.findViewById(R.id.bib_title);
            vtag.tv_checkbox.setOnClickListener(CHECK_LISTENER);
            convert.setTag(vtag);
        }else{
            vtag = (ViewHolder) convert.getTag();
        }

        convert.setBackgroundDrawable(getRowDrawable(group));

        // The checkbox's click listener checks this tag
        vtag.tv_checkbox.setTag(new Integer(group));
        vtag.tv_checkbox.setChecked(mChecked.get(group, false));

        BibItem item = mItems.get(group);
        if(!item.hasCachedValues())
            item.cacheForViews();

        vtag.tv_author_lbl.setText(item.getCachedCreatorLabel() + ":");
        Util.fillBibTextField(vtag.tv_title, item.getCachedTitleString());
        Util.fillBibTextField(vtag.tv_author, item.getCachedCreatorValue());
        return convert;
    }

    private Drawable getRowDrawable(int group){
        if((group % 2) == 0)
            return mResources.getDrawable(R.drawable.group_selector_even);
        return mResources.getDrawable(R.drawable.group_selector_odd);
    }

    @Override
    public Object getChild(int group, int child) {
        return mItems.get(group);
    }

    @Override
    public long getChildId(int group, int child) {
        return group;
    }

    @Override
    public int getChildrenCount(int group) {
        return 1;
    }

    @Override
    public Object getGroup(int group) {
        return mItems.get(group);
    }

    @Override
    public int getGroupCount() {
        return mItems.size();
    }

    @Override
    public long getGroupId(int group) {
        return group;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public boolean isChildSelectable(int group, int child) {
        return true;
    }

    public String getTitleOfGroup(int group){
        String title = mItems.get(group).getCachedTitleString();
        if(TextUtils.isEmpty(title))
            return mContext.getString(R.string.unknown);
        return title;
    }

    /* Selection management */
    public void shiftDownSelections(int indx){
        // Update selections to reflect removal of an item at indx
        mChecked.delete(indx);
        for(int key=indx+1; key<mItems.size(); key++){
            if(mChecked.get(key)){ // All entries in mChecked are set to true
                mChecked.delete(key);
                mChecked.put(key-1, true);
            }
        }
    }

    public void shiftUpSelections(int indx){
        // Update selections to reflect insertion of item at indx
        for(int key=mItems.size()-1; key >= indx; key--){
            if(mChecked.get(key)){ // All entries in mChecked are set to true
                mChecked.delete(key);
                mChecked.put(key+1, true);
            }
        }
    }

    public void setChecked(int[] checks){
        mChecked.clear();
        for(int i=0; i<checks.length; i++)
            mChecked.put(checks[i], true);
    }

    public int[] getChecked() {
        int[] result = new int[mChecked.size()];
        for(int i=0; i<mChecked.size(); i++){
            result[i] = mChecked.keyAt(i);
        }
        return result;
    }

    /* View tag */
    protected final class ViewHolder {
        public CheckBox tv_checkbox;
        public TextView tv_author_lbl;
        public TextView tv_author;
        public TextView tv_title;
    }
}
