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
import java.io.IOException
import java.io.InputStreamReader
import java.io.UnsupportedEncodingException
import java.net.*
import java.util.*

class DownloadRoutesTask(private val mDownloadedRoutesListener: OnDownloadedRoutesListener) : AsyncTask<Void, Int, Void>() {
    private var mError: MetroTransitDownloader.DownloadError? = null
    private var mRoutes: List<BrowseRoutesViewModel.Route>? = null

    interface OnDownloadedRoutesListener {
        fun onDownloadedRoutes(routes: List<BrowseRoutesViewModel.Route>)
        fun onDownloadedRoutesError(err: MetroTransitDownloader.DownloadError)
    }

    override fun doInBackground(vararg params: Void): Void? {
        try {
            val reader = MetroTransitDownloader().openJsonReader(MetroTransitDownloader.NexTripOperation.GetRoutes)
            mRoutes = parseRoutes(reader)
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
            mError?.let { mDownloadedRoutesListener.onDownloadedRoutesError(it) }
            mRoutes?.let { mDownloadedRoutesListener.onDownloadedRoutes(it) }
        }
    }

    private fun parseRoutes(reader: JsonReader): List<BrowseRoutesViewModel.Route>? {
        var routes: MutableList<BrowseRoutesViewModel.Route> = mutableListOf()

        reader.beginArray()
        while (!isCancelled && reader.hasNext()) {
            reader.beginObject()
            var routeId: String? = null
            var agencyId: Int? = null
            var routeLabel: String? = null
            while (reader.hasNext()) {
                when (reader.nextName()) {
                    "route_id"    -> routeId = reader.nextString()
                    "agency_id"   -> agencyId = reader.nextInt()
                    "route_label" -> routeLabel = reader.nextString()
                    else          -> reader.skipValue()
                }
            }
            reader.endObject()
            if (routeId != null && agencyId != null && routeLabel != null) {
                routes.add(BrowseRoutesViewModel.Route(routeId, agencyId, routeLabel))
            }
        }
        reader.endArray()

        return routes
    }
}
