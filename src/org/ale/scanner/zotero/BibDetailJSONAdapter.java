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

import org.ale.scanner.zotero.data.ItemField;

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
    private ArrayList<String> mLabels;
    private int mNumFilledFields;

    private final LayoutInflater mInflater;

    public BibDetailJSONAdapter(Context context, JSONObject json){
        mInflater =
            (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        setJSON(json);
    }

    private void setLabelsToDisplay(JSONObject obj){
        mLabels = new ArrayList<String>();
        try {
            JSONArray labels = obj.names();
            if(labels == null)
                return;
            for(int i=0; i < labels.length(); i++){
                String label = (String) labels.get(i);
                String entry = obj.optString(label);
                if(!TextUtils.isEmpty(entry.trim())){
                    mLabels.add(label);
                }
            }
        } catch (JSONException e) {
            // XXX: There's bad data in the database!
        }

        // Don't include creator/title/notes/tags/itemType fields
        mLabels.remove(ItemField.creators);
        mLabels.remove(ItemField.title);
        mLabels.remove(ItemField.notes);
        mLabels.remove(ItemField.tags);
        mLabels.remove(ItemField.itemType);
    }

    public void setJSON(JSONObject replacement){
        mBacker = replacement;
        setLabelsToDisplay(replacement);
        mNumFilledFields = mLabels.size();
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
        String label = mLabels.get(position);
        String value = mBacker.optString(label);
        TextView tv_label = (TextView) convertView.findViewById(R.id.entry_lbl);
        TextView tv_value = (TextView) convertView.findViewById(R.id.entry_content);
        tv_label.setText(ItemField.Localized.get(label) + ":");
        Util.fillBibTextField(tv_value, value);
        return convertView;
    }
}
