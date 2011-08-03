package org.ale.scan2zotero;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class PendingListAdapter extends ArrayAdapter<String> {

    public static final Integer STATUS_LOADING = new Integer(R.string.pending_status_loading);
    public static final Integer STATUS_FAILED = new Integer(R.string.pending_status_failed);
    public static final Integer STATUS_NO_NETWORK = new Integer(R.string.pending_status_nonet);

    private ArrayList<Integer> mPendingStatus;
    private Context mContext;

    public PendingListAdapter(Context context, int resource,
            int textViewResourceId, List<String> objects,
            ArrayList<Integer> status) {
        super(context, resource, textViewResourceId, objects);
        mContext = context;
        mPendingStatus = status;
    }

    @Override
    public void add(String item){
        super.add(item);
        mPendingStatus.add(STATUS_LOADING);
    }

    @Override
    public void clear(){
        super.clear();
        mPendingStatus.clear();
    }

    @Override
    public void remove(String item){
        int idx = getPosition(item);
        super.remove(item);
        mPendingStatus.remove(idx);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        convertView = super.getView(position, convertView, parent);

        if(convertView != null && position < mPendingStatus.size()){
            ((TextView)convertView.findViewById(R.id.pending_item_status))
                .setText(mContext.getString(mPendingStatus.get(position).intValue()));
        }
        
        return convertView;
    }
}
