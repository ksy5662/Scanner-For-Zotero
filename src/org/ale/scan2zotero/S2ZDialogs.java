package org.ale.scan2zotero;

import java.util.ArrayList;

import org.ale.scan2zotero.data.Account;
import org.ale.scan2zotero.data.S2ZDatabase;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.net.http.SslCertificate;
import android.text.TextUtils;
import android.view.animation.AnimationUtils;
import android.webkit.WebView;
import android.widget.Toast;
import android.widget.ViewFlipper;

public class S2ZDialogs {

    protected static final int DIALOG_NO_DIALOG = -1;
    // Used by S2ZLoginActivity
    protected static final int DIALOG_SAVED_KEYS = 0;
    protected static final int DIALOG_ZOTERO_REGISTER = 1;
    protected static final int DIALOG_ZOTERO_LOGIN = 2;
    // Used by S2ZMainActivity
    protected static final int DIALOG_ZXING = 3;
    // Used by GetApiKeyActivity
    protected static final int DIALOG_SSL = 4;
    protected static final int DIALOG_EMAIL_VERIFY = 5;
    protected static final int DIALOG_NO_KEYS = 6;
    protected static final int DIALOG_FOUND_KEYS = 7;
    protected static final int DIALOG_CHECKING_LOGIN = 8;

    protected static int displayedDialog = DIALOG_NO_DIALOG;

    /* S2ZLoginActivity Dialogs */
    /* Dialog to ask user if we may go to Zotero.org to manage API keys*/
    protected static AlertDialog informUserAboutLogin(final S2ZLoginActivity parent, final int loginType){
        S2ZDialogs.displayedDialog = S2ZDialogs.DIALOG_ZOTERO_LOGIN;
        AlertDialog.Builder builder = new AlertDialog.Builder(parent);

        DialogInterface.OnClickListener clickListener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int i) {
                if(i == DialogInterface.BUTTON_POSITIVE){
                    Intent intent = new Intent(parent, GetApiKeyActivity.class);
                    intent.putExtra(GetApiKeyActivity.LOGIN_TYPE, loginType);
                    parent.startActivityForResult(intent, S2ZLoginActivity.RESULT_APIKEY);
                    S2ZDialogs.displayedDialog = S2ZDialogs.DIALOG_NO_DIALOG;
                }else{
                    dialog.dismiss();
                    S2ZDialogs.displayedDialog = S2ZDialogs.DIALOG_NO_DIALOG;
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
        return builder.show();
    }
    
    /* Dialog to present the user with saved keys */
    protected static int selectedSavedKey = 0;
    protected static AlertDialog promptToUseSavedKey(final S2ZLoginActivity parent, final Cursor c){
        if(c.getCount() == 0){
            Toast.makeText(parent, "No saved keys", Toast.LENGTH_SHORT).show();
            return null;
        }

        S2ZDialogs.displayedDialog = S2ZDialogs.DIALOG_SAVED_KEYS;
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(parent);
        DialogInterface.OnClickListener clickListener = 
                    new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int i) {
                if(i == DialogInterface.BUTTON_POSITIVE){ 
                    c.moveToPosition(S2ZDialogs.selectedSavedKey);
                    int dbid = c.getInt(S2ZDatabase.ACCOUNT_ID_INDEX);
                    String alias = c.getString(S2ZDatabase.ACCOUNT_ALIAS_INDEX);
                    String uid = c.getString(S2ZDatabase.ACCOUNT_UID_INDEX);
                    String key = c.getString(S2ZDatabase.ACCOUNT_KEY_INDEX);
                    Account acct = new Account(dbid, alias, uid, key);
                    parent.setUserAndKey(acct);
                    dialog.dismiss();
                    ViewFlipper vf = (ViewFlipper)parent.findViewById(R.id.login_view_flipper);
                    vf.setInAnimation(AnimationUtils.loadAnimation(parent, R.anim.slide_in_next));
                    vf.setOutAnimation(AnimationUtils.loadAnimation(parent, R.anim.slide_out_next));
                    vf.showNext();
                    S2ZDialogs.displayedDialog = S2ZDialogs.DIALOG_NO_DIALOG;
                    S2ZDialogs.selectedSavedKey = 0;
                }else if(i == DialogInterface.BUTTON_NEGATIVE){
                    // User cancelled dialog
                    S2ZDialogs.displayedDialog = S2ZDialogs.DIALOG_NO_DIALOG;
                    S2ZDialogs.selectedSavedKey = 0;
                    dialog.dismiss();
                }else{
                    // User clicked a key, but did not yet confirm their choice
                    S2ZDialogs.selectedSavedKey = i;
                }
            }
        };
        if(S2ZDialogs.selectedSavedKey >= c.getCount())
            S2ZDialogs.selectedSavedKey = 0;
        dialogBuilder.setTitle("Login with saved key?");
        dialogBuilder.setPositiveButton("Use selected key", clickListener);
        dialogBuilder.setNegativeButton(parent.getString(R.string.cancel), clickListener);
        dialogBuilder.setSingleChoiceItems(c, S2ZDialogs.selectedSavedKey, Account.COL_ALIAS, clickListener);
        return dialogBuilder.show(); 
    }

    /* S2ZMainActivity Dialogs */
    /* Dialog for asking the user to install ZXing Scanner */
    protected static AlertDialog getZxingScanner(final S2ZMainActivity parent) {
        S2ZDialogs.displayedDialog = S2ZDialogs.DIALOG_ZXING;
        AlertDialog.Builder downloadDialog = new AlertDialog.Builder(parent);

        DialogInterface.OnClickListener clickListener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int i) {
                if(i == DialogInterface.BUTTON_POSITIVE){
                    Uri uri = Uri.parse(parent.getString(R.string.zxing_uri));
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    parent.startActivity(intent);
                    S2ZDialogs.displayedDialog = S2ZDialogs.DIALOG_NO_DIALOG;
                }else{
                    dialog.dismiss();
                    S2ZDialogs.displayedDialog = S2ZDialogs.DIALOG_NO_DIALOG;
                }
            }
        };

        downloadDialog.setTitle(parent.getString(R.string.install_bs_title));
        downloadDialog.setMessage(parent.getString(R.string.install_bs_msg));
        downloadDialog.setPositiveButton(parent.getString(R.string.install), clickListener);
        downloadDialog.setNegativeButton(parent.getString(R.string.cancel), clickListener);
        return downloadDialog.show();        
    }

    /* GetApiKeyActivity Dialogs */
    protected static AlertDialog showEmailValidationDialog(final GetApiKeyActivity parent){
        S2ZDialogs.displayedDialog = S2ZDialogs.DIALOG_EMAIL_VERIFY;
        AlertDialog.Builder builder = new AlertDialog.Builder(parent);
        builder.setTitle("Email Verification");
        builder.setMessage("You will need to verify your email address before using your new account with Scan2Zotero.");
        builder.setNeutralButton("Ok", new DialogInterface.OnClickListener(){
            public void onClick(DialogInterface dialog, int i){
                S2ZDialogs.displayedDialog = S2ZDialogs.DIALOG_NO_DIALOG;
                parent.finish();
            }
        });
       return builder.show();
    }

    protected static AlertDialog showSSLDialog(final GetApiKeyActivity parent){
        SslCertificate cert =
            ((WebView) parent.findViewById(R.id.webView)).getCertificate();
        if(cert == null)
            return null;
        S2ZDialogs.displayedDialog = S2ZDialogs.DIALOG_SSL;
        SslCertificate.DName issuer = cert.getIssuedBy();
        SslCertificate.DName owner = cert.getIssuedTo();

        String msg = (new StringBuilder())
                        .append("Issued to:\n(CN)    ").append(owner.getCName())
                        .append("\n(O)    ").append(owner.getOName())
                        .append("\n(OU)    ").append(owner.getUName())
                        .append("\n\nIssued By:\n(CN)    ").append(issuer.getCName())
                        .append("\n(O)    ").append(issuer.getOName())
                        .append("\n(OU)    ").append(issuer.getUName())
                        .append("\n\nValidity:\nIssued On    ")
                        .append(cert.getValidNotBefore().substring(0, 10))
                        .append("\nExpires On    ")
                        .append(cert.getValidNotAfter().substring(0, 10))
                        .toString();

        AlertDialog.Builder builder = new AlertDialog.Builder(parent);

        builder.setMessage(msg.toString());
        builder.setNeutralButton("Done", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int i) {
                S2ZDialogs.displayedDialog = S2ZDialogs.DIALOG_NO_DIALOG;
                dialog.dismiss();
            }
        });
        return builder.show();
    }

    // No keys were found, provide shortcut to key creation page on zotero.org
    // or let the user abort.
    protected static AlertDialog showNoKeysDialog(final GetApiKeyActivity parent){
        S2ZDialogs.displayedDialog = S2ZDialogs.DIALOG_NO_KEYS;
        AlertDialog.Builder builder = new AlertDialog.Builder(parent);

        DialogInterface.OnClickListener clickListener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int i) {
                if(i == DialogInterface.BUTTON_POSITIVE){
                    WebView wv = (WebView) parent.findViewById(R.id.webView);
                    wv.loadUrl("https://zotero.org/settings/keys/new");
                }else if(i == DialogInterface.BUTTON_NEGATIVE){
                    parent.setResult(Activity.RESULT_CANCELED, null);
                    parent.finish();
                }
                displayedDialog = DIALOG_NO_DIALOG;
                dialog.dismiss();
            }
        };

        builder.setTitle("No API Keys Found");
        builder.setPositiveButton("Create a new key", clickListener);
        builder.setNegativeButton(parent.getString(R.string.cancel), clickListener);
        return builder.show();
    }

    // One or more keys were found on the page, present the user with a list,
    // by name, and let them pick the one they want to use.
    private static int selectedNewKey = 0;
    protected static AlertDialog showSelectKeyDialog(final GetApiKeyActivity parent,
                                                     final ArrayList<String> names,
                                                     final ArrayList<String> ids,
                                                     final ArrayList<String> keys){
        S2ZDialogs.displayedDialog = S2ZDialogs.DIALOG_FOUND_KEYS;
        AlertDialog.Builder builder = new AlertDialog.Builder(parent);
        
        DialogInterface.OnClickListener clickListener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int i) {
                if(i == DialogInterface.BUTTON_POSITIVE){
                    String userName = names.get(S2ZDialogs.selectedNewKey);
                    String userId = ids.get(S2ZDialogs.selectedNewKey);
                    String userKey = keys.get(S2ZDialogs.selectedNewKey);
                    if(!TextUtils.isEmpty(userId) && !TextUtils.isEmpty(userKey)){
                        Intent resultIntent = new Intent();
                        resultIntent.putExtra(GetApiKeyActivity.ACCOUNT, 
                                new Account(userName, userId, userKey));
                        parent.setResult(Activity.RESULT_OK, resultIntent);
                        parent.finish();
                    }
                    S2ZDialogs.displayedDialog = S2ZDialogs.DIALOG_NO_DIALOG;
                }else if(i == DialogInterface.BUTTON_NEGATIVE){ // User cancelled dialog
                    S2ZDialogs.displayedDialog = S2ZDialogs.DIALOG_NO_DIALOG;
                    dialog.dismiss();
                }else{ // User clicked a key, but did not yet confirm their choice
                    S2ZDialogs.selectedNewKey = i;
                }
            }
        };

        builder.setTitle("Found API Keys");
        // Make radio buttons w/ key names and select first key
        CharSequence uglyHackNames[] = new CharSequence[names.size()];
        uglyHackNames = names.toArray(uglyHackNames);
        builder.setSingleChoiceItems(uglyHackNames, S2ZDialogs.selectedNewKey, clickListener);
        builder.setPositiveButton("Use selected key", clickListener);
        builder.setNegativeButton("None of these", clickListener);
        return builder.show();
    }
}
