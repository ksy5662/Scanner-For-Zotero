package org.ale.scan2zotero;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ViewFlipper;


//http://daniel-codes.blogspot.com/2010/05/viewflipper-receiver-not-registered.html
// Seems to be an old bug but I got it a few times on GRI40, so...
public class WorkaroundViewFlipper extends ViewFlipper {

    public WorkaroundViewFlipper(Context context) {
        super(context);
    }

    public WorkaroundViewFlipper(Context context, AttributeSet set) {
        super(context, set);
    }

    @Override
    protected void onDetachedFromWindow() {
        try {
            super.onDetachedFromWindow();
        }
        catch (IllegalArgumentException e) {
            stopFlipping();
        }
    }
}
