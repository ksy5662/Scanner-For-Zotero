package org.ale.scan2zotero;

import android.os.Parcel;
import android.os.Parcelable;

public class PString implements Parcelable {
    String mValue;
    public PString(String s){
        mValue = s;
    }
    public int describeContents() {
        return 0;
    }
    @Override
    public void writeToParcel(Parcel p, int flags) {
        p.writeString(mValue);
    }
    public String toString(){
        return mValue;
    }
}