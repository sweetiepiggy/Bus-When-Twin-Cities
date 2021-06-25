/*
    Copyright (C) 2019,2021 Sweetie Piggy Apps <sweetiepiggyapps@gmail.com>

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
        var stops: List<Stop>? = null

        try {
            val reader = MetroTransitDownloader().openJsonReader(MetroTransitDownloader.NexTripOperation.GetDepartures(mStopId))
            val rawNexTripsAndStops = parseNexTrips(reader)
            reader.close()
            rawNexTrips = rawNexTripsAndStops.first
            stops = rawNexTripsAndStops.second
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
            val time = Calendar.getInstance().timeInMillis / 1000
            mNexTrips = rawNexTrips.map { NexTrip.from(it, time) }
        }

        return null
    }

    override fun onPostExecute(result: Void?) {
        if (!isCancelled) {
            mError?.let { mDownloadedListener.onDownloadedNexTripsError(it) }
            mNexTrips?.let { mDownloadedListener.onDownloadedNexTrips(it) }
        }
    }

    private fun parseNexTrips(reader: JsonReader): Pair<List<RawNexTrip>, List<Stop>> {
        val rawNexTrips: MutableList<RawNexTrip> = mutableListOf()
        var stops: MutableList<Stop> = mutableListOf()

        reader.beginObject()
        while (!isCancelled() && reader.hasNext()) {
            when (reader.nextName()) {
                "stops" -> {
                    reader.beginArray()
                    while (!isCancelled() && reader.hasNext()) {
                        reader.beginObject()
                        var stopId: Int? = null
                        var stopLat: Double? = null
                        var stopLon: Double? = null
                        var stopDesc: String? = null
                        while (reader.hasNext()) {
                            val n = reader.nextName()
                            when (n) {
                                "stop_id" -> stopId = reader.nextInt()
                                "latitude" -> stopLat = reader.nextDouble()
                                "longitude" -> stopLon = reader.nextDouble()
                                "description" -> stopDesc = reader.nextString()
                                else -> reader.skipValue()
                            }
                        }
                        if (stopId != null && stopLat != null && stopLon != null) {
                            stops.add(Stop(stopId, null, stopLat, stopLon, stopDesc))
                        }
                        reader.endObject()
                    }
                    reader.endArray()
                }
                "alerts" -> reader.skipValue()
                "departures" -> {
                    reader.beginArray()
                    while (!isCancelled() && reader.hasNext()) {
                        reader.beginObject()
                        var isActual: Boolean = false
                        var tripId: String? = null
                        var departureText: String? = null
                        var departureTime: Long? = null
                        var description: String? = null
                        var routeId: String? = null
                        var routeShortName: String? = null
                        var directionId: Int? = null
                        var directionText: String? = null
                        var terminal: String? = null
                        var scheduleRelationship: String? = null
                        while (reader.hasNext()) {
                            val name = reader.nextName()
                            if (reader.peek() == JsonToken.NULL)
                                reader.skipValue()
                            else
                                when (name) {
                                    "actual" -> isActual = reader.nextBoolean()
                                    "trip_id" -> tripId = reader.nextString()
                                    "stop_id" -> reader.skipValue()
                                    "departure_text" -> departureText = reader.nextString()
                                    "departure_time" -> departureTime = reader.nextLong()
                                    "description" -> description = reader.nextString()
                                    "route_id" -> routeId = reader.nextString()
                                    "route_short_name" -> routeShortName = reader.nextString()
                                    "direction_id" -> directionId = reader.nextInt()
                                    "direction_text" -> directionText = reader.nextString()
                                    "terminal" -> terminal = reader.nextString()
                                    "schedule_relationship" -> scheduleRelationship = reader.nextString()
                                    else -> reader.skipValue()
                                }
                        }
                        rawNexTrips.add(RawNexTrip(isActual, tripId, departureText,
                                                   departureTime, description, routeId, routeShortName,
                                                   directionId, directionText, terminal,
                                                   scheduleRelationship))
                        reader.endObject()
                    }
                    reader.endArray()
                }
                else -> reader.skipValue()
            }
        }
        reader.endObject()

        return Pair(rawNexTrips, stops)
    }

}
