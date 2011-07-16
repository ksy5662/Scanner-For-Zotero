package org.ale.scan2zotero;

import android.text.TextUtils;

public class Util {
    public static boolean validateUserId(String id){
        return !TextUtils.isEmpty(id) && 
                TextUtils.isDigitsOnly(id) && 
                Integer.parseInt(id) > 0;
    }
    
    public static boolean validateApiKey(String key){
        return (key.length() == 24);
    }
}
