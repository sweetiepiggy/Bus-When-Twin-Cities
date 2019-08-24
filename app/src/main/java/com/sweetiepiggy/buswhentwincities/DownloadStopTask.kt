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
import java.io.IOException
import java.io.InputStreamReader
import java.io.UnsupportedEncodingException
import java.net.*
import java.util.*

class DownloadStopTask(private val mDownloadedListener: OnDownloadedListener,
                           private val mStopId: Int) : AsyncTask<Void, Int, Void>() {
    private var mStop: Stop? = null

    interface OnDownloadedListener {
        fun onDownloaded(stop: Stop)
    }

    inner class UnauthorizedException : IOException()

    override fun doInBackground(vararg params: Void): Void? {
        var retry: Boolean

        do {
            retry = false
            try {
                mStop = downloadStop(mStopId)
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
            mStop?.let { mDownloadedListener.onDownloaded(it) }
        }
    }

    @Throws(MalformedURLException::class, UnsupportedEncodingException::class, IOException::class,
			IllegalStateException::class)
    private fun downloadStop(stopId: Int): Stop? {
        val stopUrl = ((if (mUseHttps) "https://" else "http://")
                + STOP_URL + stopId.toString() + "?format=json")
        val urlConnection = URL(stopUrl).openConnection() as HttpURLConnection
        val reader = JsonReader(InputStreamReader(urlConnection.inputStream, "utf-8"))

        var stop: Stop?

        try {
            stop = parseStop(reader)
        } finally {
            reader.close()
        }

        return stop
    }

    @Throws(IOException::class, IllegalStateException::class)
    private fun parseStop(reader: JsonReader): Stop? {
        var stop: Stop? = null

        if (!isCancelled && reader.hasNext()) {
            reader.beginObject()
            var stopId: Int? = null
            var stopName: String? = null
            var stopDesc: String? = null
            var stopLat: Double? = null
            var stopLon: Double? = null
            var wheelchairBoarding: Int? = null
            while (reader.hasNext()) {
                val n = reader.nextName()
                when (n) {
                    "id" -> stopId = reader.nextInt()
                    "stop_name" -> stopName = reader.nextString()
                    "stop_desc" -> stopDesc = reader.nextString()
                    "stop_lat" -> stopLat = reader.nextDouble()
                    "stop_lon" -> stopLon = reader.nextDouble()
                    "wheelchair_boarding" -> wheelchairBoarding = reader.nextInt()
                    else -> reader.skipValue()
                }
            }
            reader.endObject()
            if (stopId != null && stopName != null && stopLat != null && stopLon != null) {
                stop = Stop(stopId, stopName, stopDesc, stopLat, stopLon, wheelchairBoarding)
            }
        }

        return stop
    }

    companion object {
        private val STOP_URL = "buswhentwincities.herokuapp.com/stops/"
        // private val STOP_URL = "buswhentwincities.appspot.com/stops/"
        private var mUseHttps = true
    }
}
