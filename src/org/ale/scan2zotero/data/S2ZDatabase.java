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
import android.text.TextUtils;

public class S2ZDatabase extends ContentProvider {

    private static final String DBNAME = "s2z.db";

    private static final int DBVERSION = 2;

    public static final String AUTHORITY = "org.ale.scan2zotero.data.s2zdatabase";

    /* Account table constants */
    public static final int ACCOUNT_ID_INDEX = 0;
    public static final int ACCOUNT_ALIAS_INDEX = 1;
    public static final int ACCOUNT_UID_INDEX = 2;
    public static final int ACCOUNT_KEY_INDEX = 3;

    private static final String SQL_CREATE_ACCOUNT_TBL = 
        "CREATE TABLE IF NOT EXISTS "+Account.TBL_NAME+" ("
        +Account._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
        +Account.COL_ALIAS+" TEXT, "+Account.COL_UID+" TEXT, "
        +Account.COL_KEY+" TEXT UNIQUE);";

    private static final String ACCOUNT_BASE_PATH = "account";

    /* Bibinfo table constants */
    public static final int BIBINFO_ID_INDEX = 0;
    public static final int BIBINFO_DATE_INDEX = 1;
    public static final int BIBINFO_TYPE_INDEX = 2;
    public static final int BIBINFO_JSON_INDEX = 3;

    private static final String SQL_CREATE_BIBINFO_TBL = 
        "CREATE TABLE IF NOT EXISTS "+BibItem.TBL_NAME+" ("
        +BibItem._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
        +BibItem.COL_DATE+" INTEGER, "+BibItem.COL_TYPE+" INTEGER, "
        +BibItem.COL_JSON+" BLOB);";

    private static final String BIBINFO_BASE_PATH = "bibinfo";

    /* URI Matching */
    public static final Uri ACCOUNT_URI = 
        Uri.parse("content://" + AUTHORITY + "/" + ACCOUNT_BASE_PATH);

    public static final Uri BIBINFO_URI = 
        Uri.parse("content://" + AUTHORITY + "/" + BIBINFO_BASE_PATH);

    private static final int ACCOUNT = 1;
    private static final int ACCOUNT_ID = 100;

    private static final int BIBINFO = 2;
    private static final int BIBINFO_ID = 200;

    private static final UriMatcher URI_MATCHER;
    static {
        URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
        URI_MATCHER.addURI(AUTHORITY, ACCOUNT_BASE_PATH, ACCOUNT);
        URI_MATCHER.addURI(AUTHORITY, ACCOUNT_BASE_PATH + "/#", ACCOUNT_ID);
        URI_MATCHER.addURI(AUTHORITY, BIBINFO_BASE_PATH, BIBINFO);
        URI_MATCHER.addURI(AUTHORITY, BIBINFO_BASE_PATH + "/#", BIBINFO_ID);
    }

    private DatabaseHelper mSQLiteHelper;

    @Override
    public String getType(Uri uri) {
        int match = URI_MATCHER.match(uri);
        switch (match)
        {
            case ACCOUNT:
                return "vnd.android.cursor.dir/" + ACCOUNT_BASE_PATH;
            case ACCOUNT_ID:
                return "vnd.android.cursor.item/" + ACCOUNT_BASE_PATH;
            case BIBINFO:
                return "vnd.android.cursor.dir/" + BIBINFO_BASE_PATH;
            case BIBINFO_ID:
                return "vnd.android.cursor.item/" + BIBINFO_BASE_PATH;
            default:
                return null;
        }
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

        String tbl = getTable(uri);
        queryBuilder.setTables(tbl);

        String whereId = getWhere(uri);
        if(!TextUtils.isEmpty(whereId)){
            queryBuilder.appendWhere(whereId);
        }

        SQLiteDatabase db = mSQLiteHelper.getReadableDatabase();
        Cursor cursor = queryBuilder.query(db, projection, selection,
                                            selectionArgs, null, null,
                                            sortOrder);
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    @Override
    public Uri insert(Uri uri, ContentValues content) {
        if(content == null)
            return null;

        SQLiteDatabase db = mSQLiteHelper.getWritableDatabase();
        String tbl = getTable(uri);

        final long rowId = db.insert(tbl, null, content);
        if(rowId >=  0){
            Uri noteUri = ContentUris.withAppendedId(uri, rowId);
            getContext().getContentResolver().notifyChange(noteUri, null);
            return noteUri;
        }

        throw new SQLException("Failed to insert row into " + uri);
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        if(values == null)
            return 0;

        SQLiteDatabase db = mSQLiteHelper.getWritableDatabase();

        String tbl = getTable(uri);

        String whereId = getWhere(uri);
        if(!TextUtils.isEmpty(whereId)){
            // Only allow one of ID or selection
            if(!TextUtils.isEmpty(selection)){
                throw new IllegalArgumentException("Update by ID with non-null selection argument");
            }
            selection = whereId;
        }

        return db.update(tbl, values, selection, selectionArgs);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase db = mSQLiteHelper.getWritableDatabase();
        String tbl = getTable(uri);
        return db.delete(tbl, selection, selectionArgs);
    }

    private String getTable(Uri uri) {
        String tbl; 
        switch(URI_MATCHER.match(uri)){
            case ACCOUNT:
                tbl = Account.TBL_NAME;
                break;
            case ACCOUNT_ID:
                tbl = Account.TBL_NAME;
            case BIBINFO:
                tbl = BibItem.TBL_NAME;
                break;
            case BIBINFO_ID:
                tbl = BibItem.TBL_NAME;
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        return tbl;
    }
    
    private String getWhere(Uri uri){
        String where = null;
        switch (URI_MATCHER.match(uri)){
        case ACCOUNT:
            break;
        case BIBINFO:
            break;
        case ACCOUNT_ID:
            where = Account._ID + "=" + uri.getLastPathSegment();
            break;
        case BIBINFO_ID:
            where = BibItem._ID + "=" + uri.getLastPathSegment();
            break;
        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
        return where;
    }

private class DatabaseHelper extends SQLiteOpenHelper {
    
    public DatabaseHelper(Context context) {
        super(context, DBNAME, null, DBVERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ACCOUNT_TBL);
        db.execSQL(SQL_CREATE_BIBINFO_TBL);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + Account.TBL_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + BibItem.TBL_NAME);
        onCreate(db);
    }
}
}
