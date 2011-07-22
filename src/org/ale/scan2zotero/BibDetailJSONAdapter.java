package org.ale.scan2zotero;

import java.util.ArrayList;

import org.ale.scan2zotero.data.ItemField;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.TextView;

public class BibDetailJSONAdapter extends BaseAdapter implements ListAdapter {

    private JSONObject mBacker;
    private ArrayList<String> mNames;
    private int mNumFilledFields;

    private final LayoutInflater mInflater;

    public BibDetailJSONAdapter(Context context, JSONObject json){
        mInflater =
            (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        setJSON(json);
    }

    private void setNamesToDisplay(JSONObject obj){
        mNames = new ArrayList<String>();
        try {
            JSONArray names = obj.names();
            for(int i=0; i < names.length(); i++){
                String name = (String) names.get(i);
                // Don't include creator or title fields
                if(name.equals(ItemField.creators) || name.equals(ItemField.title))
                    continue;
                String entry = obj.optString(name);
                if(!TextUtils.isEmpty(entry.trim())){
                    mNames.add(name);
                }
            }
        } catch (JSONException e) {
            // XXX: There's bad data in the database!
        }
    }

    public void setJSON(JSONObject replacement){
        mBacker = replacement;
        setNamesToDisplay(replacement);
        mNumFilledFields = mNames.size();
    }

    public void fillLinearLayout(LinearLayout layout){
        int childCount = layout.getChildCount();
        // Layout might not be the view we used last time, so
        // remove any views we won't need
        if(mNumFilledFields < childCount){
            layout.removeViews(mNumFilledFields, layout.getChildCount()-mNumFilledFields);
        }

        // Edit any of the already attached views
        for(int entry=0; entry < mNumFilledFields; entry++){
            if(entry < childCount){
                View v = layout.getChildAt(entry);
                getView(entry, v, layout);
            }else{
                layout.addView(getView(entry, null, layout));
            }
        }
    }

    @Override
    public int getCount() {
        return mNumFilledFields;
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if(convertView == null){
            convertView = mInflater.inflate(R.layout.bib_entry, null);
        }
        String name = mNames.get(position);
        String value = mBacker.optString(name);
        ((TextView)convertView.findViewById(R.id.entry_lbl))
                        .setText(name);
        ((TextView)convertView.findViewById(R.id.entry_content))
                        .setText(value);
        return convertView;
    }

}
