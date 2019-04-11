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

class NexTripsViewModel(private val mStopId: Int?, private val mContext: Context) : ViewModel(), DownloadNexTripsTask.OnDownloadedListener, DownloadStopTask.OnDownloadedListener {
    private var mDownloadNexTripsTask: DownloadNexTripsTask? = null
    private var mDownloadStopTask: DownloadStopTask? = null
    private var mLoadNexTripsErrorListener: OnLoadNexTripsErrorListener? = null
    private var mLastUpdate: Long = 0
    private var mDbLastUpdate: Long = 0
    private var mDbNexTrips: List<NexTrip>? = null
    private var mLoadingNexTrips: Boolean = false

    private val mNexTrips: MutableLiveData<List<NexTrip>> by lazy {
        MutableLiveData<List<NexTrip>>().also { loadNexTrips() }
    }

    private val mDoShowRoutes: MutableLiveData<Map<Pair<String?, String?>, Boolean>> by lazy {
        MutableLiveData<Map<Pair<String?, String?>, Boolean>>().also {
            LoadDoShowRoutesTask().execute()
        }
    }

    private val mStop: MutableLiveData<Stop> by lazy {
        MutableLiveData<Stop>().also { LoadStopTask().execute() }
    }

    private val unixTime: Long
        get() = Calendar.getInstance().timeInMillis / 1000L

    fun getNexTrips(): LiveData<List<NexTrip>> = mNexTrips

    fun getDoShowRoutes(): LiveData<Map<Pair<String?, String?>, Boolean>> = mDoShowRoutes
    fun setDoShowRoutes(doShowRoutes: Map<Pair<String?, String?>, Boolean>) {
        mDoShowRoutes.value = doShowRoutes
    }

    fun getStop(): LiveData<Stop> = mStop

    interface OnLoadNexTripsErrorListener {
        fun onLoadNexTripsError(err: DownloadNexTripsTask.DownloadError)
    }

    fun loadNexTrips() {
        if (!mLoadingNexTrips) mStopId?.let { stopId ->
            mLoadingNexTrips = true
            val downloadNextTripsTask = mDownloadNexTripsTask
            if (mLastUpdate == 0L) {
                // this is the first time we're loading nexTrips,
                // reload from the database if it is fresh, otherwise download
                InitLoadNexTripsTask().execute()
            } else if ((downloadNextTripsTask == null ||
            			downloadNextTripsTask.status == AsyncTask.Status.FINISHED) &&
		    			unixTime - mLastUpdate >= MIN_SECONDS_BETWEEN_REFRESH) {
                // refresh displayed times now in case internet connection is slow
                mNexTrips.value?.let { mNexTrips.value = it }

                // start a new download task if there is no currently running task and
                // it's been as at least MIN_SECONDS_BETWEEN_REFRESH since the last download
                mDownloadNexTripsTask = DownloadNexTripsTask(this, stopId)
                mDownloadNexTripsTask!!.execute()
            } else {
                // too soon to download again, just refresh the displayed times
                mNexTrips.value = mNexTrips.value?.let {
                    filterOldNexTrips(it, unixTime, mLastUpdate)
                } ?: listOf()
                mLoadingNexTrips = false
            }
        }
    }

    override fun onDownloaded(nexTrips: List<NexTrip>) {
        mLastUpdate = unixTime
        android.util.Log.d("abc", "got here: set mNexTrips from onDownloaded()")
        mNexTrips.value = nexTrips
        StoreNexTripsInDbTask(nexTrips).execute()
        mLoadingNexTrips = false
    }

    override fun onDownloadError(err: DownloadNexTripsTask.DownloadError) {
        if (mLastUpdate == 0L) {
            val dbNexTrips = mDbNexTrips
            // fall back to nexTrips from database if they exist
            if (dbNexTrips != null) {
                mLastUpdate = mDbLastUpdate
        	    mNexTrips.value = filterOldNexTrips(dbNexTrips, unixTime, mLastUpdate)
            } else {
                mNexTrips.value = listOf()
            }
        } else mNexTrips.value?.let { nexTrips ->
        	mNexTrips.value = filterOldNexTrips(nexTrips, unixTime, mLastUpdate)
        }
        mLoadNexTripsErrorListener?.onLoadNexTripsError(err)
        mLoadingNexTrips = false
    }

    override fun onCleared() {
        mDownloadNexTripsTask?.cancel(true)
        mDownloadStopTask?.cancel(true)
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
                    open(mContext)
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
            mDbNexTrips = result?.let { filterOldNexTrips(it, unixTime, mDbLastUpdate) } ?: null

            // display the database results now in case internet connection is slow
            if (!mDbNexTrips.isNullOrEmpty()) {
                android.util.Log.d("abc", "got here: set mNexTrips from InitLoadNexTripsTask()")
                mNexTrips.value = mDbNexTrips
                mLastUpdate = mDbLastUpdate
            }

            // download nexTrips if no or not-fresh results in database
            if (result == null || unixTime - mDbLastUpdate >= MIN_SECONDS_BETWEEN_REFRESH) {
                mStopId?.let { stopId ->
                    android.util.Log.d("abc", "got here: downloading from InitLoadNexTripsTask()")
                    mDownloadNexTripsTask = DownloadNexTripsTask(this@NexTripsViewModel, stopId)
                    mDownloadNexTripsTask!!.execute()
                }
            // show results from database if they exist and are fresh
            } else {
                // mNexTrips.value = filterOldNexTrips(result, unixTime, mLastUpdate)
                mLoadingNexTrips = false
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

    private inner class LoadDoShowRoutesTask(): AsyncTask<Void, Void, Map<Pair<String?, String?>, Boolean>>() {
        override fun doInBackground(vararg params: Void): Map<Pair<String?, String?>, Boolean> {
            var doShowRoutes: Map<Pair<String?, String?>, Boolean> = mapOf()
        	mStopId?.let { stopId ->
                DbAdapter().run {
                    open(mContext)
                    doShowRoutes = getDoShowRoutes(stopId)
                    close()
                }
            }
            return doShowRoutes
        }

        override fun onPostExecute(result: Map<Pair<String?, String?>, Boolean>) {
            mDoShowRoutes.value = result
        }
    }

    private inner class LoadStopTask(): AsyncTask<Void, Void, Stop?>() {
        override fun doInBackground(vararg params: Void): Stop? {
            var stop: Stop? = null
            mStopId?.let { stopId ->
                DbAdapter().apply {
                    open(mContext)
                    stop = getStop(stopId)
                    close()
                }
            }
            return stop
        }

        override fun onPostExecute(result: Stop?) {
            if (result == null) {
                mStopId?.let { stopId ->
                    mDownloadStopTask = DownloadStopTask(this@NexTripsViewModel, stopId)
                    mDownloadStopTask!!.execute()
                }
            } else {
                mStop.value = result
            }
        }
    }

    override fun onDownloaded(stop: Stop) {
        mStop.value = stop
        StoreStopInDbTask(stop).execute()
    }

    private inner class StoreStopInDbTask(private val stop: Stop): AsyncTask<Void, Void, Void>() {
        override fun doInBackground(vararg params: Void): Void? {
            DbAdapter().run {
                openReadWrite(mContext)
                updateStop(stop)
                close()
            }
            return null
        }

        override fun onPostExecute(result: Void?) { }
    }

    companion object {
        private val MIN_SECONDS_BETWEEN_REFRESH: Long = 30
        // don't display NexTrips that were due this long or more before now
        private val SECONDS_BEFORE_NOW_TO_IGNORE = 120
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
