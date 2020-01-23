/*
    Copyright (C) 2020 Sweetie Piggy Apps <sweetiepiggyapps@gmail.com>

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
import java.io.IOException
import java.io.InputStreamReader
import java.io.UnsupportedEncodingException
import java.net.*

class DownloadShapeIdTask(private val mDownloadedListener: OnDownloadedShapeIdListener,
                           private val mNexTrip: NexTrip, private val mStopId: Int?) : AsyncTask<Void, Int, Void>() {
    private var mShapeId: Int? = null

    interface OnDownloadedShapeIdListener {
        fun onDownloadedShapeId(nexTrip: NexTrip, shapeId: Int)
    }

    inner class UnauthorizedException : IOException()

    override fun doInBackground(vararg params: Void): Void? {
        var retry: Boolean

        do {
            retry = false
            try {
                mShapeId = downloadShapeId(mNexTrip)
            } catch (e: UnknownHostException) { // probably no internet connection
            } catch (e: java.io.FileNotFoundException) {
            } catch (e: java.net.SocketTimeoutException) {
            } catch (e: UnauthorizedException) {
            } catch (e: SocketException) {
            } catch (e: MalformedURLException) {
            } catch (e: IllegalStateException) {
            } catch (e: UnsupportedEncodingException) {
            } catch (e: IOException) {
                // old Android versions seem to have a problem with https and
                // throw IOException: CertPathValidatorException,
                // (or javax.net.ssl.SSLException? or javax.net.ssl.SSLHandshakeException?)
                // try again using http
                if (mUseHttps) {
                    mUseHttps = false
                    retry = true
                }
            }
        } while (retry && !isCancelled)

        return null
    }

    override fun onPostExecute(result: Void?) {
        if (!isCancelled) {
            mShapeId?.let { mDownloadedListener.onDownloadedShapeId(mNexTrip, it) }
        }
    }

    @Throws(MalformedURLException::class, UnsupportedEncodingException::class, IOException::class,
			IllegalStateException::class)
    private fun downloadShapeId(nexTrip: NexTrip): Int?  {
        var possibleShapeIds: Set<Int> = setOf()

        if (nexTrip.blockNumber != null && nexTrip.departureTimeInMillis != null &&
                nexTrip.routeDirection != null && nexTrip.description != null) {
            val dt = nexTrip.departureTimeInMillis / 1000
            val dir = NexTrip.getGtfsDirectionId(nexTrip.routeDirection)
            val tripsUrl = ((if (mUseHttps) "https://" else "http://")
                             + TRIPS_URL
                             + "?block_id=${nexTrip.blockNumber}"
                             + "&departure_time=$dt"
                             + "&direction_id=$dir"
                             + "&description=" + URLEncoder.encode(nexTrip.description, "UTF-8")
                             + (mStopId?.let { "&stop_id=$it" } ?: ""))
            val urlConnection = URL(tripsUrl).openConnection()
            val reader = JsonReader(InputStreamReader(urlConnection.inputStream, "utf-8"))

            try {
                possibleShapeIds = parseTrips(reader)
            } finally {
                reader.close()
            }
        }

        return if (possibleShapeIds.size == 1) possibleShapeIds.first() else null
    }

    @Throws(IOException::class, IllegalStateException::class)
    private fun parseTrips(reader: JsonReader): Set<Int> {
        val possibleShapeIds: MutableSet<Int> = mutableSetOf()

        reader.beginArray()
        while (!isCancelled && reader.hasNext()) {
            reader.beginObject()
            // var tripId: String? = null
            // var blockId: Int? = null
            // var directionId: Int? = null
            // var routeId: String? = null
            // var serviceId: String? = null
            var shapeId: Int? = null
            // var tripHeadsign: String? = null
            // var wheelchairAccessible: Int? = null
            while (reader.hasNext()) {
                val n = reader.nextName()
                when (n) {
                    // "_id" -> tripId = reader.nextString()
                    // "block_id" -> blockId = reader.nextInt()
                    // "direction_id" -> directionId = reader.nextInt()
                    // "route_id" -> routeId = reader.nextString()
                    // "service_id" -> serviceId = reader.nextString()
                    "shape_id" -> shapeId = reader.nextInt()
                    // "trip_headsign" -> tripHeadsign = reader.nextString()
                    // "wheelchair_accessible" -> wheelchairAccessible = reader.nextInt()
                    else -> reader.skipValue()
                }
            }
            reader.endObject()
            if (shapeId != null) {
                possibleShapeIds.add(shapeId)
            }
        }
        reader.endArray()

        return possibleShapeIds
    }

    companion object {
        private val TRIPS_URL = "buswhentwincities.herokuapp.com/trips"
        private var mUseHttps = true
    }
}
