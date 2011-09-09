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

import org.ale.scanner.zotero.data.BibItem;
import org.ale.scanner.zotero.data.ItemField;
import org.ale.scanner.zotero.data.ItemType;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class EditItemActivity extends Activity {

    public static final String INTENT_EXTRA_BIBITEM = "BIBITEM";
    public static final String INTENT_EXTRA_INDEX = "INDEX";

    public static final String RC_ORIG_BIBITEM = "OB";
    public static final String RC_WORKING_BIBITEM = "WB";

    private BibItem mTargetItem;
    private BibItem mWorkingItem;
    private int mIndex;

    @Override
    public void onCreate(Bundle state){
        super.onCreate(state);
        setContentView(R.layout.edit);

        Bundle extras = getIntent().getExtras();
        mIndex = extras.getInt(INTENT_EXTRA_INDEX);
        if(state != null){
            mTargetItem = state.getParcelable(RC_ORIG_BIBITEM);
            mWorkingItem = state.getParcelable(RC_WORKING_BIBITEM);
        }else{
            mTargetItem = (BibItem) extras.getParcelable(INTENT_EXTRA_BIBITEM);
            mWorkingItem = mTargetItem.copy();
        }
        
        findViewById(R.id.save).setOnClickListener(saveListener);
    }

    @Override
    public void onResume() {
        super.onResume();
        fillForm();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        try {
            extractValues();
        } catch (JSONException e) {
            Toast.makeText(this, "Could not save changes", Toast.LENGTH_LONG).show();
        }
        outState.putParcelable(RC_ORIG_BIBITEM, mTargetItem);
        outState.putParcelable(RC_WORKING_BIBITEM, mWorkingItem);
    }

    private void fillForm(){
        LayoutInflater inflater =
            (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        
        LinearLayout container = ((LinearLayout)findViewById(R.id.editContainer));

        JSONObject info = mWorkingItem.getSelectedInfo();

        String type = ItemType.book; //info.optString(ItemType.type);
        JSONArray names;
        try {
            names = 
            new JSONArray("[\"title\", \"creators\", \"abstractNote\", \"series\", \"seriesNumber\", \"volume\", \"numberOfVolumes\", \"edition\", \"place\", \"publisher\", \"date\", \"numPages\", \"language\", \"ISBN\", \"shortTitle\", \"url\", \"accessDate\", \"archive\", \"archiveLocation\", \"libraryCatalog\", \"callNumber\", \"rights\", \"extra\"]");
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }

        LinearLayout row;
        for(int i=0; i<names.length(); i++){
            String key = names.optString(i);
            if(key.equals(ItemField.itemType)){
                continue;
            }else if(key.equals(ItemField.creators)){
                row = (LinearLayout) inflater.inflate(R.layout.edit_creators, container, false);
            }else{
                row = (LinearLayout) inflater.inflate(R.layout.edit_field, container, false);
                TextView tv_lbl = (TextView) row.findViewById(R.id.label);
                EditText et = (EditText) row.findViewById(R.id.content);
                tv_lbl.setText(key);
                et.setText(info.optString(key));
            }
            container.addView(row);
        }
    }

    private void extractValues() throws JSONException {
        LinearLayout container = ((LinearLayout)findViewById(R.id.editContainer));
        JSONObject info = mWorkingItem.getSelectedInfo();
        int childCount = container.getChildCount();
        LinearLayout row;
        for(int i=0; i<childCount; i++){
            row = (LinearLayout) container.getChildAt(i);
            switch(row.getId()){
            case R.id.edit_field:
                TextView tv = (TextView) row.findViewById(R.id.label);
                EditText et = (EditText) row.findViewById(R.id.content);
                String label = tv.getText().toString();
                String content = et.getText().toString();
                info.put(label, content);
                break;
            case R.id.edit_creators:
                break;
            default:
                break;
            }
        }
    }

    private final Button.OnClickListener saveListener = new Button.OnClickListener() {
        public void onClick(View v) {
            try {
                extractValues();
            } catch (JSONException e) {
                //Toast.makeText(this, "Could not save changes", Toast.LENGTH_LONG).show();
            }
            Intent result = new Intent();
            result.putExtra(EditItemActivity.INTENT_EXTRA_INDEX, mIndex);
            result.putExtra(EditItemActivity.INTENT_EXTRA_BIBITEM, mWorkingItem);
            setResult(Activity.RESULT_OK, result);
            finish();
        }
    };
}
