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

package com.sweetiepiggy.buswhentwincities

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DbAdapter {

    private var mDbHelper: DatabaseHelper? = null

    private class DatabaseHelper internal constructor(private val mContext: Context) : SQLiteOpenHelper(mContext, DATABASE_NAME, null, DATABASE_VERSION) {
        var mDb: SQLiteDatabase? = null

        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL("DROP TABLE IF EXISTS $TABLE_FAV_STOPS")
            db.execSQL(DATABASE_CREATE_FAV_STOPS)
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVer: Int, newVer: Int) {
            android.util.Log.d("abc", "got here: $oldVer, $newVer")
            // changed type of stop_id from TEXT to INTEGER, made it primary key, added timestamp
            if (oldVer < 2) {
                db.execSQL("DROP TABLE IF EXISTS new_fav_stops")
                db.execSQL("""
                	CREATE TABLE new_fav_stops (
                        stop_id INTEGER PRIMARY KEY,
                        stop_description TEXT,
                        timestamp DATETIME DEFAULT CURRENT_TIMESTAMP
                    )
                """)
                db.execSQL("""
                    INSERT INTO new_fav_stops (stop_id, stop_description)
                    	SELECT stop_id, stop_description from fav_stops
                """);
                db.execSQL("DROP TABLE fav_stops");
                db.execSQL("ALTER TABLE new_fav_stops RENAME TO fav_stops");
            }
        }

        fun open() {
            mDb = readableDatabase
        }

        @Throws(SQLException::class)
        fun openReadWrite() {
            mDb = writableDatabase
        }

        @Synchronized
        override fun close() {
            if (mDb != null) {
                mDb!!.close()
            }
            super.close()
        }
    }

    fun open(context: Context): DbAdapter {
        mDbHelper = DatabaseHelper(context)
        mDbHelper!!.open()
        return this
    }

    @Throws(SQLException::class)
    fun openReadWrite(context: Context): DbAdapter {
        mDbHelper = DatabaseHelper(context)
        mDbHelper!!.openReadWrite()
        return this
    }

    fun close() {
        mDbHelper!!.close()
    }

    /** @return rowId or -1 if failed */
    fun createFavStop(stopId: Int, stopDescription: String?): Long {
        val cv = ContentValues().apply {
            put(KEY_STOP_ID, stopId)
            put(KEY_STOP_DESCRIPTION, stopDescription)
        }

        return mDbHelper!!.mDb!!.replace(TABLE_FAV_STOPS, null, cv)
    }

    fun deleteFavStop(stopId: Int) {
        mDbHelper!!.mDb!!.delete(TABLE_FAV_STOPS, "$KEY_STOP_ID == ?", arrayOf(stopId.toString()))
    }

    fun isFavStop(stopId: Int): Boolean {
        val c = mDbHelper!!.mDb!!.query(TABLE_FAV_STOPS, arrayOf(KEY_STOP_ID),
                "$KEY_STOP_ID == ?", arrayOf(stopId.toString()), null, null, null, "1")

        val found = c.moveToFirst()
        c.close()
        return found
    }

    fun getStopDesc(stopId: Int): String? {
        val c = mDbHelper!!.mDb!!.query(TABLE_FAV_STOPS, arrayOf(KEY_STOP_DESCRIPTION),
	    	"$KEY_STOP_ID == ?", arrayOf(stopId.toString()), null, null, null, "1")
        val ret = if (c.moveToFirst()) c.getString(c.getColumnIndex(KEY_STOP_DESCRIPTION)) else null
        c.close()
        return ret
    }

    fun fetchFavStops(): Cursor {
        return mDbHelper!!.mDb!!.query(TABLE_FAV_STOPS, null, null, null, null, null,
                "$KEY_TIMESTAMP DESC", null)
    }

    fun hasAnyFavorites(): Boolean {
        val c = mDbHelper!!.mDb!!.query(TABLE_FAV_STOPS, null, null, null, null, null, null, "1")
        val found = c.moveToFirst()
        c.close()
        return found
    }

    companion object {
        val KEY_STOP_ID = "stop_id"
        val KEY_STOP_DESCRIPTION = "stop_description"

        private val KEY_TIMESTAMP = "timestamp"

        private val TABLE_FAV_STOPS = "fav_stops"

        private val DATABASE_NAME = "buswhen.db"
        private val DATABASE_VERSION = 2

        private val DATABASE_CREATE_FAV_STOPS = """
	        CREATE TABLE $TABLE_FAV_STOPS (
                $KEY_STOP_ID INTEGER PRIMARY KEY,
                $KEY_STOP_DESCRIPTION TEXT,
                $KEY_TIMESTAMP DATETIME DEFAULT CURRENT_TIMESTAMP
            )
            """
    }
}
