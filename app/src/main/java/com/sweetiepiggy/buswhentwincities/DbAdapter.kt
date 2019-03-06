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
            when (oldVer) {
                else -> {
                }
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
    fun createFavStop(stopId: String, stopDescription: String?): Long {
        val cv = ContentValues()
        cv.put(KEY_STOP_ID, stopId)
        cv.put(KEY_STOP_DESCRIPTION, stopDescription)

        return mDbHelper!!.mDb!!.replace(TABLE_FAV_STOPS, null, cv)
    }

    fun deleteFavStop(stopId: String) {
        mDbHelper!!.mDb!!.delete(TABLE_FAV_STOPS, "$KEY_STOP_ID == ?", arrayOf(stopId))
    }

    fun isFavStop(stopId: String): Boolean {
        val c = mDbHelper!!.mDb!!.query(TABLE_FAV_STOPS, arrayOf(KEY_STOP_ID),
                "$KEY_STOP_ID == ?", arrayOf(stopId), null, null, null, "1")

        val found = c.moveToFirst()
        c.close()
        return found
    }

    fun getStopDesc(stopId: String): String? {
        val c = mDbHelper!!.mDb!!.query(TABLE_FAV_STOPS, arrayOf(KEY_STOP_DESCRIPTION),
	    	"$KEY_STOP_ID == ?", arrayOf(stopId), null, null, null, "1")
        val ret = if (c.moveToFirst()) c.getString(c.getColumnIndex(KEY_STOP_DESCRIPTION)) else null
        c.close()
        return ret
    }

    fun fetchFavStops(): Cursor {
        return mDbHelper!!.mDb!!.query(TABLE_FAV_STOPS, null, null, null, null, null,
                "$KEY_ROWID DESC", null)
    }

    fun hasAnyFavorites(): Boolean {
        val c = mDbHelper!!.mDb!!.query(TABLE_FAV_STOPS, null, null, null, null, null, null, "1")
        val found = c.moveToFirst()
        c.close()
        return found
    }

    companion object {
        val KEY_ROWID = "_id"
        val KEY_STOP_ID = "stop_id"
        val KEY_STOP_DESCRIPTION = "stop_description"

        val TABLE_FAV_STOPS = "fav_stops"

        private val DATABASE_NAME = "buswhen.db"
        private val DATABASE_VERSION = 1

        private val DATABASE_CREATE_FAV_STOPS = "CREATE TABLE " + TABLE_FAV_STOPS + " (" +
                KEY_ROWID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                KEY_STOP_ID + " TEXT UNIQUE, " +
                KEY_STOP_DESCRIPTION + " TEXT" +
                ");"
    }
}
