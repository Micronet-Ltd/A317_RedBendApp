/////////////////////////////////////////////////////////////
// MySQLiteHelper
//  Helper class that is used to open the correct Database and create/upgrade tables if needed
/////////////////////////////////////////////////////////////


package com.redbend.client.micronet;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;


public class MicronetMySQLiteHelper extends SQLiteOpenHelper {

    private static final String TAG = "RBC-MNSQLiteHelper";

    private static final String DATABASE_NAME = "installlocks.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_NAME = "installlocks";

    public static final String COLUMN_ID = "id";
    public static final String COLUMN_USERID = "uid"; // the id of the requesting linux user
    public static final String COLUMN_SERVICEINSTANCEID = "sid"; // a unqique id for the instance of the service
    public static final String COLUMN_LOCK_NAME = "lockname"; // a name assigned by the user
    public static final String COLUMN_EXPIRES_RT_SECS = "expires_rt_s"; // a calendar expiration in runtime seconds

    public static final String SQL_CREATE = "create table "
            + TABLE_NAME + "(" +
            COLUMN_ID + " integer primary key autoincrement, " +
            COLUMN_USERID + " integer," +
            COLUMN_SERVICEINSTANCEID + " varchar," +
            COLUMN_LOCK_NAME + " varchar," +
            COLUMN_EXPIRES_RT_SECS + " integer" +
            ");";




    public MicronetMySQLiteHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        Log.w(TAG,
                "Creating database at version " + DATABASE_VERSION);

        database.execSQL(SQL_CREATE);

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(TAG,
                "Upgrading database from version " + oldVersion + " to "
                        + newVersion + ", which will destroy all old data");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);

        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(TAG,
                "Downgrading database from version " + oldVersion + " to "
                        + newVersion + ", which will destroy all old data");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);

        onCreate(db);
    }


    ////////////////////////////////////////////
    // getMaxExpirationElapsedSec()
    //  returns the max expiration (e.g. the expiration time of the longest lock) in elapsed seconds of runtime
    //  this can be compared to now() to determine if we are currently under lock or not
    ////////////////////////////////////////////
    long getMaxExpirationElapsedSec() {
        SQLiteDatabase database = getReadableDatabase();

        Cursor c = database.rawQuery("SELECT MAX(expires_rt_s) FROM " + TABLE_NAME, null);

        if (c.getCount() > 0) {
            c.moveToFirst();
            long expires_elapsed_s = c.getLong(0);
            c.close();

            return expires_elapsed_s;
        }

        return 0;
    }


    ///////////////////////////////////////////////////////
    // addLock()
    //  adds a lock
    ///////////////////////////////////////////////////////
    int addLock(String serviceinstanceid, int userid, String lock_name, long expires_elapsed_s) {

        try {
            ContentValues values = new ContentValues();

            values.put(COLUMN_USERID, userid);
            values.put(COLUMN_SERVICEINSTANCEID , serviceinstanceid);
            values.put(COLUMN_LOCK_NAME, lock_name);
            values.put(COLUMN_EXPIRES_RT_SECS ,expires_elapsed_s);


            SQLiteDatabase database = getWritableDatabase();

            long insertId = database.insertOrThrow(TABLE_NAME, null,
                    values);
        } catch (Exception e) {
            Log.e(TAG, "Exception addLock(): " + e.toString(), e);
            return -1;
        }
        return 0;
    }


    ///////////////////////////////////////////////////////
    // updateLock()
    //  updates a lock that has the same userid and lock_name with the new serviceinstanceid and expires_elapsed_s
    // Returns -1 on error, or the count of the locks which were updated (which should be 1 or 0)
    ///////////////////////////////////////////////////////
    int updateLock(String serviceinstanceid, int userid, String lock_name, long expires_elapsed_s) {


        // values to be updated
        ContentValues values = new ContentValues();
        values.put(COLUMN_EXPIRES_RT_SECS ,expires_elapsed_s);
        values.put(COLUMN_SERVICEINSTANCEID , serviceinstanceid);

        SQLiteDatabase database = getWritableDatabase();

        try {
            database.update(TABLE_NAME,
                    values,
                    COLUMN_USERID + "=" + userid + " and " + COLUMN_LOCK_NAME + " ='" + lock_name + "'",
                    null
            );

        } catch (Exception e) {
            Log.e(TAG, "Exception updateLock() 1: " + e.toString(), e);
            return -1;
        }

        int affectedRowCount = 0;
        try {
            Cursor cursor = database.rawQuery("SELECT changes() AS affected_row_count", null);
            if (cursor != null && cursor.getCount() > 0 && cursor.moveToFirst()) {
                affectedRowCount = cursor.getInt(0);
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception updateLock() 2: " + e.toString(), e);
            return -1;
        }

        return affectedRowCount;

    }


    int addorUpdateLock(String serviceinstanceid, int userid, String lock_name, long expires_elapsed_s) {
        int res = updateLock(serviceinstanceid, userid, lock_name, expires_elapsed_s);

        if (res < 1) {// less than one row was updated (either 0 rows or error)
            res = addLock(serviceinstanceid, userid, lock_name, expires_elapsed_s);
        }

        return res;
    } // addorUpdateLock()

    ///////////////////////////////////////////////////////
    // removeLock()
    //  remove a specific lock for a given user with a given name
    //      returns -1 on error, o the count of locks which were removed (which should be 1 or 0)
    ///////////////////////////////////////////////////////
    int removeLock(int userid, String lock_name) {

        SQLiteDatabase database = getWritableDatabase();
        database.delete(TABLE_NAME,
                COLUMN_USERID + " = " + userid + " and " +
                COLUMN_LOCK_NAME + " = '" + lock_name + "'",
                null);

        int affectedRowCount = 0;
        try {
            Cursor cursor = database.rawQuery("SELECT changes() AS affected_row_count", null);
            if (cursor != null && cursor.getCount() > 0 && cursor.moveToFirst()) {
                affectedRowCount = cursor.getInt(0);
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception removeLock(): " + e.toString(), e);
            return -1;
        }


        return affectedRowCount;
    }


    ///////////////////////////////////////////////////////
    // removeOtherLocks()
    //  remove all locks that are not from this same service instance
    //      returns -1 on error, or the count of locks which were removed
    ///////////////////////////////////////////////////////
    int removeOtherLocks(String serviceinstance_uuid) {

        SQLiteDatabase database = getWritableDatabase();
        database.delete(TABLE_NAME,
                COLUMN_SERVICEINSTANCEID + " != '" + serviceinstance_uuid + "'",
                null);


        int affectedRowCount = 0;
        try {
            Cursor cursor = database.rawQuery("SELECT changes() AS affected_row_count", null);
            if (cursor != null && cursor.getCount() > 0 && cursor.moveToFirst()) {
                affectedRowCount = cursor.getInt(0);
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception removeLock(): " + e.toString(), e);
            return -1;
        }

        return affectedRowCount;
    }


}
