/*
    Copyright (C) 2019-2020 Sweetie Piggy Apps <sweetiepiggyapps@gmail.com>

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

import android.content.Context
import android.content.res.Resources
import android.location.Location
import android.text.format.DateFormat
import com.google.android.gms.maps.model.LatLng
import java.lang.Math.abs
import java.util.*

// the raw data as it comes
data class RawNexTrip(val isActual: Boolean, val blockNumber: Int?, val departureText: String?,
              val departureTime: String?, val description: String?, val gate: String?,
              val route: String?, val routeDirection: String?, val terminal: String?,
              val vehicleHeading: Double?, val vehicleLatitude: Double?,
              val vehicleLongitude: Double?)

// processed data, replace String with Int/enum, etc.
class NexTrip(val isActual: Boolean, val blockNumber: Int?, val departureTimeInMillis: Long?,
			val description: String?, val gate: String?, val route: String?,
			val routeDirection: Direction?, val terminal: String?, val vehicleHeading: Double?,
        	vehicleLatitude: Double?, vehicleLongitude: Double?, val shapeId: Int?,
            val locationSuppressed: Boolean = false) {

    val position: LatLng? = vehicleLatitude?.let { latitude ->
        vehicleLongitude?.let { longitude ->
            LatLng(latitude, longitude).let {
                if (distanceBetweenIsSmall(it, ORIGIN_LAT_LNG)) null else it
            }
        }
    }

    val routeAndTerminal: String?
    	get() = route?.let { it + (terminal ?: "") }

    enum class Direction {
        SOUTH, EAST, WEST, NORTH;
        companion object {
            fun from(strDirection: String?): Direction? =
            	when(strDirection) {
                    "SOUTHBOUND" -> SOUTH
                    "EASTBOUND"  -> EAST
                    "WESTBOUND"  -> WEST
                    "NORTHBOUND" -> NORTH
                    else -> null
                }
            fun from(directionId: Int?): Direction? =
            	when(directionId) {
                    SOUTH_ID -> SOUTH
                    EAST_ID  -> EAST
                    WEST_ID  -> WEST
                    NORTH_ID -> NORTH
                    else -> null
                }
        }
    }

    enum class Vehicle {
        BUS, LIGHTRAIL, TRAIN
    }

    fun minutesUntilDeparture(timeInMillis: Long): Long? =
    	departureTimeInMillis?.let { (it - timeInMillis) / 1000 / 60 }

    companion object {
        val MINUTES_BEFORE_TO_SHOW_LOC = 30

        fun from(rawNexTrip: RawNexTrip, timeInMillis: Long): NexTrip {
            // if we're not given departureTime then try to compute it from departureText
            val departureTimeInMillis: Long? = parseDepartureTime(rawNexTrip.departureTime) ?:
    	    	parseDepartureText(rawNexTrip.departureText, timeInMillis)

        	return NexTrip(
                rawNexTrip.isActual, rawNexTrip.blockNumber, departureTimeInMillis,
                rawNexTrip.description, rawNexTrip.gate, rawNexTrip.route,
                Direction.from(rawNexTrip.routeDirection), rawNexTrip.terminal,
                rawNexTrip.vehicleHeading, rawNexTrip.vehicleLatitude, rawNexTrip.vehicleLongitude,
                null
            )
        }

        fun suppressLocation(nexTrip: NexTrip): NexTrip {
            val locationSuppressed = nexTrip.position != null || nexTrip.locationSuppressed
        	return NexTrip(nexTrip.isActual, nexTrip.blockNumber, nexTrip.departureTimeInMillis,
        		           nexTrip.description, nexTrip.gate, nexTrip.route, nexTrip.routeDirection,
        		           nexTrip.terminal, null, null, null, nexTrip.shapeId, locationSuppressed)
        }

        fun setShapeId(nexTrip: NexTrip, shapeId: Int): NexTrip =
        	NexTrip(nexTrip.isActual, nexTrip.blockNumber, nexTrip.departureTimeInMillis,
        		    nexTrip.description, nexTrip.gate, nexTrip.route, nexTrip.routeDirection,
        		    nexTrip.terminal, nexTrip.vehicleHeading, nexTrip.position?.latitude,
                    nexTrip.position?.longitude, shapeId, nexTrip.locationSuppressed)

        /** @return departure time in millis since 1970 */
        private fun parseDepartureTime(departureTime: String?): Long? =
            if (departureTime != null && departureTime.startsWith("/Date(")) {
                val timezoneIdx0 = departureTime.indexOf('-', 6)
                val timezoneIdx = if (timezoneIdx0 >= 0) timezoneIdx0 else departureTime.indexOf('+', 6)
                try {
                    departureTime.substring(6, timezoneIdx).toLong()
                } catch (e: NumberFormatException) {
                    null
                }
            } else {
                null
            }

        /** @return departure time in millis since 1970 */
        private fun parseDepartureText(departureText: String?, timeInMillis: Long): Long? =
        	if (departureText != null && departureText.endsWith(" Min")) {
                try {
                    val minutesUntilDeparture =
                    	departureText.substring(0, departureText.length - 4).toInt()
                    val millisUntilDeparture = minutesUntilDeparture * 60 * 1000
                    timeInMillis + millisUntilDeparture
                } catch (e: NumberFormatException) {
                    null
                }
            } else {
                null
            }

        fun distanceBetweenIsSmall(pos1: LatLng?, pos2: LatLng?): Boolean =
            (pos1 == pos2) || (distanceBetween(pos1!!, pos2!!)?.let { it < 1 } ?: false)

        /** @return distance in meters between the two positions */
        private fun distanceBetween(pos1: LatLng, pos2: LatLng): Float? {
            var results: FloatArray = floatArrayOf(0f)
            Location.distanceBetween(pos1.latitude, pos1.longitude,
        		pos2.latitude, pos2.longitude, results)
            return results[0]
        }

        private val ORIGIN_LAT_LNG: LatLng = LatLng(0.0, 0.0)

        fun guessIsSameNexTrip(a: NexTrip, b: NexTrip) =
        	a.blockNumber == b.blockNumber &&
                // blockNumber is not unique, but hopefully it is unique per 20 minutes,
                //   and hopefully departure time doesn't change by more than 20 minutes
        		(abs(a.departureTimeInMillis!! - b.departureTimeInMillis!!) < 20 * 60 * 1000)

        fun getDirectionId(dir: NexTrip.Direction): Int =
	    	when(dir) {
        	    NexTrip.Direction.SOUTH -> SOUTH_ID
        	    NexTrip.Direction.EAST  -> EAST_ID
        	    NexTrip.Direction.WEST  -> WEST_ID
        	    NexTrip.Direction.NORTH -> NORTH_ID
            }
        fun getGtfsDirectionId(dir: NexTrip.Direction): Int =
	    	when(dir) {
        	    NexTrip.Direction.SOUTH -> 1
        	    NexTrip.Direction.EAST  -> 0
        	    NexTrip.Direction.WEST  -> 1
        	    NexTrip.Direction.NORTH -> 0
            }

        private val SOUTH_ID = 1
        private val EAST_ID = 2
        private val WEST_ID = 3
        private val NORTH_ID = 4
     }
}

// processed data for presentation
class PresentableNexTrip(val nexTrip: NexTrip, timeInMillis: Long, context: Context) {

    val isActual: Boolean = nexTrip.isActual
    val blockNumber: Int? = nexTrip.blockNumber
    val description: String? = nexTrip.description
    val route: String? = nexTrip.route
    val terminal: String? = nexTrip.terminal
    val routeAndTerminal: String? = route?.let { it + (terminal ?: "") }
    val routeDirection: NexTrip.Direction? = nexTrip.routeDirection
    val routeDirectionStr: String? = translateDirection(nexTrip.routeDirection, context.resources)
    val routeDirectionBoundStr: String? = translateDirectionBound(nexTrip.routeDirection, context.resources)
    val position: LatLng? = nexTrip.position
    val departureTimeInMillis: Long? = nexTrip.departureTimeInMillis
    val minutesUntilDeparture: Long? = nexTrip.minutesUntilDeparture(timeInMillis)
    val shapeId: Int? = nexTrip.shapeId
    val locationSuppressed: Boolean = nexTrip.locationSuppressed

    val departureText: String?
	val departureTime: String?

    init {
        if (nexTrip.departureTimeInMillis == null) {
            departureText = null
            departureTime = null
        } else if (nexTrip.departureTimeInMillis < 0 || minutesUntilDeparture!! < 0) {
            departureText = context.resources.getString(R.string.past_due)
            departureTime = null
        } else if (minutesUntilDeparture < 60) {
            departureText = if (minutesUntilDeparture < 1)
                 context.resources.getString(R.string.due)
            else
                 minutesUntilDeparture.toString() + " " + context.resources.getString(R.string.minutes)
            departureTime = DateFormat.getTimeFormat(context).format(Date(nexTrip.departureTimeInMillis))
        } else {
            departureText = DateFormat.getTimeFormat(context).format(Date(nexTrip.departureTimeInMillis))
            departureTime = null
         }
    }

    fun getVehicle(): NexTrip.Vehicle =
        when (route) {
            "Blue"  -> NexTrip.Vehicle.LIGHTRAIL
            "Grn"   -> NexTrip.Vehicle.LIGHTRAIL
            "Nstar" -> NexTrip.Vehicle.TRAIN
            else    -> NexTrip.Vehicle.BUS
        }

    companion object {
        fun guessIsSameNexTrip(a: PresentableNexTrip, b: PresentableNexTrip) =
            NexTrip.guessIsSameNexTrip(a.nexTrip, b.nexTrip)

        fun translateDirection(dir: NexTrip.Direction?, resources: Resources): String? =
        	when (dir) {
                NexTrip.Direction.SOUTH -> resources.getString(R.string.south)
                NexTrip.Direction.EAST  -> resources.getString(R.string.east)
                NexTrip.Direction.WEST  -> resources.getString(R.string.west)
                NexTrip.Direction.NORTH -> resources.getString(R.string.north)
                else -> null
            }

        fun translateDirectionBound(dir: NexTrip.Direction?, resources: Resources): String? =
        	when (dir) {
                NexTrip.Direction.SOUTH -> resources.getString(R.string.southbound)
                NexTrip.Direction.EAST  -> resources.getString(R.string.eastbound)
                NexTrip.Direction.WEST  -> resources.getString(R.string.westbound)
                NexTrip.Direction.NORTH -> resources.getString(R.string.northbound)
                else -> null
            }
    }

    override fun toString(): String =
      "($departureText, $departureTime, $isActual, $blockNumber, $description, $routeAndTerminal, $routeDirection, $departureTimeInMillis)"
}
