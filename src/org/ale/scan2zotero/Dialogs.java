package org.ale.scan2zotero;

import java.util.ArrayList;

import org.ale.scan2zotero.data.Account;
import org.ale.scan2zotero.data.Database;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.net.http.SslCertificate;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class Dialogs {

    protected static final int DIALOG_NO_DIALOG = -1;
    // Used by LoginActivity
    protected static final int DIALOG_SAVED_KEYS = 0;
    protected static final int DIALOG_ZOTERO_LOGIN = 2;
    protected static final int DIALOG_LOGIN_HELP = 3;
    // Used by MainActivity
    protected static final int DIALOG_ZXING = 4;
    protected static final int DIALOG_CREDENTIALS = 5;
    protected static final int DIALOG_NO_PERMS = 6;
    // Used by GetApiKeyActivity
    protected static final int DIALOG_SSL = 7;
    protected static final int DIALOG_NO_KEYS = 8;
    protected static final int DIALOG_FOUND_KEYS = 9;
    // Used by ManageAccountsActivity
    protected static final int DIALOG_RENAME_KEY = 10;
    
    protected static int displayedDialog = DIALOG_NO_DIALOG;

    protected static DialogInterface.OnCancelListener ON_CANCEL = new DialogInterface.OnCancelListener(){
        public void onCancel(DialogInterface arg0) {
            Dialogs.displayedDialog = Dialogs.DIALOG_NO_DIALOG;
        } 
    };

    /* LoginActivity Dialogs */
    /* Dialog to ask user if we may go to Zotero.org to manage API keys*/
    protected static AlertDialog informUserAboutLogin(final LoginActivity parent){
        Dialogs.displayedDialog = Dialogs.DIALOG_ZOTERO_LOGIN;
        AlertDialog.Builder builder = new AlertDialog.Builder(parent);

        DialogInterface.OnClickListener clickListener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int i) {
                if(i == DialogInterface.BUTTON_POSITIVE){
                    Intent intent = new Intent(parent, GetApiKeyActivity.class);
                    parent.startActivityForResult(intent, LoginActivity.RESULT_APIKEY);
                    Dialogs.displayedDialog = Dialogs.DIALOG_NO_DIALOG;
                }else{
                    dialog.dismiss();
                    Dialogs.displayedDialog = Dialogs.DIALOG_NO_DIALOG;
                }
            }
        };

        builder.setTitle(parent.getString(R.string.redirect_title));
        builder.setMessage(parent.getString(R.string.redirect));
        builder.setPositiveButton(parent.getString(R.string.proceed), clickListener);
        builder.setNegativeButton(parent.getString(R.string.cancel), clickListener);
        builder.setOnCancelListener(ON_CANCEL);
        return builder.show();
    }

    protected static AlertDialog showLoginHelp(final LoginActivity parent){
        Dialogs.displayedDialog = Dialogs.DIALOG_LOGIN_HELP;
        AlertDialog.Builder builder = new AlertDialog.Builder(parent);

        builder.setOnCancelListener(ON_CANCEL);

        builder.setMessage(R.string.login_instructions);
        return builder.show();
    }

    /* Dialog to present the user with saved keys */
    protected static AlertDialog promptToUseSavedKey(final LoginActivity parent, final Cursor c){
        if(c.getCount() == 0){
            Toast.makeText(parent, "No saved keys", Toast.LENGTH_SHORT).show();
            return null;
        }

        Dialogs.displayedDialog = Dialogs.DIALOG_SAVED_KEYS;
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(parent);
        DialogInterface.OnClickListener clickListener = 
                    new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int i) {
                if(i == DialogInterface.BUTTON_NEGATIVE){
                    // User cancelled dialog
                    Dialogs.displayedDialog = Dialogs.DIALOG_NO_DIALOG;
                    dialog.dismiss();
                }else{ // User selected a key
                    c.moveToPosition(i);
                    int dbid = c.getInt(Database.ACCOUNT_ID_INDEX);
                    String alias = c.getString(Database.ACCOUNT_ALIAS_INDEX);
                    String uid = c.getString(Database.ACCOUNT_UID_INDEX);
                    String key = c.getString(Database.ACCOUNT_KEY_INDEX);
                    Account acct = new Account(dbid, alias, uid, key);
                    parent.setUserAndKey(acct);
                    dialog.dismiss();
                    parent.showNext();
                    Dialogs.displayedDialog = Dialogs.DIALOG_NO_DIALOG;
                }
            }
        };

        dialogBuilder.setTitle("Select a key");
        dialogBuilder.setNegativeButton(parent.getString(R.string.cancel), clickListener);
        dialogBuilder.setSingleChoiceItems(c, -1, Account.COL_ALIAS, clickListener);
        dialogBuilder.setOnCancelListener(ON_CANCEL);

        return dialogBuilder.show();
    }

    /* MainActivity Dialogs */
    /* Dialog for asking the user to install ZXing Scanner */
    protected static AlertDialog getZxingScanner(final MainActivity parent) {
        Dialogs.displayedDialog = Dialogs.DIALOG_ZXING;
        AlertDialog.Builder downloadDialog = new AlertDialog.Builder(parent);

        DialogInterface.OnClickListener clickListener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int i) {
                if(i == DialogInterface.BUTTON_POSITIVE){
                    Uri uri = Uri.parse(parent.getString(R.string.zxing_uri));
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    parent.startActivity(intent);
                    Dialogs.displayedDialog = Dialogs.DIALOG_NO_DIALOG;
                }else{
                    dialog.dismiss();
                    Dialogs.displayedDialog = Dialogs.DIALOG_NO_DIALOG;
                }
            }
        };

        downloadDialog.setTitle(parent.getString(R.string.install_bs_title));
        downloadDialog.setMessage(parent.getString(R.string.install_bs_msg));
        downloadDialog.setPositiveButton(parent.getString(R.string.install), clickListener);
        downloadDialog.setNegativeButton(parent.getString(R.string.cancel), clickListener);
        downloadDialog.setOnCancelListener(ON_CANCEL);

        return downloadDialog.show();
    }

    // Checking the user's credentials
    protected static AlertDialog showCheckingCredentialsDialog(final MainActivity parent){
        Dialogs.displayedDialog = Dialogs.DIALOG_CREDENTIALS;

        ProgressDialog dialog = ProgressDialog.show(parent, "Checking library and group access", "Please wait", true, true);
        dialog.setOnCancelListener(new ProgressDialog.OnCancelListener(){
            public void onCancel(DialogInterface view) {
                Dialogs.displayedDialog = Dialogs.DIALOG_NO_DIALOG;
                parent.logout();
            }
        });

        return dialog;
    }

    protected static AlertDialog showNoPermissionsDialog(final MainActivity parent){
        Dialogs.displayedDialog = Dialogs.DIALOG_NO_PERMS;
        AlertDialog.Builder builder = new AlertDialog.Builder(parent);

        DialogInterface.OnClickListener clickListener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int i) {
                Dialogs.displayedDialog = Dialogs.DIALOG_NO_DIALOG;
                parent.logout();
            }
        };

        builder.setTitle("No write access");
        builder.setMessage("The key you have logged in with does not have write " +
        		           "access to your library, nor to any groups. Please " +
        		           "modify this key or log in with a different one");

        builder.setNeutralButton("Log out", clickListener);
        return builder.show();
    }

    /* GetApiKeyActivity Dialogs */
    protected static AlertDialog showSSLDialog(final GetApiKeyActivity parent){
        SslCertificate cert =
            ((WebView) parent.findViewById(R.id.webView)).getCertificate();
        if(cert == null)
            return null;
        Dialogs.displayedDialog = Dialogs.DIALOG_SSL;
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
                Dialogs.displayedDialog = Dialogs.DIALOG_NO_DIALOG;
                dialog.dismiss();
            }
        });
        builder.setOnCancelListener(ON_CANCEL);


        return builder.show();
    }

    // No keys were found, provide shortcut to key creation page on zotero.org
    // or let the user abort.
    protected static AlertDialog showNoKeysDialog(final GetApiKeyActivity parent){
        Dialogs.displayedDialog = Dialogs.DIALOG_NO_KEYS;
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
                Dialogs.displayedDialog = Dialogs.DIALOG_NO_DIALOG;
                dialog.dismiss();
            }
        };

        builder.setTitle("No API Keys Found");
        builder.setPositiveButton("Create a new key", clickListener);
        builder.setNegativeButton(parent.getString(R.string.cancel), clickListener);
        builder.setOnCancelListener(ON_CANCEL);

        return builder.show();
    }

    // One or more keys were found on the page, present the user with a list,
    // by name, and let them pick the one they want to use.
    private static int selectedNewKey = 0;
    protected static AlertDialog showSelectKeyDialog(final GetApiKeyActivity parent,
                                                     final ArrayList<String> names,
                                                     final ArrayList<String> ids,
                                                     final ArrayList<String> keys){
        Dialogs.displayedDialog = Dialogs.DIALOG_FOUND_KEYS;
        AlertDialog.Builder builder = new AlertDialog.Builder(parent);
        
        DialogInterface.OnClickListener clickListener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int i) {
                if(i == DialogInterface.BUTTON_POSITIVE){
                    String userName = names.get(Dialogs.selectedNewKey);
                    String userId = ids.get(Dialogs.selectedNewKey);
                    String userKey = keys.get(Dialogs.selectedNewKey);
                    if(!TextUtils.isEmpty(userId) && !TextUtils.isEmpty(userKey)){
                        Intent resultIntent = new Intent();
                        resultIntent.putExtra(GetApiKeyActivity.ACCOUNT, 
                                new Account(userName, userId, userKey));
                        parent.setResult(Activity.RESULT_OK, resultIntent);
                        parent.finish();
                    }
                    Dialogs.displayedDialog = Dialogs.DIALOG_NO_DIALOG;
                }else if(i == DialogInterface.BUTTON_NEGATIVE){ // User cancelled dialog
                    Dialogs.displayedDialog = Dialogs.DIALOG_NO_DIALOG;
                    dialog.dismiss();
                }else{ // User clicked a key, but did not yet confirm their choice
                    Dialogs.selectedNewKey = i;
                }
            }
        };

        builder.setTitle("Found API Keys");
        // Make radio buttons w/ key names and select first key
        CharSequence uglyHackNames[] = new CharSequence[names.size()];
        uglyHackNames = names.toArray(uglyHackNames);
        builder.setSingleChoiceItems(uglyHackNames, Dialogs.selectedNewKey, clickListener);
        builder.setPositiveButton("Use selected key", clickListener);
        builder.setNegativeButton("None of these", clickListener);
        builder.setOnCancelListener(ON_CANCEL);

        return builder.show();
    }

    // One or more keys were found on the page, present the user with a list,
    // by name, and let them pick the one they want to use.
    protected static AlertDialog showRenameKeyDialog(final ManageAccountsActivity parent,
                                                     final String original,
                                                     final int row){
        Dialogs.displayedDialog = Dialogs.DIALOG_RENAME_KEY;
        AlertDialog.Builder builder = new AlertDialog.Builder(parent);
        
        final EditText input = new EditText(parent);
        input.setText(original);
        builder.setView(input);
        
        DialogInterface.OnClickListener clickListener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int i) {
                if(i == DialogInterface.BUTTON_POSITIVE){
                    String newAlias = input.getText().toString();
                    Account.renameAccount(parent.getContentResolver(), row, newAlias);
                    Dialogs.displayedDialog = Dialogs.DIALOG_NO_DIALOG;
                    parent.updateList();
                }else if(i == DialogInterface.BUTTON_NEGATIVE){ // User cancelled dialog
                    Dialogs.displayedDialog = Dialogs.DIALOG_NO_DIALOG;
                }
            }
        };

        builder.setPositiveButton("Save", clickListener);
        builder.setNegativeButton("Cancel", clickListener);
        builder.setOnCancelListener(ON_CANCEL);

        AlertDialog alert = builder.show();
        final Button positiveButton = alert.getButton(DialogInterface.BUTTON_POSITIVE);

        // Try to do some input validation... KeyListeners are apparently unreliable :(
        alert.setOnKeyListener(new AlertDialog.OnKeyListener() {
            public boolean onKey(DialogInterface arg0, int arg1, KeyEvent arg2) {
                if(input.getText().toString().matches("[A-Za-z0-9 ]*")){
                    positiveButton.setEnabled(true); 
                    input.setError(null);
                }else{
                    positiveButton.setEnabled(false);
                    input.setError("No symbols, please!");
                }
                return false;
            }
        });

        return alert;
    }
}
