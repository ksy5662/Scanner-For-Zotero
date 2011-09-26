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

import java.util.ArrayList;
import java.util.List;

import org.ale.scanner.zotero.data.Account;
import org.ale.scanner.zotero.data.Database;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.net.http.SslCertificate;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;


/**
 * A static reference `displayedDialog` is kept to the currently displayed
 * dialog in order that it may be recreated during onResume.
 * 
 * Activities which use any of the dialogs below are responsible for
 * displaying and (in particular) dismissing dialogs at the appropriate
 * time.
 */
public class Dialogs {

    // The default displayedDialog value:
    protected static final int DIALOG_NO_DIALOG = -1;
    // Used by LoginActivity:
    protected static final int DIALOG_SAVED_KEYS = 0;
    protected static final int DIALOG_ZOTERO_LOGIN = 2;
    protected static final int DIALOG_LOGIN_HELP = 3;
    // Used by MainActivity:
    protected static final int DIALOG_ZXING = 4;
    protected static final int DIALOG_CREDENTIALS = 5;
    protected static final int DIALOG_NO_PERMS = 6;
    protected static final int DIALOG_MANUAL_ENTRY = 7;
    protected static final int DIALOG_SELECT_LIBRARY = 8;
    protected static final int DIALOG_SEARCH_ENGINE = 9;
    // Used by GetApiKeyActivity:
    protected static final int DIALOG_SSL = 10;
    protected static final int DIALOG_NO_KEYS = 11;
    protected static final int DIALOG_FOUND_KEYS = 12;
    // Used by ManageAccountsActivity:
    protected static final int DIALOG_RENAME_KEY = 13;


    protected static int displayedDialog = DIALOG_NO_DIALOG;

    // Static state
    private static String curSearch = "";
    private static int selection;


    protected static DialogInterface.OnCancelListener ON_CANCEL = new DialogInterface.OnCancelListener(){
        public void onCancel(DialogInterface arg0) {
            Dialogs.displayedDialog = Dialogs.DIALOG_NO_DIALOG;
        } 
    };

/* LoginActivity Dialogs */

    /**
     * Dialog which asks for permission to visit Zotero.org
     * 
     * @param parent  Context
     */
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

    /**
     * Provides the user with login instructions
     * 
     * @param parent  Context
     */
    protected static AlertDialog showLoginHelp(final LoginActivity parent){
        Dialogs.displayedDialog = Dialogs.DIALOG_LOGIN_HELP;
        AlertDialog.Builder builder = new AlertDialog.Builder(parent);

        builder.setOnCancelListener(ON_CANCEL);

        builder.setMessage(R.string.login_instructions);
        return builder.show();
    }

    /**
     * Dialog to select a saved key from a list.
     * 
     * @param parent  Context
     * @param cursor
     *            Cursor to the Account db table used to extract aliases,
     *            user IDs and api keys. The projection of the query which
     *            created this cursor must include Account._ID,
     *            Account.COL_ALIAS, Account.COL_UID, Account.COL_KEY
     */
    protected static AlertDialog promptToUseSavedKey(
            final LoginActivity parent, final Cursor cursor) {
        if(cursor.getCount() == 0){
            Toast.makeText(parent, "No saved keys", Toast.LENGTH_SHORT).show();
            return null;
        }

        Dialogs.displayedDialog = Dialogs.DIALOG_SAVED_KEYS;
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(parent);
        DialogInterface.OnClickListener clickListener = 
                    new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int i) {
                // All button choices result in immediate dismissal
                if(i != DialogInterface.BUTTON_NEGATIVE){
                    // User selected a key
                    cursor.moveToPosition(i);
                    int dbid = cursor.getInt(Database.ACCOUNT_ID_INDEX);
                    String alias = cursor.getString(Database.ACCOUNT_ALIAS_INDEX);
                    String uid = cursor.getString(Database.ACCOUNT_UID_INDEX);
                    String key = cursor.getString(Database.ACCOUNT_KEY_INDEX);
                    Account acct = new Account(dbid, alias, uid, key);
                    parent.setUserAndKey(acct);
                    parent.showNext();

                    dialog.dismiss();
                }
                Dialogs.displayedDialog = Dialogs.DIALOG_NO_DIALOG;
            }
        };

        dialogBuilder.setTitle("Select a key");
        dialogBuilder.setNegativeButton(parent.getString(R.string.cancel), clickListener);
        dialogBuilder.setSingleChoiceItems(cursor, -1, Account.COL_ALIAS, clickListener);
        dialogBuilder.setOnCancelListener(ON_CANCEL);

        return dialogBuilder.show();
    }

    
/* MainActivity Dialogs */
    /**
     * Dialog to inform the user why they need ZXing, and where they can get it
     * (market / google code), in the event that it is not already installed.
     * 
     * @param parent    Context
     */
    protected static AlertDialog getZxingScanner(final MainActivity parent) {
        Dialogs.displayedDialog = Dialogs.DIALOG_ZXING;

        // Places we can get ZXing:
        final Uri market_uri = Uri.parse("market://search?q=pname:com.google.zxing.client.android");
        final Uri gcode_uri = Uri.parse("https://code.google.com/p/zxing/downloads/detail?name=BarcodeScanner3.6.apk");

        final Intent install;
        Intent test = new Intent(Intent.ACTION_VIEW, market_uri);

        // Query for activities that can handle market:// urls
        List<ResolveInfo> marketApps = parent.getPackageManager()
                .queryIntentActivities(test, PackageManager.MATCH_DEFAULT_ONLY);

        if(marketApps.size() > 0){
            install = test;
        }else{ // If we don't have any such activities,
               // send the user to the ZXing gcode page
            install = new Intent(Intent.ACTION_VIEW, gcode_uri);
        }

        AlertDialog.Builder downloadDialog = new AlertDialog.Builder(parent);

        DialogInterface.OnClickListener clickListener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int i) {
                if(i == DialogInterface.BUTTON_POSITIVE){
                    parent.startActivity(install);
                }
                // about to be dismissed
                Dialogs.displayedDialog = Dialogs.DIALOG_NO_DIALOG;
            }
        };

        downloadDialog.setTitle(parent.getString(R.string.install_bs_title));
        downloadDialog.setMessage(parent.getString(R.string.install_bs_msg));
        downloadDialog.setPositiveButton(parent.getString(R.string.install), clickListener);
        downloadDialog.setNegativeButton(parent.getString(R.string.cancel), clickListener);
        downloadDialog.setOnCancelListener(ON_CANCEL);

        return downloadDialog.show();
    }

    /**
     * Progress dialog which informs the user that we're checking their library
     * and group access.
     * 
     * @param parent    Context
     */
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

    /**
     * Notifies the user that they have insufficient permissions to access or
     * write to any libraries before sending them back to the login screen.
     * 
     * @param parent    Context
     */
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

    /**
     * Allows the user to enter an ISBN manually.
     * XXX: This validates the ISBN entered, and won't perform a lookup
     * unless the checksum checks out. Unfortunately, many books have been
     * printed with invalid ISBNs.
     *
     * @param parent    Context
     */
    protected static AlertDialog showManualEntryDialog(final MainActivity parent){
        Dialogs.displayedDialog = Dialogs.DIALOG_MANUAL_ENTRY;

        // Inflate manual entry layout
        LayoutInflater factory = LayoutInflater.from(parent);
        final View dview = factory.inflate(R.layout.manual_entry_field, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(parent);

        builder.setTitle(R.string.manual_entry_title);
        builder.setView(dview);

        builder.setOnCancelListener(ON_CANCEL);

        final AlertDialog dialog = builder.create();
        final EditText tv = (EditText) dview.findViewById(R.id.edit_field);
        
        // This is ridiculous but otherwise we lose the query contents on orientation changes
        tv.append(Dialogs.curSearch);
        dialog.setOnKeyListener(new AlertDialog.OnKeyListener(){
            public boolean onKey(DialogInterface arg0, int arg1, KeyEvent arg2) {
                curSearch = tv.getText().toString();
                return false;
            }
        });

        View.OnClickListener clickListener = new View.OnClickListener() {
            public void onClick(View v) {
                if(v.getId() == R.id.positive){
                    String ident = tv.getText().toString();
                    ident = ident.replaceAll("-","");
                    if(ident.length() == 9) {
                        // User may have been unable to enter trailing X
                        ident += 'X';
                    }
                    if(Util.isValidISBN(ident)){
                        parent.lookupISBN(ident);
                    }else if(Util.isValidISSN(ident)){
                        parent.lookupISSN(ident);
                    }else{
                        tv.setError("Invalid ISBN");
                        return; // avoid dismissing the dialog
                    }
                }
                Dialogs.curSearch = "";
                dialog.dismiss();
                Dialogs.displayedDialog = Dialogs.DIALOG_NO_DIALOG;
            }
        };

        ((Button)dview.findViewById(R.id.positive)).setOnClickListener(clickListener);
        ((Button)dview.findViewById(R.id.negative)).setOnClickListener(clickListener);
        
        dialog.show();
        return dialog;
    }

    /**
     * Allows the user to select their upload destination, such as their
     * personal library, or that of a group to which they have access.
     * 
     * @param parent    Context
     * @param groups    Sparse array mapping group IDs to group names.
     *                  Note: Always maps Group.GROUP_LIBRARY to "My Library"
     * @param selected  Group ID of currently selected group
     */
    protected static AlertDialog showSelectLibraryDialog(
            final MainActivity parent,
            final SparseArray<PString> groups,
            final int selected) {

        Dialogs.displayedDialog = Dialogs.DIALOG_SELECT_LIBRARY;
        Dialogs.selection = groups.indexOfKey(selected);
        if(Dialogs.selection < 0)
            Dialogs.selection = 0;
        AlertDialog.Builder builder = new AlertDialog.Builder(parent);

        DialogInterface.OnClickListener clickListener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int i) {
                if(i == DialogInterface.BUTTON_POSITIVE){
                    int gid = groups.keyAt(Dialogs.selection);
                    parent.setSelectedGroup(gid);
                    Dialogs.displayedDialog = Dialogs.DIALOG_NO_DIALOG;
                }else if(i == DialogInterface.BUTTON_NEGATIVE){
                    Dialogs.displayedDialog = Dialogs.DIALOG_NO_DIALOG;
                }else{
                    Dialogs.selection = i;
                }
            }
        };

        builder.setTitle("Select upload destination");
        // Make radio buttons w/ key names and select first key
        CharSequence libraries[] = new CharSequence[groups.size()];
        for(int i=0; i < libraries.length; i++){
            libraries[i] = groups.get(groups.keyAt(i));
        }
        builder.setSingleChoiceItems(libraries, Dialogs.selection, clickListener);
        builder.setPositiveButton("Use selected", clickListener);
        builder.setNegativeButton(R.string.cancel, clickListener);
        builder.setOnCancelListener(ON_CANCEL);

        return builder.show();
    }

    /**
     * Allows the user to select from a list of available search engines.
     * Currently: Google Books (0), WorldCat (1)
     * 
     * @param parent    Context
     * @param curSearchEngine   Index of current search engine in R.array.search_engines
     */
    protected static AlertDialog showSearchEngineDialog(
            final MainActivity parent,
            final int curSearchEngine) {

        Dialogs.displayedDialog = Dialogs.DIALOG_SEARCH_ENGINE;
        Dialogs.selection = curSearchEngine;
        AlertDialog.Builder builder = new AlertDialog.Builder(parent);

        DialogInterface.OnClickListener clickListener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int i) {
                if(i == DialogInterface.BUTTON_POSITIVE){
                    parent.setISBNService(Dialogs.selection);
                    Dialogs.displayedDialog = Dialogs.DIALOG_NO_DIALOG;
                }else if(i == DialogInterface.BUTTON_NEGATIVE){
                    Dialogs.displayedDialog = Dialogs.DIALOG_NO_DIALOG;
                }else{
                    Dialogs.selection = i;
                }
            }
        };

        builder.setTitle("Select search engine");
        builder.setSingleChoiceItems(R.array.search_engines, selection, clickListener);
        builder.setPositiveButton("Use selected", clickListener);
        builder.setNegativeButton(R.string.cancel, clickListener);
        builder.setOnCancelListener(ON_CANCEL);

        return builder.show();
    }

    /**
     * Displays some basic information about the SSL certificate loaded in
     * parent's webview. XXX: Android doesn't give us the fingerprint...
     * 
     * @param parent    Context
     */
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
            }
        });
        builder.setOnCancelListener(ON_CANCEL);


        return builder.show();
    }

    /**
     * When no keys are found on the 'Feeds/API' page, this provides a link to the
     * key creation page.
     * 
     * @param parent    Context
     */
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
            }
        };

        builder.setTitle("No API Keys Found");
        builder.setPositiveButton("Create a new key", clickListener);
        builder.setNegativeButton(parent.getString(R.string.cancel), clickListener);
        builder.setOnCancelListener(ON_CANCEL);

        return builder.show();
    }

    /**
     * When one or more keys are found on the 'Feeds/API' page, present the user
     * with a list of key aliases and let them pick the one they want to use.
     * 
     * @param parent    Context
     * @param names     Key Aliases scraped from site
     * @param ids       Key IDs scraped from site
     * @param keys      API keys scraped from site
     */
    protected static AlertDialog showSelectKeyDialog(final GetApiKeyActivity parent,
                                                     final ArrayList<String> names,
                                                     final ArrayList<String> ids,
                                                     final ArrayList<String> keys){
        Dialogs.displayedDialog = Dialogs.DIALOG_FOUND_KEYS;
        Dialogs.selection = 0;
        AlertDialog.Builder builder = new AlertDialog.Builder(parent);
        
        DialogInterface.OnClickListener clickListener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int i) {
                if(i >= 0){ // User clicked a key, but did not yet confirm their choice
                    Dialogs.selection = i;
                }else{ // Other buttons ultimately result in dismissal
                    Dialogs.displayedDialog = Dialogs.DIALOG_NO_DIALOG;
                }

                if(i == DialogInterface.BUTTON_POSITIVE){
                    String userName = names.get(Dialogs.selection);
                    String userId = ids.get(Dialogs.selection);
                    String userKey = keys.get(Dialogs.selection);
                    if(!TextUtils.isEmpty(userId) && !TextUtils.isEmpty(userKey)){
                        Intent resultIntent = new Intent();
                        resultIntent.putExtra(GetApiKeyActivity.ACCOUNT, 
                                new Account(userName, userId, userKey));
                        parent.setResult(Activity.RESULT_OK, resultIntent);
                        parent.finish();
                    }
                }else if(i == DialogInterface.BUTTON_NEGATIVE){
                    WebView wv = (WebView) parent.findViewById(R.id.webView);
                    wv.loadUrl("https://zotero.org/settings/keys/new");
                }
            }
        };

        builder.setTitle("Found API Keys");
        // Make radio buttons w/ key names and select first key
        CharSequence uglyHackNames[] = new CharSequence[names.size()];
        uglyHackNames = names.toArray(uglyHackNames);
        builder.setSingleChoiceItems(uglyHackNames, Dialogs.selection, clickListener);
        builder.setPositiveButton("Use selected key", clickListener);
        builder.setNegativeButton("Create new key", clickListener);
        builder.setOnCancelListener(ON_CANCEL);

        return builder.show();
    }


/* ManageAccountsActivity dialogs */

    /**
     * Allows the user to rename their saved keys
     * 
     * @param parent    the context
     * @param original  Original key alias
     * @param row       Database _ID of the key to edit
     */
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
