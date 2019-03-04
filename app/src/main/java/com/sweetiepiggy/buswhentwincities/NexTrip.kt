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

import android.content.Context
import android.text.format.DateFormat

import java.util.Calendar
import java.util.Date

class NexTrip(private val mCtxt: Context, internal val isActual: Boolean, internal val blockNumber: Int, departureText: String,
              departureTime: String, internal val description: String, internal val gate: String,
              internal val route: String, routeDirection: String, internal val terminal: String,
              internal val vehicleHeading: Double, internal val vehicleLatitude: Double,
              internal val vehicleLongitude: Double) {
    internal var departureText: String? = null
        private set
    internal var departureTime: String? = null
        private set
    internal val routeDirection: String

    init {
        this.routeDirection = translateDirection(routeDirection)

        val departureTimeInMillis = parseDepartureTime(departureTime)
        val millisUntilDeparture = departureTimeInMillis - Calendar.getInstance().timeInMillis
        val minutesUntilDeparture = millisUntilDeparture / 1000 / 60
        if (departureTimeInMillis < 0 || millisUntilDeparture < 0) {
            this.departureText = translateDepartureText(departureText)
            this.departureTime = ""
        } else if (minutesUntilDeparture < 60) {
            this.departureText = if (minutesUntilDeparture < 1)
                mCtxt.resources.getString(R.string.due)
            else
                java.lang.Long.toString(minutesUntilDeparture)
                        + " " + mCtxt.resources.getString(R.string.minutes)
            this.departureTime = DateFormat.getTimeFormat(mCtxt).format(Date(departureTimeInMillis))
        } else {
            this.departureText = DateFormat.getTimeFormat(mCtxt).format(Date(departureTimeInMillis))
            this.departureTime = ""
        }
    }

    internal fun parseDepartureTime(departureTime: String): Long {
        if (departureTime.startsWith("/Date(")) {
            var timezoneIdx = departureTime.indexOf('-', 6)
            if (timezoneIdx < 0) {
                timezoneIdx = departureTime.indexOf('+', 6)
                if (timezoneIdx < 0) {
                    return -1
                }
            }
            return java.lang.Long.parseLong(departureTime.substring(6, timezoneIdx))
        }
        return -1
    }

    internal fun translateDepartureText(departureText: String): String {
        return if (departureText.endsWith(" Min")) {
            departureText.substring(0, departureText.length - 3) + mCtxt.resources.getString(R.string.minutes)
        } else {
            departureText
        }
    }

    internal fun translateDirection(dir: String): String {
        return if (dir == "SOUTHBOUND") {
            mCtxt.resources.getString(R.string.south)
        } else if (dir == "EASTBOUND") {
            mCtxt.resources.getString(R.string.east)
        } else if (dir == "WESTBOUND") {
            mCtxt.resources.getString(R.string.west)
        } else if (dir == "NORTHBOUND") {
            mCtxt.resources.getString(R.string.north)
        } else {
            dir
        }
    }
}
