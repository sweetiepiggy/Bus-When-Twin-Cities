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

import android.content.Context
import android.os.AsyncTask
import android.util.JsonReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.UnsupportedEncodingException
import org.osmdroid.util.GeoPoint
import java.net.MalformedURLException
import java.net.SocketException
import java.net.URL
import java.net.UnknownHostException

class DownloadShapeTask(private val mDownloadedListener: OnDownloadedShapeListener,
                        private val mContext: Context,
                        private val mShapeId: Int) : AsyncTask<Void, Int, Void>() {
    private var mShape: List<GeoPoint>? = null

    interface OnDownloadedShapeListener {
        fun onDownloadedShape(shapeId: Int, shape: List<GeoPoint>)
    }

    inner class UnauthorizedException : IOException()

    override fun doInBackground(vararg params: Void): Void? {
        var retry: Boolean

        do {
            retry = false
            try {
                mShape = downloadShape(mShapeId)
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
            mShape?.let { mDownloadedListener.onDownloadedShape(mShapeId, it) }
        }
    }

    @Throws(MalformedURLException::class, UnsupportedEncodingException::class, IOException::class,
			IllegalStateException::class)
    private fun downloadShape(shapeId: Int): List<GeoPoint>?  {
        var shape: List<GeoPoint>?

        val shapesUrl = ((if (mUseHttps) "https://" else "http://")
                         + "$SHAPES_URL/$shapeId")
        val urlConnection = URL(shapesUrl).openConnection()
        val reader = JsonReader(InputStreamReader(urlConnection.inputStream, "utf-8"))

        try {
            shape = parseShape(reader)
        } finally {
            reader.close()
        }

        return shape
    }

    @Throws(IOException::class, IllegalStateException::class)
    private fun parseShape(reader: JsonReader): List<GeoPoint> {
        val shape: MutableList<Pair<Int, GeoPoint>> = mutableListOf()

        reader.beginArray()
        while (!isCancelled && reader.hasNext()) {
            reader.beginObject()
            var shapeId: Int? = null
            var shapePtLat: Double? = null
            var shapePtLon: Double? = null
            var shapePtSequence: Int? = null
            while (reader.hasNext()) {
                val n = reader.nextName()
                when (n) {
                    "shape_id" -> shapeId = reader.nextInt()
                    "shape_pt_lat" -> shapePtLat = reader.nextDouble()
                    "shape_pt_lon" -> shapePtLon = reader.nextDouble()
                    "shape_pt_sequence" -> shapePtSequence = reader.nextInt()
                    else -> reader.skipValue()
                }
            }
            reader.endObject()
            if (shapeId == mShapeId && shapePtLat != null && shapePtLon != null && shapePtSequence != null) {
                shape.add(Pair(shapePtSequence, GeoPoint(shapePtLat, shapePtLon)))
            }
        }
        reader.endArray()

        DbAdapter().run {
            openReadWrite(mContext)
            replaceShape(mShapeId, shape)
            close()
        }

        return shape.sortedWith(compareBy({ it.first })).map { it.second }
    }

    companion object {
        private val SHAPES_URL = "buswhentwincities.herokuapp.com/shapes"
        private var mUseHttps = true
    }
}
