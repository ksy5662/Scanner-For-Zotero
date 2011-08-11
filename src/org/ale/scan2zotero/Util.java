package org.ale.scan2zotero;

import android.content.res.Resources;
import android.text.TextUtils;
import android.widget.TextView;

public class Util {

    public static final int SCAN_PARSE_FAILURE = -1;
    public static final int SCAN_PARSE_ISSN = 977;
    public static final int SCAN_PARSE_ISBN = 978;
    
    public static int parseBarcode(String content, String format){
        if(format.equals("EAN_13")){
            return Util.parseEAN13(content);
        }else if(format.equals("CODE_128")){
            return Util.parseCODE128(content);
        }
        return SCAN_PARSE_FAILURE;
    }

    public static int parseEAN13(String content){
        if(content.length() == 13 && TextUtils.isDigitsOnly(content)){
            if(content.startsWith("978") || content.startsWith("979")){
                return SCAN_PARSE_ISBN;
            }else if(content.startsWith("977")){
                return SCAN_PARSE_ISSN;
            }
        }
        return SCAN_PARSE_FAILURE;
    }
    
    public static int parseCODE128(String content){
        // A CODE_128 result may be an ISBN-10
        if(content.length() == 10 && TextUtils.isDigitsOnly(content)){
            return SCAN_PARSE_ISBN;
        }
        return SCAN_PARSE_FAILURE;
    }

    public static boolean isbnMatch(String isbn1, String isbn2){
        if(isbn1.length() == 13)
            isbn1 = isbn1.substring(3, 12);
        if(isbn2.length() == 13)
            isbn2 = isbn2.substring(3,12);
        return isbn1.equals(isbn2);
    }

    public static void fillBibTextField(TextView tv, String data){
        Resources res = tv.getResources();
        if(!TextUtils.isEmpty(data)){
            tv.setText(data);
            tv.setTextColor(res.getColor(android.R.color.primary_text_light));
        }else{
            tv.setText(res.getString(R.string.unknown));
            tv.setTextColor(res.getColor(android.R.color.tertiary_text_light));
        }
    }
}
