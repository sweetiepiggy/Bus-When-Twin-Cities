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

import android.os.AsyncTask
import android.util.JsonReader
import android.util.JsonToken
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.net.MalformedURLException
import java.net.SocketException
import java.net.UnknownHostException
import java.util.*

class DownloadNexTripsTask(private val mDownloadedListener: OnDownloadedNexTripsListener,
                           private val mStopId: Int) : AsyncTask<Void, Int, Void>() {
    private var mError: MetroTransitDownloader.DownloadError? = null
    private var mNexTrips: List<NexTrip>? = null

    interface OnDownloadedNexTripsListener {
        fun onDownloadedNexTrips(nexTrips: List<NexTrip>)
        fun onDownloadedNexTripsError(err: MetroTransitDownloader.DownloadError)
    }

    override fun doInBackground(vararg params: Void): Void? {
        var rawNexTrips: List<RawNexTrip>? = null

        try {
            val reader = MetroTransitDownloader().openJsonReader(MetroTransitDownloader.NexTripOperation.GetDepartures(mStopId))
            rawNexTrips = parseNexTrips(reader)
            reader.close()
        } catch (e: UnknownHostException) { // probably no internet connection
        	mError = MetroTransitDownloader.DownloadError.UnknownHost
        } catch (e: java.io.FileNotFoundException) {
            mError = MetroTransitDownloader.DownloadError.FileNotFound(e.message)
        } catch (e: java.net.SocketTimeoutException) {
            mError = MetroTransitDownloader.DownloadError.TimedOut(e.message)
        } catch (e: SocketException) {
            mError = MetroTransitDownloader.DownloadError.OtherDownloadError(e.message)
        } catch (e: MalformedURLException) {
            mError = MetroTransitDownloader.DownloadError.OtherDownloadError(e.message)
        } catch (e: UnsupportedEncodingException) {
            mError = MetroTransitDownloader.DownloadError.OtherDownloadError(e.message)
        } catch (e: IOException) {
            mError = MetroTransitDownloader.DownloadError.OtherDownloadError(e.message)
        }

        if (!isCancelled() && rawNexTrips != null) {
            val timeInMillis = Calendar.getInstance().timeInMillis
            mNexTrips = rawNexTrips.map { NexTrip.from(it, timeInMillis) }
        }

        return null
    }

    override fun onPostExecute(result: Void?) {
        if (!isCancelled) {
            mError?.let { mDownloadedListener.onDownloadedNexTripsError(it) }
            mNexTrips?.let { mDownloadedListener.onDownloadedNexTrips(it) }
        }
    }

    private fun parseNexTrips(reader: JsonReader): List<RawNexTrip> {
        val rawNexTrips: MutableList<RawNexTrip> = mutableListOf()

        reader.beginArray()
        while (!isCancelled() && reader.hasNext()) {
            reader.beginObject()
            var actual = false
            var blockNumber: Int? = null
            var departureText: String? = null
            var departureTime: String? = null
            var description: String? = null
            var gate: String? = null
            var route: String? = null
            var routeDirection: String? = null
            var terminal: String? = null
            var vehicleHeading: Double? = null
            var vehicleLatitude: Double? = null
            var vehicleLongitude: Double? = null
            while (reader.hasNext()) {
                val name = reader.nextName()
                if (reader.peek() == JsonToken.NULL)
                  reader.skipValue()
                else
                  when (name) {
                      "Actual" -> actual = reader.nextBoolean()
                      "BlockNumber" -> blockNumber = reader.nextInt()
                      "DepartureText" -> departureText = reader.nextString()
                      "DepartureTime" -> departureTime = reader.nextString()
                      "Description" -> description = reader.nextString()
                      "Gate" -> gate = reader.nextString()
                      "Route" -> route = reader.nextString()
                      "RouteDirection" -> routeDirection = reader.nextString()
                      "Terminal" -> terminal = reader.nextString()
                      "VehicleHeading" -> vehicleHeading = reader.nextDouble()
                      "VehicleLatitude" -> vehicleLatitude = reader.nextDouble()
                      "VehicleLongitude" -> vehicleLongitude = reader.nextDouble()
                      else -> reader.skipValue()
                  }
            }
            rawNexTrips.add(RawNexTrip(actual, blockNumber, departureText,
                    departureTime, description, gate, route,
                    routeDirection, terminal, vehicleHeading,
                    vehicleLatitude, vehicleLongitude))
            reader.endObject()
        }
        reader.endArray()

        return rawNexTrips
    }

}
