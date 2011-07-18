package org.ale.scan2zotero;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;

public class Util {

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
                    parent.startActivityForResult(intent, Scan2ZoteroActivity.RESULT_APIKEY);
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
