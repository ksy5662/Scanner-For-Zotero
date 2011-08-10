package org.ale.scan2zotero;

import java.util.ArrayList;

import org.ale.scan2zotero.data.BibItemDBHandler;
import org.ale.scan2zotero.data.BibItem;
import org.ale.scan2zotero.data.ItemField;
import org.ale.scan2zotero.data.Database;

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

    private ArrayList<BibItem> mItems;
    private ArrayList<BibDetailJSONAdapter> mAdapters;
    private SparseBooleanArray mChecked;

    private BibItemDBHandler mHandler;

    private final Context mContext;
    private final Resources mResources;
    private final LayoutInflater mInflater;
    
    private CheckBox.OnClickListener CHECK_LISTENER = new CheckBox.OnClickListener(){
        public void onClick(View cb) {
            mChecked.put(((Integer)cb.getTag()).intValue(), ((CheckBox)cb).isChecked());
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

    public void readyToGo(){
        mHandler.registerAdapter(BibItemListAdapter.this);
    }

    public void prepForDestruction(){
        mHandler.unregisterAdapter();
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
                        toadd.add(BibItem.fromCursor(c));
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

    public void deleteItem(int indx){
        final BibItem item = mItems.get(indx);
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

    public void finishAddItem(BibItem item){
        shiftUpSelections(0);
        mItems.add(0, item);
        mAdapters.add(0, new BibDetailJSONAdapter(
                              mContext, item.getSelectedInfo()));
        ((MainActivity)mContext).showOrHideUploadButton();
        notifyDataSetChanged();
    }

    public void finishAddItems(ArrayList<BibItem> items){
        mItems.addAll(items);
        for(BibItem b : items){
            mAdapters.add(new BibDetailJSONAdapter(mContext, b.getSelectedInfo()));
        }
        ((MainActivity)mContext).showOrHideUploadButton();
        notifyDataSetChanged();
    }

    public void finishDeleteItem(BibItem item){
        int indx = mItems.indexOf(item);
        shiftDownSelections(indx);
        mItems.remove(indx);
        mAdapters.remove(indx);
        ((MainActivity)mContext).showOrHideUploadButton();
        notifyDataSetChanged();
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

        GroupViewComponents vtag;
        if(convert == null){ // Create a new view
            convert = mInflater.inflate(R.layout.expandable_bib_item, parent, false);
            // Avoid a bunch of calls to findViewById by tagging this view w/
            // references to its children
            vtag = new GroupViewComponents();
            vtag.tv_checkbox = (CheckBox) convert.findViewById(R.id.bib_row_checkbox);
            vtag.tv_author_lbl = (TextView) convert.findViewById(R.id.bib_author_lbl);
            vtag.tv_author = (TextView) convert.findViewById(R.id.bib_author);
            vtag.tv_title = (TextView) convert.findViewById(R.id.bib_title);
            vtag.tv_checkbox.setOnClickListener(CHECK_LISTENER);
            convert.setTag(vtag);
        }else{
            vtag = (GroupViewComponents) convert.getTag();
        }

        convert.setBackgroundDrawable(getRowDrawable(group));

        // The checkbox's click listener checks this tag
        vtag.tv_checkbox.setTag(new Integer(group));
        vtag.tv_checkbox.setChecked(mChecked.get(group, false));

        BibItem item = mItems.get(group);
        if(!item.hasCachedValues())
            item.cacheForViews();

        vtag.tv_author_lbl.setText(mResources.getQuantityString(
                                R.plurals.author, item.getCachedNumAuthors()));
        Util.fillBibTextField(vtag.tv_title, item.getCachedTitleString());
        Util.fillBibTextField(vtag.tv_author, item.getCachedAuthorString());
        return convert;
    }

    private Drawable getRowDrawable(int group){
        if((mItems.size()-group) % 2 == 0)
            return mResources.getDrawable(R.drawable.group_selector_even);
        return mResources.getDrawable(R.drawable.group_selector_odd);
    }

    @Override
    public Object getChild(int group, int child) {
        return mItems.get(group);
    }

    @Override
    public long getChildId(int group, int child) {
        return 100000 + group;
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
            int i = mChecked.indexOfKey(key);
            if(i < 0) continue; // Not storing that key
            boolean value = mChecked.get(key);
            mChecked.delete(key);
            mChecked.put(key-1, value);
        }
    }

    public void shiftUpSelections(int indx){
        // Update selections to reflect insertion of item at indx
        for(int key=mItems.size()-1; key >= indx; key--){
            int i = mChecked.indexOfKey(key);
            if(i < 0) continue; // Not storing that key
            boolean value = mChecked.get(key);
            mChecked.delete(key);
            mChecked.put(key+1, value);
        }
    }

    public void setChecked(boolean[] checks){
        mChecked.clear();
        for(int i=0; i<checks.length; i++)
            mChecked.put(i, checks[i]);
    }

    public boolean[] getChecked(){
        boolean[] result = new boolean[mItems.size()];
        for(int i=0; i<result.length; i++)
            result[i] = mChecked.get(i, false);
        return result;
    }

    /* View tag */
    protected final class GroupViewComponents {
        public CheckBox tv_checkbox;
        public TextView tv_author_lbl;
        public TextView tv_author;
        public TextView tv_title;
    }
}
