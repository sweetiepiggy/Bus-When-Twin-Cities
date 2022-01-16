/*
    Copyright (C) 2019,2021-2022 Sweetie Piggy Apps <sweetiepiggyapps@gmail.com>

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

class DownloadTimepointDeparturesTask(private val mDownloadedListener: DownloadNexTripsTask.OnDownloadedNexTripsListener,
                                      private val mRouteId: String, private val mDirectionId: Int,
                                      private val mTimestopId: String) : AsyncTask<Void, Int, Void>(),
                                      DownloadNexTripsTask.OnDownloadedNexTripsListener {
    private var mError: MetroTransitDownloader.DownloadError? = null
    private var mStop: Stop? = null

    override fun doInBackground(vararg params: Void): Void? {
        var stops: List<Stop>? = null

        try {
            val reader = MetroTransitDownloader().openJsonReader(MetroTransitDownloader.NexTripOperation.GetTimepointDepartures(mRouteId, mDirectionId, mTimestopId))
            stops = parseNexTrips(reader)
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

        if (!isCancelled() && stops != null && stops.size == 1) {
            mStop = stops[0]
        }

        return null
    }

    override fun onPostExecute(result: Void?) {
        if (!isCancelled) {
            mError?.let { mDownloadedListener.onDownloadedNexTripsError(it) }
            mStop?.let {
                mDownloadedListener.onDownloadedStop(it)
                DownloadNexTripsTask(this, it.stopId).execute()
            }
        }
    }

    override fun onDownloadedNexTrips(nexTrips: List<NexTrip>, vehicles: List<Vehicle>) {
        mDownloadedListener.onDownloadedNexTrips(nexTrips, vehicles)
    }

    override fun onDownloadedStop(stop: Stop) {
    }

    override fun onDownloadedNexTripsError(err: MetroTransitDownloader.DownloadError) {
        mDownloadedListener.onDownloadedNexTripsError(err)
    }

    private fun parseNexTrips(reader: JsonReader): List<Stop> {
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
                "departures" -> reader.skipValue()
                else -> reader.skipValue()
            }
        }
        reader.endObject()

        return stops
    }
}
