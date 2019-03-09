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
import android.content.DialogInterface
import android.os.AsyncTask
import androidx.appcompat.app.AlertDialog
import android.util.JsonReader

import java.io.IOException
import java.io.InputStreamReader
import java.io.UnsupportedEncodingException
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.SocketException
import java.net.URL
import java.net.UnknownHostException
import java.util.ArrayList

class DownloadNexTripsTask(private val mDownloadedListener: OnDownloadedListener,
                           private val mStopId: String) : AsyncTask<Void, Int, Void>() {
    private var mError: DownloadError? = null
    private var mNexTrips: List<NexTrip>? = null

    interface OnDownloadedListener {
        fun onDownloaded(nexTrips: List<NexTrip>)
        fun onDownloadError(err: DownloadError)
    }

    inner class UnauthorizedException : IOException()

    override fun doInBackground(vararg params: Void): Void? {
        var retry: Boolean
        var firstError: DownloadError? = null

        do {
            retry = false
            try {
                mNexTrips = downloadNexTrips(mStopId)
            } catch (e: UnknownHostException) { // probably no internet connection
            	mError = DownloadError.UnknownHost
            } catch (e: java.io.FileNotFoundException) {
                mError = DownloadError.FileNotFound(e.message)
            } catch (e: java.net.SocketTimeoutException) {
                mError = DownloadError.TimedOut(e.message)
            } catch (e: UnauthorizedException) {
                mError = DownloadError.Unauthorized
            } catch (e: SocketException) {
                mError = DownloadError.OtherDownloadError(e.message)
            } catch (e: MalformedURLException) {
                mError = DownloadError.OtherDownloadError(e.message)
            } catch (e: UnsupportedEncodingException) {
                mError = DownloadError.OtherDownloadError(e.message)
            } catch (e: IOException) {
                if (firstError == null) firstError = DownloadError.OtherDownloadError(e.message)
                // old Android versions seem to have a problem with https and
                // throw IOException: CertPathValidatorException,
                // try again using http
                if (mUseHttps) {
                    mUseHttps = false
                    retry = true
                } else {
                    mError = DownloadError.OtherDownloadError(e.message)
                }
            }
        } while (retry)

        if (mError != null && firstError != null) {
            mError = firstError
        }

        return null
    }

    override fun onPostExecute(result: Void?) {
        android.util.Log.d("abc", "got here: onPostExecute(): mNexTrips?.isEmpty() == ${mNexTrips?.isEmpty()}")
        mError?.let { mDownloadedListener.onDownloadError(it) }
        mNexTrips?.let { mDownloadedListener.onDownloaded(it) }
    }

    @Throws(MalformedURLException::class, UnsupportedEncodingException::class, IOException::class)
    private fun downloadNexTrips(stopId: String): List<NexTrip>? {
        var nexTrips: List<NexTrip>?

        val nexTripsUrl = ((if (mUseHttps) "https://" else "http://")
                + NEXTRIPS_URL + stopId + "?format=json")

        val url = URL(nexTripsUrl)
        val urlConnection = url.openConnection() as HttpURLConnection

        val reader = JsonReader(InputStreamReader(urlConnection.inputStream,
                "utf-8"))

        try {
            nexTrips = parseNexTrips(reader)
        } finally {
            reader.close()
        }

        return nexTrips
    }

    @Throws(IOException::class)
    private fun parseNexTrips(reader: JsonReader): List<NexTrip> {
        val nexTrips = ArrayList<NexTrip>()

        reader.beginArray()
        while (reader.hasNext()) {
            reader.beginObject()
            var actual = false
            var blockNumber = -1
            var departureText: String? = null
            var departureTime: String? = null
            var description: String? = null
            var gate: String? = null
            var route: String? = null
            var routeDirection: String? = null
            var terminal: String? = null
            var vehicleHeading = 0.0
            var vehicleLatitude = 0.0
            var vehicleLongitude = 0.0
            while (reader.hasNext()) {
                when (reader.nextName()) {
                    "Actual" -> actual = reader.nextBoolean()
                    "BlockNumber" -> blockNumber = reader.nextInt()
                    "DepartureText" -> departureText = reader.nextString()
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
            nexTrips.add(NexTrip(null, actual, blockNumber, departureText,
                    departureTime, description, gate, route,
                    routeDirection, terminal, vehicleHeading,
                    vehicleLatitude, vehicleLongitude))
            reader.endObject()
        }
        reader.endArray()
        android.util.Log.d("abc", "got here: parseNexTrips().isEmpty() == ${nexTrips.isEmpty()}")

        return nexTrips
    }

    sealed class DownloadError {
        object UnknownHost: DownloadError()
        data class FileNotFound(
            val message: String?
        ): DownloadError()
        data class TimedOut(
            val message: String?
        ): DownloadError()
        object Unauthorized: DownloadError()
        data class OtherDownloadError(
            val message: String?
        ): DownloadError()
    }

    companion object {
        private val NEXTRIPS_URL = "svc.metrotransit.org/NexTrip/"
        private var mUseHttps = true
    }
}
