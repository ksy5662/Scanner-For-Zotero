package org.ale.scan2zotero.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

public class S2ZDatabase extends ContentProvider {

    private static final String DBNAME = "s2z.db";
    
    private static final int DBVERSION = 1;

    public static final int ACCOUNT_ID_INDEX = 0;
    public static final int ACCOUNT_ALIAS_INDEX = 1;
    public static final int ACCOUNT_UID_INDEX = 2;
    public static final int ACCOUNT_KEY_INDEX = 3;
    
    private static final String SQL_CREATE_KEY_DB = 
        "CREATE TABLE IF NOT EXISTS "+Account.TBL_NAME+" ("
        +Account._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
        +Account.COL_ALIAS+" TEXT, "+Account.COL_UID+" TEXT, "
        +Account.COL_KEY+" TEXT UNIQUE);";

    public static final String AUTHORITY = "org.ale.scan2zotero.data.s2zdatabase";
    private static final String ACCOUNT_BASE_PATH = "account";

    public static final Uri ACCOUNT_URI = 
        Uri.parse("content://" + AUTHORITY + "/" + ACCOUNT_BASE_PATH);

    private static final int ACCOUNT = 1;
    private static final int ACCOUNT_ID = 100;

    private static final UriMatcher URI_MATCHER;
    static {
        URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
        URI_MATCHER.addURI(AUTHORITY, ACCOUNT_BASE_PATH, ACCOUNT);
        URI_MATCHER.addURI(AUTHORITY, ACCOUNT_BASE_PATH + "/#", ACCOUNT_ID);
    }

    private DatabaseHelper mSQLiteHelper;

    @Override
    public int delete(Uri arg0, String arg1, String[] arg2) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        int match = URI_MATCHER.match(uri);
        switch (match)
        {
            case ACCOUNT:
                return "vnd.android.cursor.dir/" + ACCOUNT_BASE_PATH;
            case ACCOUNT_ID:
                return "vnd.android.cursor.item/" + ACCOUNT_BASE_PATH;
            default:
                return null;
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues content) {
        if(content == null)
            return null;

        SQLiteDatabase db = mSQLiteHelper.getWritableDatabase();
        String tbl;
        switch(URI_MATCHER.match(uri)){
            case ACCOUNT:
                tbl = Account.TBL_NAME;
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        final long rowId = db.insert(tbl, null, content);
        if(rowId >=  0){
            Uri noteUri = ContentUris.withAppendedId(uri, rowId);
            getContext().getContentResolver().notifyChange(noteUri, null);
            return noteUri;
        }

        throw new SQLException("Failed to insert row into " + uri);
    }

    @Override
    public boolean onCreate() {
        mSQLiteHelper = new DatabaseHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                            String[] selectionArgs, String sortOrder) {

        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();

        switch (URI_MATCHER.match(uri)){
        case ACCOUNT:
            queryBuilder.setTables(Account.TBL_NAME);
            break;
        case ACCOUNT_ID:
            queryBuilder.setTables(Account.TBL_NAME);
            queryBuilder.appendWhere(Account._ID + "=" 
                    + uri.getLastPathSegment());
            break;
        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        SQLiteDatabase db = mSQLiteHelper.getReadableDatabase();
        Cursor cursor = queryBuilder.query(db, projection, selection,
                                            selectionArgs, null, null,
                                            sortOrder);
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    @Override
    public int update(Uri arg0, ContentValues arg1, String arg2, String[] arg3) {
        // TODO Auto-generated method stub
        return 0;
    }

private class DatabaseHelper extends SQLiteOpenHelper {
    
    public DatabaseHelper(Context context) {
        super(context, DBNAME, null, DBVERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_KEY_DB);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        //db.execSQL("DROP TABLE IF EXISTS " + Account.TBL_NAME);
        //onCreate(db);
    }
}
}
