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
import com.google.android.gms.maps.model.LatLng

class DbAdapter {

    private var mDbHelper: DatabaseHelper? = null

    private class DatabaseHelper internal constructor(private val mContext: Context) : SQLiteOpenHelper(mContext, DATABASE_NAME, null, DATABASE_VERSION) {
        var mDb: SQLiteDatabase? = null

        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(DATABASE_CREATE_FAV_STOPS)
            db.execSQL(DATABASE_CREATE_NEXTRIPS)
            db.execSQL(DATABASE_CREATE_NEXTRIPS_INDEX)
            db.execSQL(DATABASE_CREATE_LAST_UPDATE)
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVer: Int, newVer: Int) {
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
            if (oldVer < 3) {
                db.execSQL(DATABASE_CREATE_NEXTRIPS)
                db.execSQL(DATABASE_CREATE_NEXTRIPS_INDEX)
                db.execSQL(DATABASE_CREATE_LAST_UPDATE)
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

    fun deletePastDueNexTrips(secondsBeforeNowToDelete: Int) {
        mDbHelper!!.mDb!!.delete(TABLE_NEXTRIPS, "$KEY_DEPARTURE_UNIX_TIME < strftime(\"%s\", 'now') - ?",
    		arrayOf(secondsBeforeNowToDelete.toString()))
    }

    fun getLastUpdate(stopId: Int): Long? {
        val c = mDbHelper!!.mDb!!.query(TABLE_LAST_UPDATE, arrayOf(KEY_LAST_UPDATE),
	        "$KEY_STOP_ID == ?", arrayOf(stopId.toString()), null, null, null, "1");
        val ret = if (c.moveToFirst()) c.getLong(c.getColumnIndex(KEY_LAST_UPDATE)) else null
        c.close()
        return ret
    }

    private fun setLastUpdate(stopId: Int, lastUpdate: Long) {
        val cv = ContentValues().apply {
            put(KEY_STOP_ID, stopId)
            put(KEY_LAST_UPDATE, lastUpdate)
        }

        mDbHelper!!.mDb!!.replace(TABLE_LAST_UPDATE, null, cv)
    }

    fun updateNexTrips(stopId: Int, nexTrips: List<NexTrip>, lastUpdate: Long) {
        val db = mDbHelper!!.mDb!!
        db.beginTransaction();
        try {
            db.delete(TABLE_NEXTRIPS, "$KEY_STOP_ID == ?", arrayOf(stopId.toString()))
            for (nexTrip in nexTrips.filter { it.departureTimeInMillis != null }) {
                val cv = ContentValues().apply {
                    put(KEY_STOP_ID, stopId)
                    put(KEY_IS_ACTUAL, nexTrip.isActual)
                    put(KEY_BLOCK_NUMBER, nexTrip.blockNumber)
                    put(KEY_DEPARTURE_UNIX_TIME, nexTrip.departureTimeInMillis!! / 1000)
                    put(KEY_DESCRIPTION, nexTrip.description)
                    put(KEY_GATE, nexTrip.gate)
                    put(KEY_ROUTE, nexTrip.route)
                    directionToInt(nexTrip.routeDirection)?.let { put(KEY_ROUTE_DIRECTION, it) }
                    put(KEY_TERMINAL, nexTrip.terminal)
                    put(KEY_VEHICLE_HEADING, nexTrip.vehicleHeading)
                    put(KEY_VEHICLE_LATITUDE, nexTrip.position?.latitude)
                    put(KEY_VEHICLE_LONGITUDE, nexTrip.position?.longitude)
                }
                db.insert(TABLE_NEXTRIPS, null, cv)
            }
            setLastUpdate(stopId, lastUpdate)
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    fun getNexTrips(stopId: Int, secondsBeforeNowToIgnore: Int, suppressLocations: Boolean): List<NexTrip> {
        val nexTrips: MutableList<NexTrip> = mutableListOf()
        val c = mDbHelper!!.mDb!!.query(TABLE_NEXTRIPS,
        	arrayOf(KEY_IS_ACTUAL, KEY_BLOCK_NUMBER, KEY_DEPARTURE_UNIX_TIME, KEY_DESCRIPTION,
    			KEY_GATE, KEY_ROUTE, KEY_ROUTE_DIRECTION, KEY_TERMINAL, KEY_VEHICLE_HEADING,
    		    KEY_VEHICLE_LATITUDE, KEY_VEHICLE_LONGITUDE),
        	"$KEY_STOP_ID == ? AND $KEY_DEPARTURE_UNIX_TIME >= strftime(\"%s\", 'now') - ?",
        	arrayOf(stopId.toString(), secondsBeforeNowToIgnore.toString()), null, null,
        	"$KEY_DEPARTURE_UNIX_TIME ASC", null)
        val isActualIndex = c.getColumnIndex(KEY_IS_ACTUAL)
        val blockNumberIndex = c.getColumnIndex(KEY_BLOCK_NUMBER)
        val departureUnixTimeIndex = c.getColumnIndex(KEY_DEPARTURE_UNIX_TIME)
        val descriptionIndex = c.getColumnIndex(KEY_DESCRIPTION)
        val gateIndex = c.getColumnIndex(KEY_GATE)
        val routeIndex = c.getColumnIndex(KEY_ROUTE)
        val routeDirectionIndex = c.getColumnIndex(KEY_ROUTE_DIRECTION)
        val terminalIndex = c.getColumnIndex(KEY_TERMINAL)
        val vehicleHeadingIndex = c.getColumnIndex(KEY_VEHICLE_HEADING)
        val vehicleLatitudeIndex = c.getColumnIndex(KEY_VEHICLE_LATITUDE)
        val vehicleLongitudeIndex = c.getColumnIndex(KEY_VEHICLE_LONGITUDE)
        while (c.moveToNext()) {
            val isActual = c.getInt(isActualIndex) != 0
            val blockNumber = c.getInt(blockNumberIndex)
            val departureTimeInMillis = c.getLong(departureUnixTimeIndex) * 1000
            val description = c.getString(descriptionIndex)
            val gate = c.getString(gateIndex)
            val route = c.getString(routeIndex)
            val routeDirection = directionFromInt(c.getInt(routeDirectionIndex))
            val terminal = c.getString(terminalIndex)
            val vehicleHeading = if (suppressLocations) null else c.getDouble(vehicleHeadingIndex)
            val vehicleLatitude = if (suppressLocations) null else c.getDouble(vehicleLatitudeIndex)
            val vehicleLongitude = if (suppressLocations) null else c.getDouble(vehicleLongitudeIndex)
            nexTrips.add(NexTrip(
                isActual, blockNumber, departureTimeInMillis, description,
                gate, route, routeDirection, terminal, vehicleHeading,
                vehicleLatitude, vehicleLongitude
            ))
        }
        c.close()
        return nexTrips
    }

    companion object {
        val KEY_STOP_ID = "stop_id"
        val KEY_STOP_DESCRIPTION = "stop_description"

        private val KEY_IS_ACTUAL = "is_actual"
        private val KEY_BLOCK_NUMBER = "block_number"
        private val KEY_DEPARTURE_UNIX_TIME = "departure_time"
        private val KEY_DESCRIPTION = "description"
        private val KEY_GATE = "gate"
        private val KEY_ROUTE = "route"
        private val KEY_ROUTE_DIRECTION = "route_direction"
        private val KEY_TERMINAL = "terminal"
        private val KEY_VEHICLE_HEADING = "vehicle_heading"
        private val KEY_VEHICLE_LATITUDE = "vehicle_latitude"
        private val KEY_VEHICLE_LONGITUDE = "vehicle_longitude"

        private val KEY_TIMESTAMP = "timestamp"
        private val KEY_LAST_UPDATE = "last_update"

        private val TABLE_FAV_STOPS = "fav_stops"
        private val TABLE_NEXTRIPS = "nextrips"
        private val TABLE_LAST_UPDATE = "last_update"

        private val INDEX_NEXTRIPS = "index_nextrips"

        private val DATABASE_NAME = "buswhen.db"
        private val DATABASE_VERSION = 3

        private val DATABASE_CREATE_FAV_STOPS = """
	        CREATE TABLE $TABLE_FAV_STOPS (
                $KEY_STOP_ID INTEGER PRIMARY KEY,
                $KEY_STOP_DESCRIPTION TEXT,
                $KEY_TIMESTAMP DATETIME DEFAULT CURRENT_TIMESTAMP
            )
            """

        private val DATABASE_CREATE_NEXTRIPS = """
	        CREATE TABLE $TABLE_NEXTRIPS (
                $KEY_STOP_ID INTEGER NOT NULL,
                $KEY_IS_ACTUAL BOOLEAN NOT NULL,
                $KEY_BLOCK_NUMBER INTEGER,
                $KEY_DEPARTURE_UNIX_TIME DATETIME NOT NULL,
                $KEY_DESCRIPTION TEXT,
                $KEY_GATE TEXT,
                $KEY_ROUTE TEXT,
                $KEY_ROUTE_DIRECTION INTEGER,
                $KEY_TERMINAL TEXT,
                $KEY_VEHICLE_HEADING DOUBLE,
                $KEY_VEHICLE_LATITUDE DOUBLE,
                $KEY_VEHICLE_LONGITUDE DOUBLE
            )
            """

        private val DATABASE_CREATE_NEXTRIPS_INDEX = """
	        CREATE INDEX $INDEX_NEXTRIPS ON $TABLE_NEXTRIPS ($KEY_STOP_ID)
            """

        private val DATABASE_CREATE_LAST_UPDATE = """
	        CREATE TABLE $TABLE_LAST_UPDATE (
                $KEY_STOP_ID INTEGER PRIMARY KEY,
                $KEY_LAST_UPDATE DATETIME
            )
            """

        private val DIRECTION_SOUTH = 0
        private val DIRECTION_EAST = 1
        private val DIRECTION_WEST = 2
        private val DIRECTION_NORTH = 3

        private fun directionToInt(dir: NexTrip.Direction?): Int? =
            when (dir) {
                NexTrip.Direction.SOUTH -> DIRECTION_SOUTH
                NexTrip.Direction.EAST  -> DIRECTION_EAST
                NexTrip.Direction.WEST  -> DIRECTION_WEST
                NexTrip.Direction.NORTH -> DIRECTION_NORTH
                else -> null
            }

        private fun directionFromInt(dir: Int): NexTrip.Direction? =
            when (dir) {
                DIRECTION_SOUTH -> NexTrip.Direction.SOUTH
                DIRECTION_EAST  -> NexTrip.Direction.EAST
                DIRECTION_WEST  -> NexTrip.Direction.WEST
                DIRECTION_NORTH -> NexTrip.Direction.NORTH
                else -> null
            }
    }
}
