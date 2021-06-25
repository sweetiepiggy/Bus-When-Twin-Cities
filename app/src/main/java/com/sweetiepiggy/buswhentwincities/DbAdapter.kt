/*
    Copyright (C) 2019-2021 Sweetie Piggy Apps <sweetiepiggyapps@gmail.com>

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
import java.util.*
import kotlin.collections.ArrayList

class DbAdapter {

    private var mDbHelper: DatabaseHelper? = null

    private class DatabaseHelper internal constructor(private val mContext: Context) : SQLiteOpenHelper(mContext, DATABASE_NAME, null, DATABASE_VERSION) {
        var mDb: SQLiteDatabase? = null

        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(DATABASE_CREATE_FAV_STOPS)
            db.execSQL(DATABASE_CREATE_FAV_TIMESTOPS)
            db.execSQL(DATABASE_CREATE_FILTERS)
            db.execSQL(DATABASE_CREATE_TIMESTOP_FILTERS)
            db.execSQL(DATABASE_CREATE_NEXTRIPS)
            db.execSQL(DATABASE_CREATE_NEXTRIPS_INDEX)
            db.execSQL(DATABASE_CREATE_TIMESTOP_NEXTRIPS)
            db.execSQL(DATABASE_CREATE_TIMESTOP_NEXTRIPS_INDEX)
            db.execSQL(DATABASE_CREATE_STOPS)
            db.execSQL(DATABASE_CREATE_LAST_UPDATE)
            db.execSQL(DATABASE_CREATE_LAST_TIMESTOP_UPDATE)
            db.execSQL(DATABASE_CREATE_SHAPES)
            db.execSQL(DATABASE_CREATE_SHAPES_INDEX)
            db.execSQL(DATABASE_CREATE_STOP_SEARCH_HISTORY)
            db.execSQL(DATABASE_CREATE_VEHICLES)
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
            // create timestop tables, versionCode 52
            if (oldVer < 8) {
                db.execSQL("""
                    CREATE TABLE fav_timestops (
                        timestop_id TEXT NOT NULL,
                        route TEXT NOT NULL,
                        route_direction INTEGER NOT NULL,
                        stop_description TEXT,
                        position INTEGER,
                        PRIMARY KEY(timestop_id, route, route_direction)
                    )
                """)
                db.execSQL("""
                    CREATE TABLE last_timestop_update (
                        timestop_id TEXT NOT NULL,
                        route TEXT NOT NULL,
                        route_direction INTEGER NOT NULL,
                        last_update DATETIME,
                        PRIMARY KEY(timestop_id, route, route_direction)
                    )
                """)
                db.execSQL("""
                    CREATE TABLE timestop_nextrips (
                        timestop_id TEXT NOT NULL,
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
                    CREATE INDEX index_timestop_nextrips ON timestop_nextrips (timestop_id, route_direction)
                """)
                db.execSQL("""
                    CREATE TABLE timestop_filters (
                        timestop_id TEXT NOT NULL,
                        route TEXT,
                        terminal TEXT,
                        do_show BOOLEAN NOT NULL,
                        FOREIGN KEY(timestop_id) REFERENCES fav_timestops(timestop_id),
                        PRIMARY KEY(timestop_id, route, terminal)
                    )
                """)
            }
            // add route_direction to timestop_filters table, versionCode 52
            if (oldVer < 9) {
                db.execSQL("DROP TABLE timestop_filters")
                db.execSQL("""
                    CREATE TABLE timestop_filters (
                        timestop_id TEXT NOT NULL,
                        route TEXT NOT NULL,
                        route_direction INTEGER NOT NULL,
                        terminal TEXT,
                        do_show BOOLEAN NOT NULL,
                        FOREIGN KEY(timestop_id) REFERENCES fav_timestops(timestop_id),
                        PRIMARY KEY(timestop_id, route, route_direction, terminal)
                    )
                """)
            }
            // create gtfs shapes table, versionCode 62
            if (oldVer < 10) {
                db.execSQL("""
                    CREATE TABLE shapes (
                        shape_id INTEGER NOT NULL,
                        shape_pt_lat DOUBLE NOT NULL,
                        shape_pt_lon DOUBLE NOT NULL,
                        shape_pt_sequence INTEGER NOT NULL
                    )
                """)
                db.execSQL("CREATE INDEX index_shapes ON shapes (shape_id)")
                db.execSQL("ALTER TABLE nextrips ADD COLUMN shape_id INTEGER")
                db.execSQL("ALTER TABLE timestop_nextrips ADD COLUMN shape_id INTEGER")
            }
            // create stop search history table, versionCode 70
            if (oldVer < 11) {
                db.execSQL("""
                    CREATE TABLE stop_search_history (
                        stop_id INTEGER PRIMARY KEY,
                        stop_search_datetime DATETIME NOT NULL,
                        FOREIGN KEY(stop_id) REFERENCES stops(stop_id)
                    )
                """)
            }
            // MetroTransit NexTripV2, versionCode 74
            if (oldVer < 12) {
                val mapRouteDirectionToDirectionId = { routeDirection: Int ->
                    when(routeDirection) {
                        1 -> 1 // South
                        2 -> 0 // East
                        3 -> 1 // West
                        4 -> 0 // North
                        else -> 0
                    }
                }

                db.beginTransaction();
                try {
                    // fav_timestops: route ->route_id,
                    //                route_direction -> direction_id
                    db.execSQL("ALTER TABLE fav_timestops RENAME COLUMN route TO route_id")
                    db.execSQL("ALTER TABLE fav_timestops RENAME COLUMN route_direction TO direction_id")
                    val c =  db.query("fav_timestops",
                        arrayOf("direction_id"),
                        null, null, null, null, null, null)
                    val directionIdIndex = c.getColumnIndex("direction_id")
                    while (c.moveToNext()) {
                        val directionId = c.getInt(directionIdIndex)
                        val cv = ContentValues().apply {
                            put("direction_id", mapRouteDirectionToDirectionId(directionId))
                        }
                        db.update("new_fav_stops", cv, null, null)
                    }

                    // last_timestop_update: route ->route_id,
                    //                       route_direction -> direction_id
                    //   just drop/recreate
                    db.execSQL("DROP TABLE last_timestop_update")
                    db.execSQL("""
                        CREATE TABLE last_timestop_update (
                            timestop_id TEXT NOT NULL,
                            route_id TEXT NOT NULL,
                            direction_id INTEGER NOT NULL,
                            last_update DATETIME,
                            PRIMARY KEY(timestop_id, route_id, direction_id)
                        )
                    """)
                    // timestop_nextrips: just drop/recreate
                    db.execSQL("DROP TABLE timestop_nextrips")
                    db.execSQL("""
                        CREATE TABLE timestop_nextrips (
                            timestop_id TEXT NOT NULL,
                            is_actual BOOLEAN NOT NULL,
                            trip_id TEXT,
                            departure_time DATETIME NOT NULL,
                            description TEXT,
                            route_id TEXT,
                            route_short_name TEXT,
                            direction_enum INTEGER,
                            terminal TEXT,
                            schedule_relationship TEXT,
                            FOREIGN KEY (trip_id) REFERENCES table_vehicles(trip_id)
                        )
                    """)
                    db.execSQL("""
                        CREATE INDEX index_timestop_nextrips ON timestop_nextrips (timestop_id, direction_enum)
                    """)
                    // timestop_filters: route ->route_id,
                    //                   route_direction -> direction_id
                    //   just drop/recreate
                    db.execSQL("DROP TABLE timestop_filters")
                    db.execSQL("""
                        CREATE TABLE timestop_filters (
                            timestop_id TEXT NOT NULL,
                            route_id TEXT NOT NULL,
                            direction_id INTEGER NOT NULL,
                            terminal TEXT,
                            do_show BOOLEAN NOT NULL,
                            FOREIGN KEY(timestop_id) REFERENCES fav_timestops(timestop_id),
                            PRIMARY KEY(timestop_id, route_id, direction_id, terminal)
                        )
                    """)

                    // nextrips: just drop/recreate
                    db.execSQL("DROP TABLE nextrips")
                    db.execSQL("""
                        CREATE TABLE nextrips (
                            stop_id INTEGER NOT NULL,
                            is_actual BOOLEAN NOT NULL,
                            trip_id TEXT,
                            departure_time DATETIME NOT NULL,
                            description TEXT,
                            route_id TEXT,
                            route_short_name TEXT,
                            direction_enum INTEGER,
                            terminal TEXT,
                            schedule_relationship TEXT,
                            FOREIGN KEY (trip_id) REFERENCES table_vehicles(trip_id)
                        )
                    """)
                    db.execSQL("""
                        CREATE INDEX index_nextrips ON nextrips (stop_id)
                    """)

                    // filters: route -> route_id
                    db.execSQL("ALTER TABLE filters RENAME COLUMN route TO route_id")

                    // stops: removed unused KEY_LAST_UPDATE
                    db.execSQL("ALTER TABLE stops DROP COLUMN last_update")

                    // vehicles table: create
                    db.execSQL("""
                        CREATE TABLE vehicles (
                            trip_id TEXT PRIMARY KEY,
                            direction_enum INTEGER,
                            location_time DATETIME NOT NULL,
                            route_id TEXT,
                            terminal TEXT,
                            latitude DOUBLE,
                            longitude DOUBLE,
                            bearing DOUBLE,
                            odometer DOUBLE,
                            speed DOUBLE,
                            shape_id INTEGER,
                            FOREIGN KEY (shape_id) REFERENCES shapes(shape_id)
                        )
                    """)
                    // fav_stops: no change
                    // last_update: no change
                    // shapes: no change
                    // stop_search_history: no change
                } finally {
                    db.endTransaction();
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
    fun createFavStop(stopId: Int, stopDescription: String?): Long {
        val cv = ContentValues().apply {
            put(KEY_STOP_ID, stopId)
            put(KEY_STOP_DESCRIPTION, stopDescription)
            put(KEY_POSITION, getNewFavPosition())
        }

        return mDbHelper!!.mDb!!.insert(TABLE_FAV_STOPS, null, cv)
    }

    /** @return number of rows affected */
    fun updateFavStop(stopId: Int, stopDescription: String?): Int {
        val cv = ContentValues().apply {
            put(KEY_STOP_DESCRIPTION, stopDescription)
        }

        return mDbHelper!!.mDb!!.update(TABLE_FAV_STOPS, cv, "$KEY_STOP_ID == ?",
                                        arrayOf(stopId.toString()))
    }

    private fun getNewFavPosition(): Int =
        maxOf(getNewFavPosition(TABLE_FAV_STOPS), getNewFavPosition(TABLE_FAV_TIMESTOPS))

    private fun getNewFavPosition(table: String): Int {
        val c = mDbHelper!!.mDb!!.query(table, arrayOf("COALESCE(MAX($KEY_POSITION) + 1, 0)"),
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
            for (table in listOf(TABLE_FAV_STOPS, TABLE_FAV_TIMESTOPS)) {
                db.delete(table, "$KEY_POSITION == ?", arrayOf(position.toString()))
                db.execSQL("UPDATE $table SET $KEY_POSITION = $KEY_POSITION - 1 WHERE $KEY_POSITION > ?",
                arrayOf(position.toString()))
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    fun moveFavStop(fromPosition: Int, toPosition: Int) {
        val db = mDbHelper!!.mDb!!
        // lookup the row with position equal to fromPosition
        val c0 = db.query(TABLE_FAV_STOPS, arrayOf(KEY_STOP_ID), "$KEY_POSITION == ?",
            arrayOf(fromPosition.toString()), null, null, null, "1")
        val c1 = db.query(TABLE_FAV_TIMESTOPS, arrayOf(KEY_TIMESTOP_ID), "$KEY_POSITION == ?",
            arrayOf(fromPosition.toString()), null, null, null, "1")

        val foundFavStop = c0.moveToFirst()
        val foundFavTimestop = c1.moveToFirst()

        if (foundFavStop || foundFavTimestop) {
            db.beginTransaction();
            try {
                // if we will to increase the position of the row to be moved, then
                // decrement the position of all greater rows up until the
                // position to be moved to
                if (fromPosition < toPosition){
                    for (table in listOf(TABLE_FAV_STOPS, TABLE_FAV_TIMESTOPS)) {
                        db.execSQL("UPDATE $table SET $KEY_POSITION = $KEY_POSITION - 1 WHERE ? < $KEY_POSITION AND $KEY_POSITION <= ?",
                            arrayOf(fromPosition.toString(), toPosition.toString()))
                    }
                // if we will to decrease the position of the row moved, then
                // increment the position of all lesser rows down until the
                // position to be moved to
                } else {
                    for (table in listOf(TABLE_FAV_STOPS, TABLE_FAV_TIMESTOPS)) {
                        db.execSQL("UPDATE $table SET $KEY_POSITION = $KEY_POSITION + 1 WHERE ? <= $KEY_POSITION AND $KEY_POSITION < ?",
                            arrayOf(toPosition.toString(), fromPosition.toString()))
                    }
                }

                // update the position of the row to be moved
                val cv = ContentValues().apply { put(KEY_POSITION, toPosition) }
                if (foundFavStop) {
                    val stopId = c0.getInt(c0.getColumnIndex(KEY_STOP_ID))
                    db.update(TABLE_FAV_STOPS, cv, "$KEY_STOP_ID == ?", arrayOf(stopId.toString()))
                } else {
                    val timestopId = c1.getString(c1.getColumnIndex(KEY_TIMESTOP_ID))
                    db.update(TABLE_FAV_TIMESTOPS, cv, "$KEY_TIMESTOP_ID == ?", arrayOf(timestopId))
                }
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }
        c0.close()
        c1.close()
    }

    fun isFavStop(stopId: Int): Boolean {
        val c = mDbHelper!!.mDb!!.query(TABLE_FAV_STOPS, arrayOf(KEY_STOP_ID),
                "$KEY_STOP_ID == ?", arrayOf(stopId.toString()), null, null, null, "1")

        val found = c.moveToFirst()
        c.close()
        return found
    }

    /** @return rowId or -1 if failed */
    fun createFavTimestop(timestopId: String, routeId: String, directionId: Int,
                          stopDescription: String?): Long {
        val cv = ContentValues().apply {
            put(KEY_TIMESTOP_ID, timestopId)
            put(KEY_ROUTE_ID, routeId)
            put(KEY_DIRECTION_ID, directionId)
            put(KEY_STOP_DESCRIPTION, stopDescription)
            put(KEY_POSITION, getNewFavPosition())
        }

        return mDbHelper!!.mDb!!.insert(TABLE_FAV_TIMESTOPS, null, cv)
    }

    /** @return number of rows affected */
    fun updateFavTimestop(timestopId: String, stopDescription: String?): Int {
        val cv = ContentValues().apply {
            put(KEY_STOP_DESCRIPTION, stopDescription)
        }

        return mDbHelper!!.mDb!!.update(TABLE_FAV_TIMESTOPS, cv, "$KEY_TIMESTOP_ID == ?", arrayOf(timestopId))
    }

    fun deleteFavTimestop(timestopId: String, routeId: String, directionId: Int) {
        val db = mDbHelper!!.mDb!!
        db.delete(TABLE_TIMESTOP_FILTERS, "$KEY_TIMESTOP_ID == ?", arrayOf(timestopId))
        val c = db.query(TABLE_FAV_TIMESTOPS, arrayOf(KEY_POSITION),
                "$KEY_TIMESTOP_ID == ? AND $KEY_ROUTE_ID == ? AND $KEY_DIRECTION_ID == ?",
                arrayOf(timestopId, routeId, directionId.toString()), null, null, null, "1")
        if (c.moveToFirst()) {
            deleteFavStopAtPosition(c.getInt(c.getColumnIndex(KEY_POSITION)))
        }
        c.close()
    }

    fun isFavTimestop(timestopId: String, routeId: String, directionId: Int): Boolean {
        val c = mDbHelper!!.mDb!!.query(TABLE_FAV_TIMESTOPS, arrayOf(KEY_TIMESTOP_ID),
                "$KEY_TIMESTOP_ID == ? AND $KEY_ROUTE_ID == ? AND $KEY_DIRECTION_ID == ?",
                arrayOf(timestopId, routeId, directionId.toString()), null, null, null, "1")

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

    fun getTimestopDesc(timestopId: String, routeId: String, directionId: Int): String? {
        val c = mDbHelper!!.mDb!!.query(TABLE_FAV_TIMESTOPS, arrayOf(KEY_STOP_DESCRIPTION),
            "$KEY_TIMESTOP_ID == ? AND $KEY_ROUTE_ID == ? AND $KEY_DIRECTION_ID == ?",
            arrayOf(timestopId.toString(), routeId, directionId.toString()), null, null, null, "1")
        val ret = if (c.moveToFirst()) c.getString(c.getColumnIndex(KEY_STOP_DESCRIPTION)) else null
        c.close()
        return ret
    }

    fun fetchFavStops(): Cursor {
        return mDbHelper!!.mDb!!.query(TABLE_FAV_STOPS, null, null, null, null, null,
                "$KEY_POSITION DESC", null)
    }

    fun fetchFavTimestops(): Cursor {
        return mDbHelper!!.mDb!!.query(TABLE_FAV_TIMESTOPS, null, null, null, null, null,
                "$KEY_POSITION DESC", null)
    }

    fun hasAnyFavorites(): Boolean {
        val cStop = mDbHelper!!.mDb!!.query(TABLE_FAV_STOPS, null, null, null, null, null, null, "1")
        val foundFavStop = cStop.moveToFirst()
        cStop.close()

        val cTimestop = mDbHelper!!.mDb!!.query(TABLE_FAV_TIMESTOPS, null, null, null, null, null, null, "1")
        val foundFavTimestop = cTimestop.moveToFirst()
        cTimestop.close()

        return foundFavStop || foundFavTimestop
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

    fun getLastTimestopUpdate(timestopId: String, routeId: String, directionId: Int): Long? {
        val c = mDbHelper!!.mDb!!.query(TABLE_LAST_TIMESTOP_UPDATE, arrayOf(KEY_LAST_UPDATE),
            "$KEY_TIMESTOP_ID == ? AND $KEY_ROUTE_ID == ? AND $KEY_DIRECTION_ID == ?",
            arrayOf(timestopId, routeId, directionId.toString()), null, null, null, "1");
        val ret = if (c.moveToFirst()) c.getLong(c.getColumnIndex(KEY_LAST_UPDATE)) else null
        c.close()
        return ret
    }

    private fun setTimestopLastUpdate(timestopId: String, routeId: String,
                                      directionId: Int, lastUpdate: Long) {
        val cv = ContentValues().apply {
            put(KEY_TIMESTOP_ID, timestopId)
            put(KEY_ROUTE_ID, routeId)
            put(KEY_DIRECTION_ID, directionId)
            put(KEY_LAST_UPDATE, lastUpdate)
        }

        mDbHelper!!.mDb!!.replace(TABLE_LAST_TIMESTOP_UPDATE, null, cv)
    }

    fun updateNexTrips(stopId: Int, nexTrips: List<NexTrip>, lastUpdate: Long) {
        val db = mDbHelper!!.mDb!!
        db.beginTransaction();
        try {
            db.delete(TABLE_NEXTRIPS, "$KEY_STOP_ID == ?", arrayOf(stopId.toString()))
            val stmt = db.compileStatement("""
                INSERT INTO $TABLE_NEXTRIPS
                    ($KEY_STOP_ID, $KEY_IS_ACTUAL, $KEY_TRIP_ID, $KEY_DEPARTURE_UNIX_TIME,
                     $KEY_DESCRIPTION, $KEY_ROUTE_ID, $KEY_ROUTE_SHORT_NAME,
                     $KEY_DIRECTION_ENUM, $KEY_TERMINAL, $KEY_SCHEDULE_RELATIONSHIP)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """)
            for (nexTrip in nexTrips.filter { it.departureTime != null }) {
                stmt.bindAllArgsAsStrings(arrayOf(
                    stopId.toString(), nexTrip.isActual.toString(), nexTrip.tripId ?: "",
                    nexTrip.departureTime!!.toString(), nexTrip.description ?: "",
                    nexTrip.routeId ?: "", nexTrip.routeShortName ?: "",
                    directionToInt(nexTrip.routeDirection)?.toString() ?: "null",
                    nexTrip.terminal ?: "", nexTrip.scheduleRelationship ?: ""
                ))
                stmt.execute()
                stmt.clearBindings()
            }
            setLastUpdate(stopId, lastUpdate)
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    fun updateTimestopNexTrips(timestopId: String, routeId: String,
                               routeDirectionId: Int, nexTrips: List<NexTrip>,
                               lastUpdate: Long) {
        val db = mDbHelper!!.mDb!!
        db.beginTransaction();
        try {
            // db.delete(TABLE_TIMESTOP_NEXTRIPS,
            //           "$KEY_TIMESTOP_ID == ? AND $KEY_ROUTE_ID == ? AND $KEY_DIRECTION_ID == ?",
            //           arrayOf(timestopId, routeId, routeDirectionId.toString()))
            db.delete(TABLE_TIMESTOP_NEXTRIPS,
                      "$KEY_TIMESTOP_ID == ? AND $KEY_ROUTE_ID == ?",
                      arrayOf(timestopId, routeId))

            val stmt = db.compileStatement("""
                INSERT INTO $TABLE_TIMESTOP_NEXTRIPS
                    ($KEY_TIMESTOP_ID, $KEY_IS_ACTUAL, $KEY_TRIP_ID, $KEY_DEPARTURE_UNIX_TIME,
                     $KEY_DESCRIPTION, $KEY_ROUTE_ID, $KEY_ROUTE_SHORT_NAME,
                     $KEY_DIRECTION_ENUM, $KEY_TERMINAL, $KEY_SCHEDULE_RELATIONSHIP)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """)
            for (nexTrip in nexTrips.filter { it.departureTime != null }) {
                stmt.bindAllArgsAsStrings(arrayOf(
                    timestopId.toString(), nexTrip.isActual.toString(), nexTrip.tripId ?: "",
                    nexTrip.departureTime!!.toString(), nexTrip.description ?: "",
                    nexTrip.routeId ?: "", nexTrip.routeShortName ?: "",
                    directionToInt(nexTrip.routeDirection)?.toString() ?: "null",
                    nexTrip.terminal ?: "", nexTrip.scheduleRelationship
                ))
                stmt.execute()
                stmt.clearBindings()
            }
            setTimestopLastUpdate(timestopId, routeId, routeDirectionId, lastUpdate)
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    fun getNexTrips(stopId: Int, secondsBeforeNowToIgnore: Int,
                    suppressLocations: Boolean): List<NexTrip> {
        val nexTrips: MutableList<NexTrip> = mutableListOf()
        val c = mDbHelper!!.mDb!!.query(TABLE_NEXTRIPS,
            arrayOf(KEY_IS_ACTUAL, KEY_TRIP_ID, KEY_DEPARTURE_UNIX_TIME, KEY_DESCRIPTION,
                    KEY_ROUTE_ID, KEY_ROUTE_SHORT_NAME, KEY_DIRECTION_ENUM,
                    KEY_TERMINAL, KEY_SCHEDULE_RELATIONSHIP),
            "$KEY_STOP_ID == ? AND $KEY_DEPARTURE_UNIX_TIME >= strftime(\"%s\", 'now') - ?",
            arrayOf(stopId.toString(), secondsBeforeNowToIgnore.toString()), null, null,
            "$KEY_DEPARTURE_UNIX_TIME ASC", null)
        val isActualIndex = c.getColumnIndex(KEY_IS_ACTUAL)
        val tripIdIndex = c.getColumnIndex(KEY_TRIP_ID)
        val departureUnixTimeIndex = c.getColumnIndex(KEY_DEPARTURE_UNIX_TIME)
        val descriptionIndex = c.getColumnIndex(KEY_DESCRIPTION)
        val routeIdIndex = c.getColumnIndex(KEY_ROUTE_ID)
        val routeShortNameIndex = c.getColumnIndex(KEY_ROUTE_SHORT_NAME)
        val directionEnumIndex = c.getColumnIndex(KEY_DIRECTION_ENUM)
        val terminalIndex = c.getColumnIndex(KEY_TERMINAL)
        val scheduleRelationshipIndex = c.getColumnIndex(KEY_SCHEDULE_RELATIONSHIP)
        while (c.moveToNext()) {
            val isActual = c.getInt(isActualIndex) != 0
            val tripId = c.getString(tripIdIndex)
            val departureTime = c.getLong(departureUnixTimeIndex)
            val description = c.getString(descriptionIndex)
            val routeId = c.getString(routeIdIndex)
            val routeShortName = c.getString(routeShortNameIndex)
            val routeDirection = NexTrip.Direction.from(c.getInt(directionEnumIndex))
            val terminal = c.getString(terminalIndex)
            val scheduleRelationship = c.getString(scheduleRelationshipIndex)
            val vehicleHeading: Double? = null
            val vehicleLatitude: Double? = null
            val vehicleLongitude: Double? = null
            val rawShapeId: Int? = 0
            val shapeId = if (rawShapeId == 0) null else rawShapeId
            nexTrips.add(
                NexTrip(isActual, tripId, departureTime, description,
                        routeId, routeShortName, routeDirection,
                        terminal, scheduleRelationship, vehicleHeading,
                        vehicleLatitude, vehicleLongitude, shapeId).let {
                    if (suppressLocations) NexTrip.suppressLocation(it) else it
                }
            )
        }
        c.close()
        return nexTrips
    }

    fun getTimestopNexTrips(timestopId: String, routeId: String,
                            routeDirectionId: Int, secondsBeforeNowToIgnore: Int,
                            suppressLocations: Boolean): List<NexTrip> {
        val nexTrips: MutableList<NexTrip> = mutableListOf()
        val c = mDbHelper!!.mDb!!.query(TABLE_TIMESTOP_NEXTRIPS,
            arrayOf(KEY_IS_ACTUAL, KEY_TRIP_ID, KEY_DEPARTURE_UNIX_TIME, KEY_DESCRIPTION,
                KEY_ROUTE_ID, KEY_ROUTE_SHORT_NAME, KEY_DIRECTION_ENUM,
                KEY_TERMINAL, KEY_SCHEDULE_RELATIONSHIP),
            "$KEY_TIMESTOP_ID == ? AND $KEY_ROUTE_ID == ? AND $KEY_DIRECTION_ENUM == ? AND $KEY_DEPARTURE_UNIX_TIME >= strftime(\"%s\", 'now') - ?",
            arrayOf(timestopId, routeDirectionId.toString(), secondsBeforeNowToIgnore.toString()), null, null,
            "$KEY_DEPARTURE_UNIX_TIME ASC", null)
        val isActualIndex = c.getColumnIndex(KEY_IS_ACTUAL)
        val tripIdIndex = c.getColumnIndex(KEY_TRIP_ID)
        val departureUnixTimeIndex = c.getColumnIndex(KEY_DEPARTURE_UNIX_TIME)
        val descriptionIndex = c.getColumnIndex(KEY_DESCRIPTION)
        val routeIdIndex = c.getColumnIndex(KEY_ROUTE_ID)
        val routeShortNameIndex = c.getColumnIndex(KEY_ROUTE_SHORT_NAME)
        val directionEnumIndex = c.getColumnIndex(KEY_DIRECTION_ENUM)
        val terminalIndex = c.getColumnIndex(KEY_TERMINAL)
        val scheduleRelationshipIndex = c.getColumnIndex(KEY_SCHEDULE_RELATIONSHIP)
        while (c.moveToNext()) {
            val isActual = c.getInt(isActualIndex) != 0
            val tripId = c.getString(tripIdIndex)
            val departureTime = c.getLong(departureUnixTimeIndex)
            val description = c.getString(descriptionIndex)
            val routeId = c.getString(routeIdIndex)
            val routeShortName = c.getString(routeShortNameIndex)
            val routeDirection = NexTrip.Direction.from(c.getInt(directionEnumIndex))
            val terminal = c.getString(terminalIndex)
            val scheduleRelationship = c.getString(scheduleRelationshipIndex)
            val vehicleHeading : Double? = null
            val vehicleLatitude : Double? = null
            val vehicleLongitude : Double? = null
            val rawShapeId = 0
            val shapeId = if (rawShapeId == 0) null else rawShapeId
            nexTrips.add(
                NexTrip(isActual, tripId, departureTime, description,
                        routeId, routeShortName, routeDirection,
                        terminal, scheduleRelationship, vehicleHeading,
                        vehicleLatitude, vehicleLongitude, shapeId).let {
                    if (suppressLocations) NexTrip.suppressLocation(it) else it
                }
            )
        }
        c.close()
        return nexTrips
    }

    fun updateDoShowRoutes(stopId: Int, doShowRoutes: Map<Pair<String?, String?>,
                           Boolean>) {
        val db = mDbHelper!!.mDb!!
        db.beginTransaction();
        try {
            db.delete(TABLE_FILTERS, "$KEY_STOP_ID == ?", arrayOf(stopId.toString()))
            for ((routeIdAndTerminal, doShow) in doShowRoutes) {
                val cv = ContentValues().apply {
                    put(KEY_STOP_ID, stopId)
                    put(KEY_ROUTE_ID, routeIdAndTerminal.first)
                    put(KEY_TERMINAL, routeIdAndTerminal.second)
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
            arrayOf(KEY_ROUTE_ID, KEY_TERMINAL, KEY_DO_SHOW), "$KEY_STOP_ID == ?",
            arrayOf(stopId.toString()), null, null, null, null)
        val routeIdIndex = c.getColumnIndex(KEY_ROUTE_ID)
        val terminalIndex = c.getColumnIndex(KEY_TERMINAL)
        val doShowIndex = c.getColumnIndex(KEY_DO_SHOW)
        while (c.moveToNext()) {
            val routeId = c.getString(routeIdIndex)
            val terminal = c.getString(terminalIndex)
            val doShow = c.getInt(doShowIndex) != 0
            doShowRoutes[Pair(routeId, terminal)] = doShow
        }
        c.close()
        return doShowRoutes
    }

    fun getTimestopDoShowRoutes(timestopId: String, routeId: String,
                                directionId: Int): Map<Pair<String?, String?>, Boolean> {
        val doShowRoutes: MutableMap<Pair<String?, String?>, Boolean> = mutableMapOf()
        val c = mDbHelper!!.mDb!!.query(TABLE_TIMESTOP_FILTERS,
            arrayOf(KEY_ROUTE_ID, KEY_TERMINAL, KEY_DO_SHOW), "$KEY_TIMESTOP_ID == ? AND $KEY_ROUTE_ID == ? AND $KEY_DIRECTION_ID == ?",
            arrayOf(timestopId, routeId, directionId.toString()), null, null, null, null)
        val routeIdIndex = c.getColumnIndex(KEY_ROUTE_ID)
        val terminalIndex = c.getColumnIndex(KEY_TERMINAL)
        val doShowIndex = c.getColumnIndex(KEY_DO_SHOW)
        while (c.moveToNext()) {
            val routeId = c.getString(routeIdIndex)
            val terminal = c.getString(terminalIndex)
            val doShow = c.getInt(doShowIndex) != 0
            doShowRoutes[Pair(routeId, terminal)] = doShow
        }
        c.close()
        return doShowRoutes
    }

    fun updateTimestopDoShowRoutes(timestopId: String, routeId: String,
                                   directionId: Int,
                                   doShowRoutes: Map<Pair<String?, String?>, Boolean>) {
        val db = mDbHelper!!.mDb!!
        db.beginTransaction();
        try {
            db.delete(TABLE_TIMESTOP_FILTERS, "$KEY_TIMESTOP_ID == ? AND $KEY_ROUTE_ID == ? AND $KEY_DIRECTION_ID == ?", arrayOf(timestopId, routeId, directionId.toString()))
            for ((routeIdAndTerminal, doShow) in doShowRoutes) {
                val cv = ContentValues().apply {
                    put(KEY_TIMESTOP_ID, timestopId)
                    put(KEY_ROUTE_ID, routeIdAndTerminal.first)
                    put(KEY_TERMINAL, routeIdAndTerminal.second)
                    put(KEY_DO_SHOW, doShow)
                }
                db.insert(TABLE_FILTERS, null, cv)
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    fun updateStop(stop: Stop) {
        val cv = ContentValues().apply {
            put(KEY_STOP_ID, stop.stopId)
            put(KEY_STOP_NAME, stop.stopName)
            put(KEY_STOP_DESC, stop.stopDesc)
            put(KEY_STOP_LAT, stop.stopLat)
            put(KEY_STOP_LON, stop.stopLon)
//            put(KEY_WHEELCHAIR_BOARDING, stop.wheelchairBoarding)
        }
        mDbHelper!!.mDb!!.insert(TABLE_STOPS, null, cv)
    }

    fun getStop(stopId: Int): Stop? {
        var stop: Stop? = null
        val c = mDbHelper!!.mDb!!.query(TABLE_STOPS,
            arrayOf(KEY_STOP_NAME, KEY_STOP_DESC, KEY_STOP_LAT, KEY_STOP_LON,
                    KEY_WHEELCHAIR_BOARDING),
            "$KEY_STOP_ID == ?", arrayOf(stopId.toString()), null, null, null, "1")
        if (c.moveToNext()) {
            val stopName = c.getString(c.getColumnIndex(KEY_STOP_NAME))
            val stopDesc = c.getString(c.getColumnIndex(KEY_STOP_DESC))
            val stopLat = c.getDouble(c.getColumnIndex(KEY_STOP_LAT))
            val stopLon = c.getDouble(c.getColumnIndex(KEY_STOP_LON))
//            val wheelchairBoarding = c.getInt(c.getColumnIndex(KEY_WHEELCHAIR_BOARDING))
//            stop = Stop(stopId, stopName, stopDesc, stopLat, stopLon, wheelchairBoarding)
            stop = Stop(stopId, stopName, stopLat, stopLon, stopDesc)
        }
        c.close()
        return stop
    }

    fun getShape(shapeId: Int): List<LatLng> {
        val ret = ArrayList<LatLng>()
        val c = mDbHelper!!.mDb!!.query(TABLE_SHAPES,
            arrayOf(KEY_SHAPE_PT_LAT, KEY_SHAPE_PT_LON),
            "$KEY_SHAPE_ID == ?", arrayOf(shapeId.toString()), null, null,
            "$KEY_SHAPE_PT_SEQUENCE ASC", null)
        val latIdx = c.getColumnIndex(KEY_SHAPE_PT_LAT)
        val lonIdx = c.getColumnIndex(KEY_SHAPE_PT_LON)
        while (c.moveToNext()) {
            ret.add(LatLng(c.getDouble(latIdx), c.getDouble(lonIdx)))
        }
        c.close()
        return ret
    }

    fun replaceShape(shapeId: Int, shape: List<Pair<Int, LatLng>>) {
        val db = mDbHelper!!.mDb!!
        db.beginTransaction();
        try {
            val stmt = db.compileStatement("""
                REPLACE INTO $TABLE_SHAPES
                    ($KEY_SHAPE_ID, $KEY_SHAPE_PT_LAT, $KEY_SHAPE_PT_LON, $KEY_SHAPE_PT_SEQUENCE)
                    VALUES (?, ?, ?, ?)
                """)
            for (shapeSegment in shape) {
                stmt.run {
                    bindLong(1, shapeId.toLong())
                    bindDouble(2, shapeSegment.second.latitude)
                    bindDouble(3, shapeSegment.second.longitude)
                    bindLong(4, shapeSegment.first.toLong())
                    executeInsert()
                    clearBindings()
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    fun updateStopSearchHistory(stopId: Int) {
        val cv = ContentValues().apply {
            put(KEY_STOP_SEARCH_ID, stopId)
            put(KEY_STOP_SEARCH_DATETIME, unixTime)
        }

        mDbHelper!!.mDb!!.replace(TABLE_STOP_SEARCH_HISTORY, null, cv)
    }

    fun fetchStopSearchHistory(): Cursor {
        return mDbHelper!!.mDb!!.query(TABLE_STOP_SEARCH_HISTORY, null, null,
                null, null, null, "$KEY_STOP_SEARCH_DATETIME DESC", null)
    }

    fun deleteStopSearchHistory(stopId: Int) {
        mDbHelper!!.mDb!!.delete(TABLE_STOP_SEARCH_HISTORY, "$KEY_STOP_SEARCH_ID == ?",
            arrayOf(stopId.toString()))
    }

    fun clearStopSearchHistory() {
        mDbHelper!!.mDb!!.delete(TABLE_STOP_SEARCH_HISTORY, null, arrayOf())
    }

    companion object {
        val KEY_STOP_ID = "stop_id"
        val KEY_TIMESTOP_ID = "timestop_id"
        val KEY_STOP_DESCRIPTION = "stop_description"
        val KEY_ROUTE_ID = "route_id"
        val KEY_ROUTE_SHORT_NAME = "route_short_name"
        val KEY_POSITION = "position"
        val KEY_STOP_SEARCH_ID = "stop_id"
        val KEY_STOP_SEARCH_DATETIME = "stop_search_datetime"
        val KEY_DIRECTION_ID = "direction_id"
        val KEY_DIRECTION_ENUM = "direction_enum"

        private val KEY_IS_ACTUAL = "is_actual"
        private val KEY_TRIP_ID = "trip_id"
        private val KEY_DEPARTURE_UNIX_TIME = "departure_time"
        private val KEY_DESCRIPTION = "description"
        private val KEY_TERMINAL = "terminal"
        private val KEY_VEHICLE_BEARING = "bearing"
        private val KEY_VEHICLE_LATITUDE = "latitude"
        private val KEY_VEHICLE_LONGITUDE = "longitude"
        private val KEY_VEHICLE_ODOMETER = "odometer"
        private val KEY_VEHICLE_SPEED = "speed"
        private val KEY_DO_SHOW = "do_show"
        private val KEY_STOP_NAME = "stop_name"
        private val KEY_STOP_DESC = "stop_desc"
        private val KEY_STOP_LAT = "stop_lat"
        private val KEY_STOP_LON = "stop_lon"
        private val KEY_WHEELCHAIR_BOARDING = "wheelchair_boarding"
        private val KEY_SHAPE_ID = "shape_id"
        private val KEY_SHAPE_PT_LAT = "shape_pt_lat"
        private val KEY_SHAPE_PT_LON = "shape_pt_lon"
        private val KEY_SHAPE_PT_SEQUENCE = "shape_pt_sequence"
        private val KEY_LOCATION_TIME = "location_time"
        private val KEY_SCHEDULE_RELATIONSHIP = "schedule_relationship"

        private val KEY_LAST_UPDATE = "last_update"

        private val TABLE_FAV_STOPS = "fav_stops"
        private val TABLE_FAV_TIMESTOPS = "fav_timestops"
        private val TABLE_NEXTRIPS = "nextrips"
        private val TABLE_TIMESTOP_NEXTRIPS = "timestop_nextrips"
        private val TABLE_FILTERS = "filters"
        private val TABLE_TIMESTOP_FILTERS = "timestop_filters"
        private val TABLE_STOPS = "stops"
        private val TABLE_LAST_UPDATE = "last_update"
        private val TABLE_LAST_TIMESTOP_UPDATE = "last_timestop_update"
        private val TABLE_SHAPES = "shapes"
        private val TABLE_STOP_SEARCH_HISTORY = "stop_search_history"
        private val TABLE_VEHICLES = "vehicles"

        private val INDEX_NEXTRIPS = "index_nextrips"
        private val INDEX_TIMESTOP_NEXTRIPS = "index_timestop_nextrips"
        private val INDEX_SHAPES = "index_shapes"

        private val DATABASE_NAME = "buswhen.db"
        private val DATABASE_VERSION = 12

        private val DATABASE_CREATE_FAV_STOPS = """
            CREATE TABLE $TABLE_FAV_STOPS (
                $KEY_STOP_ID INTEGER PRIMARY KEY,
                $KEY_STOP_DESCRIPTION TEXT,
                $KEY_POSITION INTEGER
            )
            """

        private val DATABASE_CREATE_FAV_TIMESTOPS = """
            CREATE TABLE $TABLE_FAV_TIMESTOPS (
                $KEY_TIMESTOP_ID TEXT NOT NULL,
                $KEY_ROUTE_ID TEXT NOT NULL,
                $KEY_DIRECTION_ID INTEGER NOT NULL,
                $KEY_STOP_DESCRIPTION TEXT,
                $KEY_POSITION INTEGER,
                PRIMARY KEY($KEY_TIMESTOP_ID, $KEY_ROUTE_ID, $KEY_DIRECTION_ID)
            )
            """

        private val DATABASE_CREATE_FILTERS = """
            CREATE TABLE $TABLE_FILTERS (
                $KEY_STOP_ID INTEGER NOT NULL,
                $KEY_ROUTE_ID TEXT,
                $KEY_TERMINAL TEXT,
                $KEY_DO_SHOW BOOLEAN NOT NULL,
                FOREIGN KEY($KEY_STOP_ID) REFERENCES $TABLE_FAV_STOPS($KEY_STOP_ID),
                PRIMARY KEY($KEY_STOP_ID, $KEY_ROUTE_ID, $KEY_TERMINAL)
            )
            """

        private val DATABASE_CREATE_TIMESTOP_FILTERS = """
            CREATE TABLE $TABLE_TIMESTOP_FILTERS (
                $KEY_TIMESTOP_ID TEXT NOT NULL,
                $KEY_ROUTE_ID TEXT NOT NULL,
                $KEY_DIRECTION_ID INTEGER NOT NULL,
                $KEY_TERMINAL TEXT,
                $KEY_DO_SHOW BOOLEAN NOT NULL,
                FOREIGN KEY($KEY_TIMESTOP_ID) REFERENCES $TABLE_FAV_TIMESTOPS($KEY_TIMESTOP_ID),
                PRIMARY KEY($KEY_TIMESTOP_ID, $KEY_ROUTE_ID, $KEY_DIRECTION_ID, $KEY_TERMINAL)
            )
            """

        private val DATABASE_CREATE_NEXTRIPS = """
            CREATE TABLE $TABLE_NEXTRIPS (
                $KEY_STOP_ID INTEGER NOT NULL,
                $KEY_IS_ACTUAL BOOLEAN NOT NULL,
                $KEY_TRIP_ID TEXT,
                $KEY_DEPARTURE_UNIX_TIME DATETIME NOT NULL,
                $KEY_DESCRIPTION TEXT,
                $KEY_ROUTE_ID TEXT,
                $KEY_ROUTE_SHORT_NAME TEXT,
                $KEY_DIRECTION_ENUM INTEGER,
                $KEY_TERMINAL TEXT,
                $KEY_SCHEDULE_RELATIONSHIP TEXT,
                FOREIGN KEY ($KEY_TRIP_ID) REFERENCES $TABLE_VEHICLES($KEY_TRIP_ID)
            )
            """

        private val DATABASE_CREATE_TIMESTOP_NEXTRIPS = """
            CREATE TABLE $TABLE_TIMESTOP_NEXTRIPS (
                $KEY_TIMESTOP_ID TEXT NOT NULL,
                $KEY_IS_ACTUAL BOOLEAN NOT NULL,
                $KEY_TRIP_ID TEXT,
                $KEY_DEPARTURE_UNIX_TIME DATETIME NOT NULL,
                $KEY_DESCRIPTION TEXT,
                $KEY_ROUTE_ID TEXT,
                $KEY_ROUTE_SHORT_NAME TEXT,
                $KEY_DIRECTION_ENUM INTEGER,
                $KEY_TERMINAL TEXT,
                $KEY_SCHEDULE_RELATIONSHIP TEXT,
                FOREIGN KEY ($KEY_TRIP_ID) REFERENCES $TABLE_VEHICLES($KEY_TRIP_ID)
            )
            """

        private val DATABASE_CREATE_NEXTRIPS_INDEX = """
            CREATE INDEX $INDEX_NEXTRIPS ON $TABLE_NEXTRIPS ($KEY_STOP_ID)
            """

        private val DATABASE_CREATE_TIMESTOP_NEXTRIPS_INDEX = """
            CREATE INDEX $INDEX_TIMESTOP_NEXTRIPS ON $TABLE_TIMESTOP_NEXTRIPS ($KEY_TIMESTOP_ID, $KEY_DIRECTION_ENUM)
            """

        private val DATABASE_CREATE_STOPS = """
            CREATE TABLE $TABLE_STOPS (
                $KEY_STOP_ID INTEGER PRIMARY KEY,
                $KEY_STOP_NAME TEXT NOT NULL,
                $KEY_STOP_DESC TEXT,
                $KEY_STOP_LAT DOUBLE NOT NULL,
                $KEY_STOP_LON DOUBLE NOT NULL,
                $KEY_WHEELCHAIR_BOARDING INTEGER
            )
            """

        private val DATABASE_CREATE_LAST_UPDATE = """
            CREATE TABLE $TABLE_LAST_UPDATE (
                $KEY_STOP_ID INTEGER PRIMARY KEY,
                $KEY_LAST_UPDATE DATETIME
            )
            """

        private val DATABASE_CREATE_LAST_TIMESTOP_UPDATE = """
            CREATE TABLE $TABLE_LAST_TIMESTOP_UPDATE (
                $KEY_TIMESTOP_ID TEXT NOT NULL,
                $KEY_ROUTE_ID TEXT NOT NULL,
                $KEY_DIRECTION_ID INTEGER NOT NULL,
                $KEY_LAST_UPDATE DATETIME,
                PRIMARY KEY($KEY_TIMESTOP_ID, $KEY_ROUTE_ID, $KEY_DIRECTION_ID)
            )
            """

        private val DATABASE_CREATE_SHAPES = """
            CREATE TABLE $TABLE_SHAPES (
                $KEY_SHAPE_ID INTEGER NOT NULL,
                $KEY_SHAPE_PT_LAT DOUBLE NOT NULL,
                $KEY_SHAPE_PT_LON DOUBLE NOT NULL,
                $KEY_SHAPE_PT_SEQUENCE INTEGER NOT NULL
            )
            """

        private val DATABASE_CREATE_SHAPES_INDEX = """
            CREATE INDEX $INDEX_SHAPES ON $TABLE_SHAPES ($KEY_SHAPE_ID)
            """

        private val DATABASE_CREATE_STOP_SEARCH_HISTORY = """
            CREATE TABLE $TABLE_STOP_SEARCH_HISTORY (
                $KEY_STOP_SEARCH_ID INTEGER PRIMARY KEY,
                $KEY_STOP_SEARCH_DATETIME DATETIME NOT NULL,
                FOREIGN KEY ($KEY_STOP_SEARCH_ID) REFERENCES $TABLE_STOPS($KEY_STOP_ID)
            )
            """

        private val DATABASE_CREATE_VEHICLES = """
            CREATE TABLE $TABLE_VEHICLES (
                $KEY_TRIP_ID TEXT PRIMARY KEY,
                $KEY_DIRECTION_ENUM INTEGER,
                $KEY_LOCATION_TIME DATETIME NOT NULL,
                $KEY_ROUTE_ID TEXT,
                $KEY_TERMINAL TEXT,
                $KEY_VEHICLE_LATITUDE DOUBLE,
                $KEY_VEHICLE_LONGITUDE DOUBLE,
                $KEY_VEHICLE_BEARING DOUBLE,
                $KEY_VEHICLE_ODOMETER DOUBLE,
                $KEY_VEHICLE_SPEED DOUBLE,
                $KEY_SHAPE_ID INTEGER,
                FOREIGN KEY ($KEY_SHAPE_ID) REFERENCES $TABLE_SHAPES($KEY_SHAPE_ID)
            )
            """

        private fun directionToInt(dir: NexTrip.Direction?): Int? =
            dir?.let { NexTrip.getDirectionEnumId(it) }

        private val unixTime: Long
            get() = Calendar.getInstance().timeInMillis / 1000L
    }
}
