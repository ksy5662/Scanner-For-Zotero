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
import org.ale.scanner.zotero.data.CreatorType;
import org.ale.scanner.zotero.data.ItemField;
import org.ale.scanner.zotero.data.ItemType;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class EditItemActivity extends Activity {

    public static final String INTENT_EXTRA_BIBITEM = "BIBITEM";
    public static final String INTENT_EXTRA_INDEX = "INDEX";

    public static final String RC_ORIG_BIBITEM = "OB";
    public static final String RC_WORKING_BIBITEM = "WB";

    private BibItem mTargetItem;
    private BibItem mWorkingItem;

    protected ArrayAdapter<String> mSpinnerAdapter;

    private LayoutInflater mInflater;
    private Resources mResources;

    private LinearLayout mCreatorList;
    private LinearLayout mNoteList;
    private LinearLayout mTagList;

    @Override
    public void onCreate(Bundle state){
        super.onCreate(state);
        setContentView(R.layout.edit);

        Bundle extras = getIntent().getExtras();
        if(state != null){
            mTargetItem = state.getParcelable(RC_ORIG_BIBITEM);
            mWorkingItem = state.getParcelable(RC_WORKING_BIBITEM);
        }else{
            mTargetItem = (BibItem) extras.getParcelable(INTENT_EXTRA_BIBITEM);
            mWorkingItem = mTargetItem.copy();
        }

        mInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mResources = getResources();

        mSpinnerAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, CreatorType.LocalizedBook);
        mSpinnerAdapter
                .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        
        findViewById(R.id.save).setOnClickListener(actionListener);
        findViewById(R.id.revert).setOnClickListener(actionListener);
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

    private void setRowColor(View v, int ctr){
        v.setBackgroundColor(
                mResources.getColor(
                        (ctr%2)==0 ? R.color.s2z_even_row : R.color.s2z_odd_row));
    }

    private void fillForm(){
        // Clear any previous data from the form
        LinearLayout container = ((LinearLayout)findViewById(R.id.editContainer));
        container.removeAllViews();

        // Get the JSON we'll be working from.
        JSONObject info = mWorkingItem.getSelectedInfo();

        final String[] fields; // XXX: temporary hack. In-order list of fields
        if(ItemType.book.equals(info.optString(ItemField.itemType))){
            fields = new String[]{
                "title", "creators", "abstractNote", "series", "seriesNumber",
                "volume", "numberOfVolumes", "edition", "place", "publisher",
                "date", "numPages", "language", "ISBN", "shortTitle", "url",
                "accessDate", "archive", "archiveLocation", "libraryCatalog",
                "callNumber", "rights", "extra", "notes", "tags"};
        }else{ // XXX: Assumes journalArticle
            fields = new String[]{
                "title", "creators", "abstractNote", "publicationTitle",
                "volume", "issue", "pages", "date", "series", "seriesTitle",
                "seriesText", "journalAbbreviation", "language", "DOI", "ISSN",
                "shortTitle", "url", "accessDate", "archive", "archiveLocation",
                "libraryCatalog", "callNumber", "rights", "extra", "tags",
                "notes"};
        }

        LinearLayout row;
        LinearLayout crow;
        int row_ctr = 0;
        for(String field : fields){
            if(field.equals(ItemField.itemType))
                continue;

            if(field.equals(ItemField.creators)){
                row = mCreatorList = (LinearLayout) mInflater.inflate(R.layout.edit_creators, container, false);

                JSONArray creators = info.optJSONArray(field);
                if(creators == null || creators.length() == 0){
                    addCreator(); // Empty creator row for new creators
                }else{
                    for(int c=0; c<creators.length(); c++){
                        JSONObject creator = creators.optJSONObject(c);
                        if(creator == null)
                            continue;
    
                        crow = addCreator();
                        Spinner sp = (Spinner) crow.findViewById(R.id.creator_type);
                        EditText et = (EditText) crow.findViewById(R.id.creator);
    
                        String curtype = creator.optString(CreatorType.type);
                        int selection = CreatorType.Book.indexOf(curtype);
                        sp.setSelection(selection >= 0 ? selection : 0);
    
                        String name = creator.optString(ItemField.Creator.name);
                        if(TextUtils.isEmpty(name)){
                            String[] firstlast = {
                                    creator.optString(ItemField.Creator.firstName),
                                    creator.optString(ItemField.Creator.lastName) };
                            name = TextUtils.join(" ", firstlast);
                        }
                        et.setText(name);
                    }
                }
            }else if(field.equals(ItemField.notes)) {
                row = mNoteList = (LinearLayout) mInflater.inflate(R.layout.edit_notes, container, false);

                JSONArray notes = info.optJSONArray(field);
                if(notes == null || notes.length() == 0){
                    addNote(); // Empty note row for new notes
                }else{
                    for(int c=0; c<notes.length(); c++){
                        JSONObject note = notes.optJSONObject(c);
                        if(note == null)
                            continue;
                        crow = addNote();
                        EditText et = (EditText) crow.findViewById(R.id.content);
                        et.setText(note.optString(ItemField.Note.note));
                    }
                }
            }else if(field.equals(ItemField.tags)) {
                row = mTagList = (LinearLayout) mInflater.inflate(R.layout.edit_tags, container, false);

                JSONArray tags = info.optJSONArray(field);
                if(tags == null || tags.length() == 0){
                    addTag(); // Empty tag row for new tags
                }else{
                    for(int c=0; c<tags.length(); c++){
                        JSONObject tag = tags.optJSONObject(c);
                        if(tag == null)
                            continue;
                        crow = addTag();
                        EditText et = (EditText) crow.findViewById(R.id.content);
                        et.setText(tag.optString(ItemField.Tag.tag));
                    }
                }
            }else{
                row = (LinearLayout) mInflater.inflate(R.layout.edit_field, container, false);
                TextView tv_lbl = (TextView) row.findViewById(R.id.label);
                EditText et = (EditText) row.findViewById(R.id.content);
                tv_lbl.setText(ItemField.Localized.get(field));
                tv_lbl.setTag(field);
                et.setText(info.optString(field));
            }

            container.addView(row);
            setRowColor(row, row_ctr);
            row_ctr++;
        }
    }

    private void extractValues() throws JSONException {
        JSONObject info = mWorkingItem.getSelectedInfo();

        LinearLayout container = ((LinearLayout)findViewById(R.id.editContainer));
        LinearLayout row;
        LinearLayout crow;
        for(int i=0; i<container.getChildCount(); i++){
            row = (LinearLayout) container.getChildAt(i);

            switch(row.getId()){
            case R.id.edit_field:
                TextView tv = (TextView) row.findViewById(R.id.label);
                EditText et = (EditText) row.findViewById(R.id.content);
                String label = (String) tv.getTag();
                String content = et.getText().toString();
                info.put(label, content);
                break;

            case R.id.edit_creators:
                JSONArray creators = new JSONArray();
                JSONObject creator;
                for(int c=0; c<row.getChildCount(); c++){
                    crow = (LinearLayout) row.getChildAt(c);
                    EditText name = (EditText) crow.findViewById(R.id.creator);
                    if(TextUtils.isEmpty(name.getText().toString())) {
                        continue;
                    }
                    Spinner sp = (Spinner) crow.findViewById(R.id.creator_type);
                    creator = new JSONObject();
                    int indx = CreatorType.LocalizedBook.indexOf((String) sp.getSelectedItem());
                    creator.put(CreatorType.type, CreatorType.Book.get(indx));
                    creator.put(ItemField.Creator.name, name.getText().toString());
                    creators.put(creator);
                }
                info.put(ItemField.creators, creators);
                break;

            case R.id.edit_notes:
                JSONArray notes = new JSONArray();
                JSONObject note;
                EditText note_et;
                for(int c=0; c<row.getChildCount(); c++){
                    crow = (LinearLayout) row.getChildAt(c);
                    note_et = (EditText) crow.findViewById(R.id.content);
                    String note_content = note_et.getText().toString();
                    if(TextUtils.isEmpty(note_content))
                        continue;

                    note = new JSONObject();
                    note.put(ItemField.Note.itemType, ItemField.Note.note);
                    note.put(ItemField.Note.note, note_content);
                    notes.put(note);
                }
                info.put(ItemField.notes, notes);
                break;

            case R.id.edit_tags:
                JSONArray tags = new JSONArray();
                JSONObject tag;
                EditText tag_et;
                for(int c=0; c<row.getChildCount(); c++){
                    crow = (LinearLayout) row.getChildAt(c);
                    tag_et = (EditText) crow.findViewById(R.id.content);
                    String tag_content = tag_et.getText().toString();
                    if(TextUtils.isEmpty(tag_content))
                        continue;

                    tag = new JSONObject();
                    tag.put(ItemField.Tag.tag, tag_content);
                    tags.put(tag);
                }
                info.put(ItemField.tags, tags);
                break;

            default:
                break;
            }
        }
    }

    protected LinearLayout addCreator(){
        LinearLayout crow = (LinearLayout) mInflater.inflate(
                R.layout.edit_creator_row, mCreatorList, false);
        ((Spinner) crow.findViewById(R.id.creator_type))
                .setAdapter(mSpinnerAdapter);
        ((Button) crow.findViewById(R.id.add_creator))
                .setOnClickListener(addListener);
        ((Button) crow.findViewById(R.id.rm_creator))
                .setOnClickListener(rmListener);
        mCreatorList.addView(crow);
        return crow;
    }

    protected LinearLayout addNote(){
        LinearLayout crow = (LinearLayout) mInflater.inflate(
                R.layout.edit_note_row, mNoteList, false);

        ((TextView) crow.findViewById(R.id.label))
                .setText(ItemField.Localized.get(ItemField.Note.note));
        ((Button) crow.findViewById(R.id.add_note))
                .setOnClickListener(addListener);
        ((Button) crow.findViewById(R.id.rm_note))
                .setOnClickListener(rmListener);

        mNoteList.addView(crow);
        return crow;
    }

    protected LinearLayout addTag(){
        LinearLayout crow = (LinearLayout) mInflater.inflate(
                R.layout.edit_tag_row, mTagList, false);

        ((TextView) crow.findViewById(R.id.label))
                .setText(ItemField.Localized.get(ItemField.Tag.tag));
        ((Button) crow.findViewById(R.id.add_tag))
                .setOnClickListener(addListener);
        ((Button) crow.findViewById(R.id.rm_tag))
                .setOnClickListener(rmListener);

        mTagList.addView(crow);
        return crow;
    }


    private final Button.OnClickListener actionListener = new Button.OnClickListener() {
        public void onClick(View v) {
            switch(v.getId()){
            case R.id.save:
                try {
                    extractValues();
                } catch (JSONException e) {
                    //Toast.makeText(this, "Could not save changes", Toast.LENGTH_LONG).show();
                }
                Intent result = new Intent();
                result.putExtra(EditItemActivity.INTENT_EXTRA_BIBITEM, mWorkingItem);
                setResult(Activity.RESULT_OK, result);
                finish();
                break;

            case R.id.revert:
                mWorkingItem = mTargetItem.copy();
                fillForm();
                break;
            }
        }
    };

    private final Button.OnClickListener addListener = new Button.OnClickListener() {
        public void onClick(View v) {
            switch(v.getId()){
            case(R.id.add_creator):
                addCreator();
                break;

            case(R.id.add_note):
                addNote();
                break;

            case(R.id.add_tag):
                addTag();
                break;
            }
        }
    };

    private final Button.OnClickListener rmListener = new Button.OnClickListener() {
        public void onClick(View v) {
            ViewGroup vg = (ViewGroup)(v.getParent().getParent());
            switch(v.getId()){
            case(R.id.rm_creator):
                mCreatorList.removeView(vg);
                if(mCreatorList.getChildCount() == 0){
                    addCreator(); // Add an empty child
                }
                break;

            case(R.id.rm_note):
                mNoteList.removeView(vg);
                if(mNoteList.getChildCount() == 0){
                    addNote(); // Add an empty child
                }
                break;

            case(R.id.rm_tag):
                mTagList.removeView(vg);
                if(mTagList.getChildCount() == 0){
                    addTag(); // Add an empty child
                }
                break;
            }
        }
    };
}
