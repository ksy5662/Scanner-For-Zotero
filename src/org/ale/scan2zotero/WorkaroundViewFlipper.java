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
