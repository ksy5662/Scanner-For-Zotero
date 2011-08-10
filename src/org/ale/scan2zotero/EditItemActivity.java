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
