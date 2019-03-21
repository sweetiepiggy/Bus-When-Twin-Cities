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
import android.os.AsyncTask
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import java.util.*

class NexTripsViewModel(private val mStopId: Int?, private val mContext: Context) : ViewModel(), DownloadNexTripsTask.OnDownloadedListener {
    private var mDownloadNexTripsTask: DownloadNexTripsTask? = null
    private var mLoadNexTripsErrorListener: OnLoadNexTripsErrorListener? = null
    private var mLastUpdate: Long = 0
    private var mDbLastUpdate: Long = 0
    private var mDbNexTrips: List<NexTrip>? = null

    private val mNexTrips: MutableLiveData<List<NexTrip>> by lazy {
        MutableLiveData<List<NexTrip>>().also {
            loadNexTrips()
        }
    }

    val hiddenRoutes: MutableSet<String> = mutableSetOf()

    private val unixTime: Long
        get() = Calendar.getInstance().timeInMillis / 1000L

    fun getNexTrips(): LiveData<List<NexTrip>> = mNexTrips

    interface OnLoadNexTripsErrorListener {
        fun onLoadNexTripsError(err: DownloadNexTripsTask.DownloadError)
    }

    fun loadNexTrips() {
        mStopId?.let { stopId ->
            val downloadNextTripsTask = mDownloadNexTripsTask
            if (mLastUpdate == 0L) {
                // this is the first time we're loading nexTrips,
                // reload from the database if it is fresh, otherwise download
                InitLoadNexTripsTask().execute()
            } else if ((downloadNextTripsTask == null ||
            			downloadNextTripsTask.status == AsyncTask.Status.FINISHED) &&
		    			unixTime - mLastUpdate >= MIN_SECONDS_BETWEEN_REFRESH) {
                // start a new download task if there is no currently running task and
                // it's been as at least MIN_SECONDS_BETWEEN_REFRESH since the last download
                mDownloadNexTripsTask = DownloadNexTripsTask(this, stopId)
                mDownloadNexTripsTask!!.execute()
            } else {
                // too soon to download again, just refresh the displayed times
                mNexTrips.value = mNexTrips.value?.let {
                    filterOldNexTrips(it, unixTime, mLastUpdate)
                } ?: ArrayList<NexTrip>()
            }
        }
    }

    override fun onDownloaded(nexTrips: List<NexTrip>) {
        mLastUpdate = unixTime
        mNexTrips.value = nexTrips
        StoreNexTripsInDbTask(nexTrips).execute()
    }

    override fun onDownloadError(err: DownloadNexTripsTask.DownloadError) {
        if (mLastUpdate == 0L) {
            // fall back to nexTrips from database if they exist
            if (mDbNexTrips != null) {
                mLastUpdate = mDbLastUpdate
                mNexTrips.value = mDbNexTrips
            } else {
                mNexTrips.value = ArrayList<NexTrip>()
            }
        } else mNexTrips.value?.let { nexTrips ->
        	mNexTrips.value = filterOldNexTrips(nexTrips, unixTime, mLastUpdate)
        }
        mLoadNexTripsErrorListener?.onLoadNexTripsError(err)
    }

    override fun onCleared() {
        mDownloadNexTripsTask?.cancel(true)
        super.onCleared()
    }

    fun setLoadNexTripsErrorListener(loadNexTripsErrorListener: OnLoadNexTripsErrorListener) {
        mLoadNexTripsErrorListener = loadNexTripsErrorListener
    }

    class NexTripsViewModelFactory(private val stopId: Int?, private val context: Context) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            NexTripsViewModel(stopId, context) as T
    }

    private inner class InitLoadNexTripsTask(): AsyncTask<Void, Void, List<NexTrip>?>() {
        override fun doInBackground(vararg params: Void): List<NexTrip>? {
            var nexTrips: List<NexTrip>? = null
            mStopId?.let { stopId ->
                DbAdapter().apply {
                    openReadWrite(mContext)
                    getLastUpdate(stopId)?.let { lastUpdate ->
                        mDbLastUpdate = lastUpdate
                        val suppressLocations = unixTime - lastUpdate >= SECONDS_BEFORE_SUPPRESS_LOCATIONS
                        nexTrips = getNexTrips(stopId, SECONDS_BEFORE_NOW_TO_IGNORE, suppressLocations)
                    }
                    close()
                }
            }
            return nexTrips
        }

        override fun onPostExecute(result: List<NexTrip>?) {
            mDbNexTrips = result

            // download nexTrips if no or not-fresh results in database
            if (result == null || unixTime - mDbLastUpdate >= MIN_SECONDS_BETWEEN_REFRESH) {
                mStopId?.let { stopId ->
                    mDownloadNexTripsTask = DownloadNexTripsTask(this@NexTripsViewModel, stopId)
                    mDownloadNexTripsTask!!.execute()
                }
            // show results from database if they exist and are fresh
            } else if (result != null) {
                mNexTrips.value = result
            }
        }
    }

    private inner class StoreNexTripsInDbTask(private val nexTrips: List<NexTrip>): AsyncTask<Void, Void, Void>() {
        override fun doInBackground(vararg params: Void): Void? {
        	mStopId?.let { stopId ->
                DbAdapter().run {
                    openReadWrite(mContext)
                    updateNexTrips(stopId, nexTrips, mLastUpdate)
                    close()
                }
            }
            return null
        }

        override fun onPostExecute(result: Void?) { }
    }

    companion object {
        private val MIN_SECONDS_BETWEEN_REFRESH: Long = 30
        // don't display NexTrips that were due a minute or more before now
        private val SECONDS_BEFORE_NOW_TO_IGNORE = 60
        private val SECONDS_BEFORE_SUPPRESS_LOCATIONS = 30

        private fun filterOldNexTrips(nexTrips: List<NexTrip>, curTime: Long, lastUpdate: Long): List<NexTrip> {
            val results: MutableList<NexTrip> = mutableListOf()
            val ignoreCutoffTime = curTime - SECONDS_BEFORE_NOW_TO_IGNORE
            val suppressLocations = (curTime - lastUpdate) >= SECONDS_BEFORE_SUPPRESS_LOCATIONS

            for (nexTrip in nexTrips) {
                if (nexTrip.departureTimeInMillis != null &&
                		nexTrip.departureTimeInMillis / 1000 >= ignoreCutoffTime) {
                    results.add(if (suppressLocations)
                		NexTrip.suppressLocation(nexTrip)
                	else
                		nexTrip)
                }
            }

            return results
        }
    }
}
