/** 
 * Copyright 2011 John M. Schanck
 * 
 * Scan2Zotero is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Scan2Zotero is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Scan2Zotero.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.ale.scan2zotero;

import org.ale.scan2zotero.data.BibItem;
import org.json.JSONObject;

import android.app.Activity;
import android.os.Bundle;

public class EditItemActivity extends Activity {

    public static final String INTENT_EXTRA_BIBITEM = "ITEMID";

    public static final String RECREATE_BIBITEM = "BIBITEM";

    private BibItem mTargetItem = null;

    @Override
    public void onCreate(Bundle state){
        super.onCreate(state);
        
        if(state != null){
            mTargetItem = state.getParcelable(RECREATE_BIBITEM);
        }else{
            Bundle extras = getIntent().getExtras();
            if(extras != null){
                mTargetItem = (BibItem) extras.getParcelable(INTENT_EXTRA_BIBITEM);
            }
        }
    }

    @Override
    public void onResume() {

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(RECREATE_BIBITEM, mTargetItem);
    }

    private void fillForm(){
        if(mTargetItem == null)
            return;
        JSONObject info = mTargetItem.getSelectedInfo();
    }
}
