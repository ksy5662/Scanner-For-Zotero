package org.ale.scan2zotero;

import java.util.ArrayList;

import org.ale.scan2zotero.data.BibItem;
import org.ale.scan2zotero.data.ItemField;
import org.ale.scan2zotero.data.S2ZDatabase;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
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

    private final int EVEN_ROW_COLOR = 0xFFF4F9EB;
    private final int ODD_ROW_COLOR = 0xFFF1F6DB;

    private static final int ACTION_ID = PersistentDBHandler.BIBITEM_ACTION_ID;
    public static final int FOUND_SAVED_ITEMS = ACTION_ID + 0;
    public static final int INSERTED_ITEM = ACTION_ID + 1;
    public static final int REMOVED_ITEM = ACTION_ID + 2;

    private ArrayList<BibItem> mItems;
    private ArrayList<BibDetailJSONAdapter> mAdapters;
    private SparseBooleanArray mChecked;

    private PersistentDBHandler mHandler;

    private final Context mContext;
    private final LayoutInflater mInflater;

    public BibItemListAdapter(Context context) {
        mContext = context;
        mInflater = 
            (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mItems = new ArrayList<BibItem>();
        mAdapters = new ArrayList<BibDetailJSONAdapter>();
        mChecked = new SparseBooleanArray();

        mHandler = PersistentDBHandler.getInstance();
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
                                   .query(S2ZDatabase.BIBINFO_URI,
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
                Uri row = mContext.getContentResolver().insert(S2ZDatabase.BIBINFO_URI, item.toContentValues());
                item.setId(Integer.parseInt(row.getLastPathSegment()));
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
                        S2ZDatabase.BIBINFO_URI, BibItem._ID+"="+item.getId(), null); 
                mHandler.sendMessage(Message.obtain(mHandler,
                                    BibItemListAdapter.REMOVED_ITEM, item));
            }
        }).start();
    }

    protected void finishAddItem(BibItem item){
        shiftUpSelections(0);
        mItems.add(0, item);
        mAdapters.add(0, new BibDetailJSONAdapter(
                              mContext, item.getSelectedInfo()));
        ((S2ZMainActivity)mContext).showOrHideUploadButton();
        notifyDataSetChanged();
    }

    protected void finishAddItems(ArrayList<BibItem> items){
        mItems.addAll(items);
        for(BibItem b : items){
            mAdapters.add(new BibDetailJSONAdapter(mContext, b.getSelectedInfo()));
        }
        ((S2ZMainActivity)mContext).showOrHideUploadButton();
        notifyDataSetChanged();
    }

    protected void finishDeleteItem(BibItem item){
        int indx = mItems.indexOf(item);
        shiftDownSelections(indx);
        mItems.remove(indx);
        mAdapters.remove(indx);
        ((S2ZMainActivity)mContext).showOrHideUploadButton();
        notifyDataSetChanged();
    }

    @Override
    public View getChildView(int group, int child, boolean last,
            View convert, ViewGroup parent) {
        if(convert == null){
            convert = mInflater.inflate(R.layout.expandable_bib_child, parent, false);
        }
        convert.setBackgroundColor(getColor(group));
        BibDetailJSONAdapter adapter = mAdapters.get(group);
        adapter.fillLinearLayout((LinearLayout) convert);
        return convert;
    }

    @Override
    public View getGroupView(final int group, boolean expanded, View convert, ViewGroup parent) {
        BibItem item = mItems.get(group);
        if(convert == null){      
            convert = mInflater.inflate(R.layout.expandable_bib_item, parent, false);
        }

        CheckBox cb = (CheckBox) convert.findViewById(R.id.bib_row_checkbox);
        cb.setChecked(mChecked.get(group, false));
        cb.setOnClickListener(new CheckBox.OnClickListener(){
            @Override
            public void onClick(View cb) {
                mChecked.put(group, ((CheckBox)cb).isChecked());
            }
        });
        convert.findViewById(R.id.bib_row).setBackgroundColor(getColor(group));
        TextView tv_author_lbl = (TextView) convert.findViewById(R.id.bib_author_lbl);
        TextView tv_author = (TextView) convert.findViewById(R.id.bib_author);
        TextView tv_title = (TextView) convert.findViewById(R.id.bib_title);

        JSONObject data = item.getSelectedInfo();
        JSONArray creators = data.optJSONArray(ItemField.creators);
        if(creators != null){
            int numAuthors = creators.length();
            if(numAuthors > 1) tv_author_lbl.setText("Authors");
            else tv_author_lbl.setText("Author");
            ArrayList<String> creatorNames = new ArrayList<String>(numAuthors);
            try {
                for(int i=0; i<creators.length(); i++){
                    String name = ((JSONObject)creators.get(i)).optString(ItemField.Creator.name);
                    if(!TextUtils.isEmpty(name))
                        creatorNames.add(name);
                }
                tv_author.setText(TextUtils.join(", ", creatorNames));
            } catch (JSONException e) {
                tv_author.setText("<unknown>");
                tv_author.setTextColor(Color.GRAY);
            }
        }else{
            tv_author.setText("<unknown>");
        }

        tv_title.setText(data.optString(ItemField.title));
        return convert;
    }

    private int getColor(int group){
        return (mItems.size()-group) % 2 == 0 ? EVEN_ROW_COLOR : ODD_ROW_COLOR;
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
        try {
            return mItems.get(group).getSelectedInfo().getString(ItemField.title);
        } catch (Exception e) {
            return "<unknown>";
        }
    }

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
}
