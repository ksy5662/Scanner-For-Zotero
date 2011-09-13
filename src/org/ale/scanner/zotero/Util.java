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
        if(isValidISBN(content)){
            return SCAN_PARSE_ISBN;
        }
        return SCAN_PARSE_FAILURE;
    }

    public static int checksumISBN13(String isbn){
        // The check digit of an ISBN-13 is k such that
        // the sum of the even digits plus three times the sum
        // of the odd digits equals -k mod 10
        int e = (isbn.charAt(0) + isbn.charAt(2) + isbn.charAt(4) +
                 isbn.charAt(6) + isbn.charAt(8) + isbn.charAt(10)) - 6*'0'; 
        int o = (isbn.charAt(1) + isbn.charAt(3) + isbn.charAt(5) +
                 isbn.charAt(7) + isbn.charAt(9) + isbn.charAt(11)) - 6*'0';
        return (10 - ((e + 3*o) % 10)) % 10;
    }

    public static int checksumISBN10(String isbn){
        // The check digit of an ISBN-10 is k such that
        // \sum_{i=0}^{8}{isbn[i]*(10-i)} + k = 0 mod 11
        int r1 = 0;
        int r2 = 0;
        for(int i=0; i<=8; i++){
            r1 += (int) isbn.charAt(i) - '0';
            r2 += r1;
        }
        r2 += r1;
        return (11 - (r2 % 11)) % 11;
    }

    public static boolean isValidISBN(String isbn){
        if(isbn.length() == 10){ // ISBN-10
            // Last character can be 'X' to denote the value 10
            if(!TextUtils.isDigitsOnly(isbn.substring(0, 8)))
                return false;

            int checksum = checksumISBN10(isbn);
            char expected = (checksum == 10) ? 'X' : Character.forDigit(checksum, 10);

            char given = isbn.charAt(9);
            if(given == 'x') given = 'X'; // Just in case..

            // Validate against the expected checksum
            if(given != expected)
                return false;
        }else if(isbn.length() == 13){ // ISBN-13
            String ean = isbn.substring(0, 3);
            if(!(ean.equals("978") || ean.equals("979")))
                return false;

            int checksum = checksumISBN13(isbn);
            char expected = Character.forDigit(checksum, 10);

            // Validate against the expected checksum
            if(isbn.charAt(12) != expected)
                return false;
        }else{ // not an ISBN
            return false;
        }
        return true;
    }

    public static boolean isbnMatch(String isbn1, String isbn2){
        if(isbn1.length() == 13){
            isbn1 = isbn1.substring(3);
        }
        if(isbn2.length() == 13) {
            isbn2 = isbn2.substring(3);
        }
        // Have to ignore check digits in case we're comparing an 13 to a 10
        return isbn1.substring(0, 8).equals(isbn2.substring(0, 8));
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
