/*
    Copyright (C) 2019 Sweetie Piggy Apps <sweetiepiggyapps@gmail.com>

    This file is part of Bus When? (Twin Cities).

    Bus When? (Twin Cities) is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 3 of the License, or
    (at your option) any later version.

    Bus When? (Twin Cities) is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Bus When? (Twin Cities); if not, see <http://www.gnu.org/licenses/>.
*/

package com.sweetiepiggy.buswhentwincities;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DbAdapter {
    public static final String KEY_ROWID = "_id";
    public static final String KEY_STOP_ID = "stop_id";
    public static final String KEY_STOP_DESCRIPTION = "stop_description";

    public static final String TABLE_FAV_STOPS = "fav_stops";

    private DatabaseHelper mDbHelper;

    private static final String DATABASE_NAME = "buswhen.db";
    private static final int DATABASE_VERSION = 1;

    private static final String DATABASE_CREATE_FAV_STOPS =
        "CREATE TABLE " + TABLE_FAV_STOPS + " (" +
        KEY_ROWID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
        KEY_STOP_ID + " TEXT UNIQUE, " +
        KEY_STOP_DESCRIPTION + " TEXT" +
        ");";

    private static class DatabaseHelper extends SQLiteOpenHelper {
        private final Context mContext;
        public SQLiteDatabase mDb;

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
            mContext = context;
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_FAV_STOPS);
            db.execSQL(DATABASE_CREATE_FAV_STOPS);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVer, int newVer) {
            switch (oldVer) {
            default:
                break;
            }
        }

        public void open() {
            mDb = getReadableDatabase();
        }

        public void openReadWrite() throws SQLException {
            mDb = getWritableDatabase();
        }

        @Override
        public synchronized void close() {
            if (mDb != null) {
                mDb.close();
            }
            super.close();
        }
    }

    public DbAdapter() {
    }

    public DbAdapter open(Context context) {
        mDbHelper = new DatabaseHelper(context);
        mDbHelper.open();
        return this;
    }

    public DbAdapter openReadWrite(Context context) throws SQLException {
        mDbHelper = new DatabaseHelper(context);
        mDbHelper.openReadWrite();
        return this;
    }

    public void close() {
        mDbHelper.close();
    }

    /** @return rowId or -1 if failed */
    public long createFavStop(String stopId, String stopDescription) {
        ContentValues cv = new ContentValues();
        cv.put(KEY_STOP_ID, stopId);
        cv.put(KEY_STOP_DESCRIPTION, stopDescription);

        return mDbHelper.mDb.replace(TABLE_FAV_STOPS, null, cv);
    }

    public void deleteFavStop(String stopId) {
        mDbHelper.mDb.delete(TABLE_FAV_STOPS, KEY_STOP_ID + " == ?", new String[] {stopId});
    }

    public boolean isFavStop(String stopId) {
        Cursor c = mDbHelper.mDb.query(TABLE_FAV_STOPS, new String[] { KEY_STOP_ID },
                                       KEY_STOP_ID + " == ?", new String[] {stopId},
                                       null, null, null, "1");

        boolean found = c.moveToFirst();
        c.close();
        return found;
    }

    public Cursor fetchFavStops() {
        return mDbHelper.mDb.query(TABLE_FAV_STOPS, null, null, null, null, null, null, null);
    }
}
