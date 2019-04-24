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
import org.osmdroid.util.GeoPoint

class DbAdapter {

    private var mDbHelper: DatabaseHelper? = null

    private class DatabaseHelper internal constructor(private val mContext: Context) : SQLiteOpenHelper(mContext, DATABASE_NAME, null, DATABASE_VERSION) {
        var mDb: SQLiteDatabase? = null

        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(DATABASE_CREATE_FAV_STOPS)
            db.execSQL(DATABASE_CREATE_FILTERS)
            db.execSQL(DATABASE_CREATE_NEXTRIPS)
            db.execSQL(DATABASE_CREATE_NEXTRIPS_INDEX)
            db.execSQL(DATABASE_CREATE_STOPS)
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
                """)
                db.execSQL("DROP TABLE fav_stops")
                db.execSQL("ALTER TABLE new_fav_stops RENAME TO fav_stops")
            }
            if (oldVer < 3) {
                db.execSQL("""
                    CREATE TABLE nextrips (
                        stop_id INTEGER NOT NULL,
                        is_actual BOOLEAN NOT NULL,
                        block_number INTEGER,
                        departure_time DATETIME NOT NULL,
                        description TEXT,
                        gate TEXT,
                        route TEXT,
                        route_direction INTEGER,
                        terminal TEXT,
                        vehicle_heading DOUBLE,
                        vehicle_latitude DOUBLE,
                        vehicle_longitude DOUBLE
                    )
                """)
                db.execSQL("""
                    CREATE INDEX index_nextrips ON nextrips (stop_id)
                """)
                db.execSQL("""
                    CREATE TABLE last_update (
                        stop_id INTEGER PRIMARY KEY,
                        last_update DATETIME
                    )
                """)
            }
            if (oldVer < 4) {
                db.execSQL("DROP TABLE IF EXISTS new_fav_stops")
                db.execSQL("""
                    CREATE TABLE new_fav_stops (
                        stop_id INTEGER PRIMARY KEY,
                        stop_description TEXT,
                        position INTEGER
                    )
                """)
                val c =  db.query("fav_stops", arrayOf("stop_id", "stop_description"),
                	null, null, null, null, "timestamp DESC", null)
                val stopIdIndex = c.getColumnIndex("stop_id")
                val stopDescriptionIndex = c.getColumnIndex("stop_description")
                var position = 0
                while (c.moveToNext()) {
                    val stopId = c.getInt(stopIdIndex)
                    val stopDescription = c.getString(stopDescriptionIndex)
                    val cv = ContentValues().apply {
                        put("stop_id", stopId)
                        put("stop_description", stopDescription)
                        put("position", position)
                    }

                    db.insert("new_fav_stops", null, cv)
                    position += 1
                }
                db.execSQL("DROP TABLE fav_stops")
                db.execSQL("ALTER TABLE new_fav_stops RENAME TO fav_stops")
            }
            if (oldVer < 5) {
                db.execSQL("""
                    CREATE TABLE filters (
                        stop_id INTEGER NOT NULL,
                        route_and_terminal TEXT NOT NULL,
                        do_show BOOLEAN NOT NULL,
                        FOREIGN KEY(stop_id) REFERENCES fav_stops(stop_id),
                        PRIMARY KEY(stop_id, route_and_terminal)
                    )
                """)
            }
            if (oldVer < 6) {
                db.execSQL("DROP TABLE filters")
                db.execSQL("""
                    CREATE TABLE filters (
                        stop_id INTEGER NOT NULL,
                        route TEXT,
                        terminal TEXT,
                        do_show BOOLEAN NOT NULL,
                        FOREIGN KEY(stop_id) REFERENCES fav_stops(stop_id),
                        PRIMARY KEY(stop_id, route, terminal)
                    )
                """)
            }
            // create stops table, versionCode 37
            if (oldVer < 7) {
                db.execSQL("""
	            	CREATE TABLE stops (
                        stop_id INTEGER PRIMARY KEY,
                        stop_name TEXT NOT NULL,
                        stop_desc TEXT,
                        stop_lat DOUBLE NOT NULL,
                        stop_lon DOUBLE NOT NULL,
                        wheelchair_boarding INTEGER,
                        last_update DATETIME
                    )
                """)
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
            put(KEY_POSITION, getNewFavPosition())
        }

        return mDbHelper!!.mDb!!.insert(TABLE_FAV_STOPS, null, cv)
    }

    private fun getNewFavPosition(): Int {
        val c = mDbHelper!!.mDb!!.query(TABLE_FAV_STOPS, arrayOf("COALESCE(MAX($KEY_POSITION) + 1, 0)"),
	    	null, null, null, null, null, null)
        val ret = if (c.moveToFirst()) c.getInt(0) else 0
        c.close()
        return ret
    }

    fun deleteFavStop(stopId: Int) {
        val db = mDbHelper!!.mDb!!
        db.delete(TABLE_FILTERS, "$KEY_STOP_ID == ?", arrayOf(stopId.toString()))
        val c = db.query(TABLE_FAV_STOPS, arrayOf(KEY_POSITION),
	    		"$KEY_STOP_ID == ?", arrayOf(stopId.toString()), null, null, null, "1")
        if (c.moveToFirst()) {
            deleteFavStopAtPosition(c.getInt(c.getColumnIndex(KEY_POSITION)))
        }
        c.close()
    }

    fun deleteFavStopAtPosition(position: Int) {
        val db = mDbHelper!!.mDb!!
        db.beginTransaction();
        try {
            db.delete(TABLE_FAV_STOPS, "$KEY_POSITION == ?", arrayOf(position.toString()))
            db.execSQL("UPDATE $TABLE_FAV_STOPS SET $KEY_POSITION = $KEY_POSITION - 1 WHERE $KEY_POSITION > ?",
            	arrayOf(position.toString()))
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    fun moveFavStop(fromPosition: Int, toPosition: Int) {
        val db = mDbHelper!!.mDb!!
        val c = db.query(TABLE_FAV_STOPS, arrayOf(KEY_STOP_ID), "$KEY_POSITION == ?",
        	arrayOf(fromPosition.toString()), null, null, null, "1")
        if (c.moveToFirst()) {
            db.beginTransaction();
            try {
                val stopId = c.getInt(c.getColumnIndex(KEY_STOP_ID))
                if (fromPosition < toPosition){
                    db.execSQL("UPDATE $TABLE_FAV_STOPS SET $KEY_POSITION = $KEY_POSITION - 1 WHERE ? < $KEY_POSITION AND $KEY_POSITION <= ?",
        				arrayOf(fromPosition.toString(), toPosition.toString()))
                } else {
                    db.execSQL("UPDATE $TABLE_FAV_STOPS SET $KEY_POSITION = $KEY_POSITION + 1 WHERE ? <= $KEY_POSITION AND $KEY_POSITION < ?",
        				arrayOf(toPosition.toString(), fromPosition.toString()))
                }

                val cv = ContentValues().apply { put(KEY_POSITION, toPosition) }
                db.update(TABLE_FAV_STOPS, cv, "$KEY_STOP_ID == ?", arrayOf(stopId.toString()))
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }
        c.close()
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
                "$KEY_POSITION DESC", null)
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

    fun updateDoShowRoutes(stopId: Int, doShowRoutes: Map<Pair<String?, String?>, Boolean>) {
        val db = mDbHelper!!.mDb!!
        db.beginTransaction();
        try {
            db.delete(TABLE_FILTERS, "$KEY_STOP_ID == ?", arrayOf(stopId.toString()))
            for ((routeAndTerminal, doShow) in doShowRoutes) {
                val cv = ContentValues().apply {
                    put(KEY_STOP_ID, stopId)
                    put(KEY_ROUTE, routeAndTerminal.first)
                    put(KEY_TERMINAL, routeAndTerminal.second)
                    put(KEY_DO_SHOW, doShow)
                }
                db.insert(TABLE_FILTERS, null, cv)
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    fun getDoShowRoutes(stopId: Int): Map<Pair<String?, String?>, Boolean> {
        val doShowRoutes: MutableMap<Pair<String?, String?>, Boolean> = mutableMapOf()
        val c = mDbHelper!!.mDb!!.query(TABLE_FILTERS,
        	arrayOf(KEY_ROUTE, KEY_TERMINAL, KEY_DO_SHOW), "$KEY_STOP_ID == ?",
        	arrayOf(stopId.toString()), null, null, null, null)
        val routeIndex = c.getColumnIndex(KEY_ROUTE)
        val terminalIndex = c.getColumnIndex(KEY_TERMINAL)
        val doShowIndex = c.getColumnIndex(KEY_DO_SHOW)
        while (c.moveToNext()) {
            val route = c.getString(routeIndex)
            val terminal = c.getString(terminalIndex)
            val doShow = c.getInt(doShowIndex) != 0
            doShowRoutes[Pair(route, terminal)] = doShow
        }
        c.close()
        return doShowRoutes
    }

    fun updateStop(stop: Stop) {
        val cv = ContentValues().apply {
            put(KEY_STOP_ID, stop.stopId)
            put(KEY_STOP_NAME, stop.stopName)
            put(KEY_STOP_DESC, stop.stopDesc)
            put(KEY_STOP_LAT, stop.stopLat)
            put(KEY_STOP_LON, stop.stopLon)
            put(KEY_WHEELCHAIR_BOARDING, stop.wheelchairBoarding)
        }
        mDbHelper!!.mDb!!.insert(TABLE_STOPS, null, cv)
    }

    fun getStop(stopId: Int): Stop? {
        var stop: Stop? = null
        val c = mDbHelper!!.mDb!!.query(TABLE_STOPS,
        	arrayOf(KEY_STOP_NAME, KEY_STOP_DESC, KEY_STOP_LAT, KEY_STOP_LON, KEY_WHEELCHAIR_BOARDING),
	        "$KEY_STOP_ID == ?", arrayOf(stopId.toString()), null, null, null, "1")
        if (c.moveToNext()) {
            val stopName = c.getString(c.getColumnIndex(KEY_STOP_NAME))
            val stopDesc = c.getString(c.getColumnIndex(KEY_STOP_DESC))
            val stopLat = c.getDouble(c.getColumnIndex(KEY_STOP_LAT))
            val stopLon = c.getDouble(c.getColumnIndex(KEY_STOP_LON))
            val wheelchairBoarding = c.getInt(c.getColumnIndex(KEY_WHEELCHAIR_BOARDING))
            stop = Stop(stopId, stopName, stopDesc, stopLat, stopLon, wheelchairBoarding)
        }
        c.close()
        return stop
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
        private val KEY_DO_SHOW = "do_show"
        private val KEY_STOP_NAME = "stop_name"
        private val KEY_STOP_DESC = "stop_desc"
        private val KEY_STOP_LAT = "stop_lat"
        private val KEY_STOP_LON = "stop_lon"
        private val KEY_WHEELCHAIR_BOARDING = "wheelchair_boarding"

        private val KEY_POSITION = "position"
        private val KEY_LAST_UPDATE = "last_update"

        private val TABLE_FAV_STOPS = "fav_stops"
        private val TABLE_NEXTRIPS = "nextrips"
        private val TABLE_FILTERS = "filters"
        private val TABLE_STOPS = "stops"
        private val TABLE_LAST_UPDATE = "last_update"

        private val INDEX_NEXTRIPS = "index_nextrips"

        private val DATABASE_NAME = "buswhen.db"
        private val DATABASE_VERSION = 7

        private val DATABASE_CREATE_FAV_STOPS = """
	        CREATE TABLE $TABLE_FAV_STOPS (
                $KEY_STOP_ID INTEGER PRIMARY KEY,
                $KEY_STOP_DESCRIPTION TEXT,
                $KEY_POSITION INTEGER
            )
            """

        private val DATABASE_CREATE_FILTERS = """
	        CREATE TABLE $TABLE_FILTERS (
                $KEY_STOP_ID INTEGER NOT NULL,
                $KEY_ROUTE TEXT,
                $KEY_TERMINAL TEXT,
                $KEY_DO_SHOW BOOLEAN NOT NULL,
                FOREIGN KEY($KEY_STOP_ID) REFERENCES $TABLE_FAV_STOPS($KEY_STOP_ID),
                PRIMARY KEY($KEY_STOP_ID, $KEY_ROUTE, $KEY_TERMINAL)
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

        private val DATABASE_CREATE_STOPS = """
	        CREATE TABLE $TABLE_STOPS (
                $KEY_STOP_ID INTEGER PRIMARY KEY,
                $KEY_STOP_NAME TEXT NOT NULL,
                $KEY_STOP_DESC TEXT,
                $KEY_STOP_LAT DOUBLE NOT NULL,
                $KEY_STOP_LON DOUBLE NOT NULL,
                $KEY_WHEELCHAIR_BOARDING INTEGER,
                $KEY_LAST_UPDATE DATETIME
            )
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
