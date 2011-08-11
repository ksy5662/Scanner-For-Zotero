package org.ale.scan2zotero;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.ViewFlipper;

public class PendingListAdapter extends ArrayAdapter<String> {

    public static final Integer STATUS_UNKNOWN_TYPE = new Integer(R.string.pending_status_unknown);
    public static final Integer STATUS_LOADING = new Integer(R.string.pending_status_loading);
    public static final Integer STATUS_NO_NETWORK = new Integer(R.string.pending_status_nonet);
    public static final Integer STATUS_BAD_REQUEST = new Integer(R.string.pending_status_bad_request);
    public static final Integer STATUS_SERVER_ERROR = new Integer(R.string.pending_status_server_error);
    public static final Integer STATUS_QUOTA_EXCEEDED = new Integer(R.string.pending_status_quota_exceeded);
    public static final Integer STATUS_FAILED = new Integer(R.string.pending_status_failed);

    private ArrayList<Integer> mPendingStatus;
    private Context mContext;
    public int _hack_childSize;

    public PendingListAdapter(Context context, int resource,
            int textViewResourceId, List<String> objects,
            ArrayList<Integer> status) {
        super(context, resource, textViewResourceId, objects);
        mContext = context;
        mPendingStatus = status;
        _hack_childSize = 72;
    }

    public boolean hasItem(String item){
        return (getPosition(item) != -1);
    }

    public void setStatus(String item, Integer status){
        int idx = getPosition(item);
        if(idx < 0) return;
        mPendingStatus.set(idx, status);
        notifyDataSetChanged();
    }

    public Integer getStatus(String item){
        return mPendingStatus.get(getPosition(item));
    }

    @Override
    public void add(String item){
        mPendingStatus.add(STATUS_LOADING);
        super.add(item);
    }

    @Override
    public void clear(){
        mPendingStatus.clear();
        super.clear();
    }

    @Override
    public void remove(String item){
        int idx = getPosition(item);
        mPendingStatus.remove(idx);
        super.remove(item);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        convertView = super.getView(position, convertView, parent);

        if(convertView != null && position < mPendingStatus.size()){
            TextView tv = (TextView) convertView.findViewById(R.id.pending_item_status);
            int stat = mPendingStatus.get(position).intValue();
            String strstat = mContext.getString(stat);
            tv.setText(strstat);
            ViewFlipper vf = (ViewFlipper)convertView.findViewById(R.id.pending_item_img);
            if(stat != R.string.pending_status_loading){
                vf.setDisplayedChild(1);
            }else{
                vf.setDisplayedChild(0);
            }
            int cvh = convertView.getMeasuredHeight();
            if(cvh != 0){
                _hack_childSize = cvh;
            }
        }
        return convertView;
    }
}
