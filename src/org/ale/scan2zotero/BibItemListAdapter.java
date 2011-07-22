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
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

public class BibItemListAdapter extends BaseExpandableListAdapter {

    private static final int EVEN_ROW_COLOR = 0xFFFFFFFF;
    private static final int ODD_ROW_COLOR = 0xFFEEEEEE;

    private static final int FOUND_SAVED_ITEMS = 0;
    private static final int INSERTED_ITEM = 1;
    private static final int REMOVED_ITEM = 2;
    
    private ArrayList<BibItem> mItems;
    private ArrayList<BibDetailJSONAdapter> mAdapters;

    private final Context mContext;
    private final LayoutInflater mInflater;

    public BibItemListAdapter(Context context) {
        mContext = context;
        mInflater = 
            (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mItems = new ArrayList<BibItem>();
        mAdapters = new ArrayList<BibDetailJSONAdapter>();
    }

    public void fillFromDatabase(){
        new Thread(new Runnable() {
            public void run(){
                Cursor c = mContext.getContentResolver()
                                   .query(S2ZDatabase.BIBINFO_URI, null, null, null, BibItem.COL_DATE + " DESC");
                if(c.getCount() > 0) {
                    ArrayList<BibItem> toadd = new ArrayList<BibItem>(c.getCount());
                    c.moveToFirst();
                    while(!c.isAfterLast()){
                        toadd.add(BibItem.fromCursor(c));
                        c.moveToNext();
                    }
                    mDatabaseResponseHandler.sendMessage(
                            Message.obtain(mDatabaseResponseHandler, 
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
                mDatabaseResponseHandler.sendMessage(
                        Message.obtain(mDatabaseResponseHandler, 
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
                mDatabaseResponseHandler.sendMessage(
                            Message.obtain(mDatabaseResponseHandler,
                                    BibItemListAdapter.REMOVED_ITEM, item));
            }
        }).start();
    }

    private final Handler mDatabaseResponseHandler = new Handler(){
        @SuppressWarnings("unchecked")
        public void handleMessage(Message msg){
            switch(msg.what){
            case BibItemListAdapter.FOUND_SAVED_ITEMS:
                mItems.addAll(((ArrayList<BibItem>)msg.obj));
                for(BibItem b :(ArrayList<BibItem>)msg.obj){
                    mAdapters.add(new BibDetailJSONAdapter(mContext, b.getSelectedInfo()));
                }
                notifyDataSetChanged();
                break;
            case BibItemListAdapter.INSERTED_ITEM:
                mItems.add(0, (BibItem)msg.obj);
                mAdapters.add(0, new BibDetailJSONAdapter(
                                      mContext, ((BibItem)msg.obj).getSelectedInfo()));
                notifyDataSetChanged();
                break;
            case BibItemListAdapter.REMOVED_ITEM:
                int indx = mItems.indexOf((BibItem)msg.obj);
                mItems.remove(indx);
                mAdapters.remove(indx);
                notifyDataSetChanged();
                break;
            }
        }
    };

    @Override
    public Object getChild(int group, int child) {
        return mItems.get(group);
    }

    @Override
    public long getChildId(int group, int child) {
        return 100000 + group;
    }

    @Override
    public View getChildView(int group, int child, boolean last,
            View convert, ViewGroup parent) {
        if(convert == null){
            convert = mInflater.inflate(R.layout.expandable_bib_child, parent, false);
        }
        convert.setBackgroundColor(group % 2 == 0 ? EVEN_ROW_COLOR : ODD_ROW_COLOR);
        BibDetailJSONAdapter adapter = mAdapters.get(group);
        adapter.fillLinearLayout((LinearLayout) convert);
        return convert;
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
    public View getGroupView(int group, boolean expanded, View convert, ViewGroup parent) {
        BibItem item = mItems.get(group);
        if(convert == null){      
            convert = mInflater.inflate(R.layout.expandable_bib_item, parent, false);
        }
        convert.findViewById(R.id.bib_row)
            .setBackgroundColor(group % 2 == 0 ? EVEN_ROW_COLOR : ODD_ROW_COLOR);
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

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public boolean isChildSelectable(int group, int child) {
        return true;
    }
}
