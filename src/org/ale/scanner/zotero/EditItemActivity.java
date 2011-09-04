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
import org.json.JSONObject;

import android.app.Activity;
import android.os.Bundle;

public class EditItemActivity extends Activity {

    public static final String INTENT_EXTRA_BIBITEM = "ITEMID";

    public static final String RC_ORIG_BIBITEM = "OB";
    public static final String RC_WORKING_BIBITEM = "WB";

    private BibItem mTargetItem;
    private BibItem mWorkingItem;

    @Override
    public void onCreate(Bundle state){
        super.onCreate(state);

        if(state != null){
            mTargetItem = state.getParcelable(RC_ORIG_BIBITEM);
            mWorkingItem = state.getParcelable(RC_WORKING_BIBITEM);
        }else{
            Bundle extras = getIntent().getExtras();
            mTargetItem = (BibItem) extras.getParcelable(INTENT_EXTRA_BIBITEM);
            mWorkingItem = mTargetItem.copy();
        }
    }

    @Override
    public void onResume() {

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(RC_ORIG_BIBITEM, mTargetItem);
        outState.putParcelable(RC_WORKING_BIBITEM, mWorkingItem);
    }

    private void fillForm(){
        if(mTargetItem == null)
            return;
        JSONObject info = mTargetItem.getSelectedInfo();
    }
}
