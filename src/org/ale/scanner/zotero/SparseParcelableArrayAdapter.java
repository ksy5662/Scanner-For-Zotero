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

import android.content.Context;
import android.os.Parcelable;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class SparseParcelableArrayAdapter<T extends Parcelable> extends BaseAdapter {

    private Context mContext;
    private LayoutInflater mInflater;

    private SparseArray<T> mItems;
    public SparseParcelableArrayAdapter(Context ctx, SparseArray<T> items){
        mContext = ctx;
        mInflater = 
            (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mItems = items;
    }

    public SparseArray<T> getData(){
        return mItems;
    }

    public void replaceData(SparseArray<T> items){
        mItems = items;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return mItems.size();
    }

    @Override
    public T getItem(int position) {
        return mItems.valueAt(position);
    }

    @Override
    public long getItemId(int position) {
        return mItems.keyAt(position);
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        if(convertView == null){
            convertView = mInflater.inflate(android.R.layout.simple_spinner_dropdown_item, parent, false);
        }
        ((TextView)convertView.findViewById(android.R.id.text1)).setText(getItem(position).toString());
        return convertView;
    }
    
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if(convertView == null){
            convertView = new TextView(mContext);
        }
        ((TextView)convertView).setText(getItem(position).toString());
        ((TextView)convertView).setTextAppearance(mContext, android.R.attr.textAppearanceMedium);
        return convertView;
    }
}
