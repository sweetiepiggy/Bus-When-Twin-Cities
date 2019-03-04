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

class DownloadNexTripsTask(private val mContext: Context?, private val mDownloadedListener: OnDownloadedListener,
                           stopId: String?) : AsyncTask<Void, Int, Void>() {
    private var mAlertMessage: String? = null
    private var mStopId: String? = null
    private var mNexTrips: List<NexTrip>? = null

    interface OnDownloadedListener {
        fun onDownloaded(nexTrips: List<NexTrip>)
    }

    inner class UnauthorizedException : IOException()

    init {
        mStopId = stopId
    }

    override fun doInBackground(vararg params: Void): Void? {
        var retry: Boolean
        var firstAlertMessage: String? = null

        do {
            retry = false
            try {
                mNexTrips = downloadNexTrips(mStopId)
            } catch (e: UnknownHostException) { // probably no internet connection
                mAlertMessage = mContext!!.resources.getString(R.string.unknown_host)
            } catch (e: java.io.FileNotFoundException) {
                mAlertMessage = mContext!!.resources.getString(R.string.file_not_found) + ":\n" + e.message
            } catch (e: java.net.SocketTimeoutException) {
                mAlertMessage = mContext!!.resources.getString(R.string.timed_out) + ":\n" + e.message
            } catch (e: UnauthorizedException) {
                mAlertMessage = mContext!!.resources.getString(R.string.unauthorized)
            } catch (e: SocketException) {
                mAlertMessage = e.message
            } catch (e: MalformedURLException) {
                mAlertMessage = e.message
            } catch (e: UnsupportedEncodingException) {
                mAlertMessage = e.message
            } catch (e: IOException) {
                if (firstAlertMessage == null) {
                    firstAlertMessage = e.message
                }
                // old Android versions seem to have a problem with https and
                // throw IOException: CertPathValidatorException,
                // try again using http
                if (mUseHttps) {
                    mUseHttps = false
                    retry = true
                } else {
                    mAlertMessage = e.message
                }
            }

        } while (retry)

        if (mAlertMessage != null && firstAlertMessage != null) {
            mAlertMessage = firstAlertMessage
        }

        return null
    }

    override fun onPostExecute(result: Void) {
        val alertMessage = mAlertMessage
        if (alertMessage != null && mContext != null) {
            alert(alertMessage)
        }
        val nexTrips = mNexTrips
        if (nexTrips != null) {
            mDownloadedListener.onDownloaded(nexTrips)
        }
    }

    private fun alert(msg: String) {
        val alert = AlertDialog.Builder(mContext!!)
        alert.setTitle(mContext.resources.getString(android.R.string.dialog_alert_title))
        alert.setMessage(msg)
        alert.setPositiveButton(android.R.string.ok) { dialog, which -> }
        alert.show()
    }

    @Throws(MalformedURLException::class, UnsupportedEncodingException::class, IOException::class)
    private fun downloadNexTrips(stopId: String?): List<NexTrip>? {
        var nexTrips: List<NexTrip>? = null

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
        // nexTrips = new ArrayList<NexTrip>();
        // nexTrips.add(new NexTrip(true, 1175, "5 Min", "/Date(1547811780000-0600)/",
        //                        "Minn Drive / France Av / Southdale", "", "6",
        //                        "SOUTHBOUND", "F", 0, 44.980820, -93.270970));
        // nexTrips.add(new NexTrip(true, 2036, "10 Min", "/Date(1547812080000-0600)/",
        //                        "Hopkins/United Health/Bren Rd W", "", "12",
        //                        "WESTBOUND", "G", 0, 44.982220, -93.268790));
        // nexTrips.add(new NexTrip(false, 1078, "7:58", "/Date(1547812200000-0600)/",
        //                        "Bryant Av/82St-35W TC/Via Lyndale", "", "4",
        //                        "SOUTHBOUND", "L", 0, 0, 0));

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
                val name = reader.nextName()
                if (name == "Actual") {
                    actual = reader.nextBoolean()
                } else if (name == "BlockNumber") {
                    blockNumber = reader.nextInt()
                } else if (name == "DepartureText") {
                    departureText = reader.nextString()
                } else if (name == "DepartureTime") {
                    departureTime = reader.nextString()
                } else if (name == "Description") {
                    description = reader.nextString()
                } else if (name == "Gate") {
                    gate = reader.nextString()
                } else if (name == "Route") {
                    route = reader.nextString()
                } else if (name == "RouteDirection") {
                    routeDirection = reader.nextString()
                } else if (name == "Terminal") {
                    terminal = reader.nextString()
                } else if (name == "VehicleHeading") {
                    vehicleHeading = reader.nextDouble()
                } else if (name == "VehicleLatitude") {
                    vehicleLatitude = reader.nextDouble()
                } else if (name == "VehicleLongitude") {
                    vehicleLongitude = reader.nextDouble()
                } else {
                    reader.skipValue()
                }
            }
            nexTrips.add(NexTrip(mContext, actual, blockNumber, departureText,
                    departureTime, description, gate, route,
                    routeDirection, terminal, vehicleHeading,
                    vehicleLatitude, vehicleLongitude))
            reader.endObject()
        }
        reader.endArray()

        return nexTrips
    }

    companion object {
        private val NEXTRIPS_URL = "svc.metrotransit.org/NexTrip/"
        private var mUseHttps = true
    }
}
