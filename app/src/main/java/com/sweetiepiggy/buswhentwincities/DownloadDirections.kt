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

class DownloadDirectionsTask(private val mDownloadedDirectionsListener: OnDownloadedDirectionsListener,
		                     private val mRoute: Int) : AsyncTask<Void, Int, Void>() {
    private var mError: MetroTransitDownloader.DownloadError? = null
    private var mDirections: List<NexTrip.Direction>? = null

    interface OnDownloadedDirectionsListener {
        fun onDownloadedDirections(directions: List<NexTrip.Direction>)
        fun onDownloadedDirectionsError(err: MetroTransitDownloader.DownloadError)
    }

    override fun doInBackground(vararg params: Void): Void? {
        try {
            val reader = MetroTransitDownloader().openJsonReader(MetroTransitDownloader.NexTripOperation.GetDirections(mRoute))
            mDirections = parseDirections(reader)
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
        } catch (e: IllegalStateException) {
            mError = MetroTransitDownloader.DownloadError.OtherDownloadError(e.message)
        } catch (e: UnsupportedEncodingException) {
            mError = MetroTransitDownloader.DownloadError.OtherDownloadError(e.message)
        } catch (e: IOException) {
            mError = MetroTransitDownloader.DownloadError.OtherDownloadError(e.message)
        }

        return null
    }

    override fun onPostExecute(result: Void?) {
        if (!isCancelled) {
            mError?.let { mDownloadedDirectionsListener.onDownloadedDirectionsError(it) }
            mDirections?.let { mDownloadedDirectionsListener.onDownloadedDirections(it) }
        }
    }

    private fun parseDirections(reader: JsonReader): List<NexTrip.Direction>? {
        var directions: MutableList<NexTrip.Direction> = mutableListOf()

        reader.beginArray()
        while (!isCancelled && reader.hasNext()) {
            reader.beginObject()
            var text: String? = null
//            var value: Int? = null
            while (reader.hasNext()) {
                when (reader.nextName()) {
                    "Text"  -> text = reader.nextString()
//                    "Value" -> value = reader.nextInt()
                    else          -> reader.skipValue()
                }
            }
            reader.endObject()
            if (text != null) {
                NexTrip.Direction.from(text)?.let { directions.add(it) }
            }
        }
        reader.endArray()

        return directions
    }
}
