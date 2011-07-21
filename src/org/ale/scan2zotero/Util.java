package org.ale.scan2zotero;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

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

    /* Dialog for asking the user to install ZXing Scanner */
    protected static AlertDialog getZxingScanner(final Activity parent) {
        AlertDialog.Builder downloadDialog = new AlertDialog.Builder(parent);
        downloadDialog.setTitle(parent.getString(R.string.install_bs_title));
        downloadDialog.setMessage(parent.getString(R.string.install_bs_msg));
        downloadDialog.setPositiveButton(parent.getString(R.string.install), new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialogInterface, int i) {
            Uri uri = Uri.parse(parent.getString(R.string.zxing_uri));
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            parent.startActivity(intent);
          }
        });
        downloadDialog.setNegativeButton(parent.getString(R.string.cancel), new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialogInterface, int i) {
              dialogInterface.dismiss();
          }
        });
        return downloadDialog.show();        
    }

    /* Dialog to ask user if we may go to Zotero.org to manage API keys*/
    protected static void informUserAboutLogin(final Activity parent, final int loginType){
        AlertDialog.Builder builder = new AlertDialog.Builder(parent);

        DialogInterface.OnClickListener clickListener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int i) {
                if(i == DialogInterface.BUTTON_POSITIVE){
                    Intent intent = new Intent(parent, GetApiKeyActivity.class);
                    intent.putExtra(GetApiKeyActivity.LOGIN_TYPE, loginType);
                    parent.startActivityForResult(intent, S2ZLoginActivity.RESULT_APIKEY);
                }else{
                    dialog.dismiss();
                }
            }
        };

        builder.setTitle(parent.getString(R.string.redirect_title));
        if(loginType == GetApiKeyActivity.EXISTING_ACCOUNT)
            builder.setMessage(parent.getString(R.string.redirect));
        else
            builder.setMessage(parent.getString(R.string.register));
        builder.setPositiveButton(parent.getString(R.string.proceed), clickListener);
        builder.setNegativeButton(parent.getString(R.string.cancel), clickListener);
        builder.show();
    }
}
