/*
    Copyright (C) 2019-2021 Sweetie Piggy Apps <sweetiepiggyapps@gmail.com>

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
import com.google.android.gms.maps.model.LatLng
import java.util.*

class NexTripsViewModel(private val mStopId: Int?, private val mTimestop: Timestop?, private val mContext: Context) : ViewModel(), DownloadNexTripsTask.OnDownloadedNexTripsListener, DownloadShapeIdTask.OnDownloadedShapeIdListener, DownloadShapeTask.OnDownloadedShapeListener {
    private var mDownloadNexTripsTask: AsyncTask<Void, Int, Void>? = null
    private var mLoadNexTripsErrorListener: OnDownloadErrorListener? = null
    private var mRefreshingListener: OnChangeRefreshingListener? = null
    private var mLastUpdate: Long = 0
    private var mDbLastUpdate: Long = 0
    private var mDbNexTrips: List<NexTrip>? = null
    private var mLoadingNexTrips: Boolean = false
    private var mNexTripsLoaded: Boolean = false
    private var mShapesLoaded: Boolean = false
    /** set of shapeIds */
    private val mFindingShapeFor: MutableSet<Int> = mutableSetOf()

    private val mNexTrips: MutableLiveData<List<NexTrip>> by lazy {
        MutableLiveData<List<NexTrip>>().also { loadNexTrips() }
    }

    private val mStop: MutableLiveData<Stop> by lazy {
        MutableLiveData<Stop>()// .also { loadNexTrips() }
    }

    private val mDoShowRoutes: MutableLiveData<Map<Pair<String?, String?>, Boolean>> by lazy {
        MutableLiveData<Map<Pair<String?, String?>, Boolean>>().also {
            LoadDoShowRoutesTask().execute()
        }
    }

    /** map from shapeId to shape */
    private val mShapes: MutableLiveData<Map<Int, List<LatLng>>> by lazy {
        MutableLiveData<Map<Int, List<LatLng>>>().also { LoadShapesTask().execute() }
    }

    private val unixTime: Long
        get() = Calendar.getInstance().timeInMillis / 1000L

    fun getNexTrips(): LiveData<List<NexTrip>> = mNexTrips
    fun nexTripsLoaded() = mNexTripsLoaded

    fun getStop(): LiveData<Stop> = mStop

    fun getDoShowRoutes(): LiveData<Map<Pair<String?, String?>, Boolean>> = mDoShowRoutes
    fun setDoShowRoutes(doShowRoutes: Map<Pair<String?, String?>, Boolean>) {
        mDoShowRoutes.value = doShowRoutes
    }

    fun getShapes(): LiveData<Map<Int, List<LatLng>>> = mShapes
    fun findShapeId(nexTrip: NexTrip) {
        DownloadShapeIdTask(this, nexTrip, mStopId).execute()
    }
    fun findShape(shapeId: Int) {
        if (mShapesLoaded && !(mShapes.value?.contains(shapeId) ?: false) && !mFindingShapeFor.contains(shapeId)) {
            mFindingShapeFor.add(shapeId)
            LoadShapeTask(shapeId).execute()
        }
    }

    fun loadNexTrips() {
        val stopId = mStopId
        val timestop = mTimestop
        if (!mLoadingNexTrips && (stopId != null || timestop != null)) {
            mLoadingNexTrips = true
            mRefreshingListener?.setRefreshing(true)
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
                if (stopId != null) {
                    mDownloadNexTripsTask = DownloadNexTripsTask(this, stopId)
                } else if (timestop != null) {
                    mDownloadNexTripsTask = DownloadTimepointDeparturesTask(this, timestop.routeId, timestop.directionId, timestop.timestopId)
                }
                mDownloadNexTripsTask!!.execute()
            } else {
                mNexTripsLoaded = true
                // too soon to download again, just refresh the displayed times
                mNexTrips.value = mNexTrips.value?.let {
                    filterOldNexTrips(it, unixTime, mLastUpdate)
                } ?: listOf()
                mLoadingNexTrips = false
                mRefreshingListener?.setRefreshing(false)
            }
        }
    }

    override fun onDownloadedNexTrips(nexTrips: List<NexTrip>) {
        mLastUpdate = unixTime
        val oldNexTrips = mNexTrips.value
        val newNexTrips = nexTrips.map { nexTrip ->
            if (nexTrip.shapeId == null) {
                val oldNexTrip = oldNexTrips?.find { it.tripId == nexTrip.tripId }
                if (oldNexTrip != null && oldNexTrip.shapeId != null) {
                    NexTrip.setShapeId(nexTrip, oldNexTrip.shapeId)
                } else {
                    nexTrip
                }
            } else {
                nexTrip
            }
        }
        mNexTripsLoaded = true
        mNexTrips.value = newNexTrips
        mRefreshingListener?.setRefreshing(false)
        StoreNexTripsInDbTask(newNexTrips).execute()
        mLoadingNexTrips = false
    }

    override fun onDownloadedStop(stop: Stop) {
        mStop.value = stop
    }

    override fun onDownloadedNexTripsError(err: MetroTransitDownloader.DownloadError) {
        mNexTripsLoaded = true
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
        mLoadNexTripsErrorListener?.onDownloadError(err)
        mLoadingNexTrips = false
        mRefreshingListener?.setRefreshing(false)
    }

    override fun onCleared() {
        mDownloadNexTripsTask?.cancel(true)
        super.onCleared()
    }

    fun setLoadNexTripsErrorListener(loadNexTripsErrorListener: OnDownloadErrorListener) {
        mLoadNexTripsErrorListener = loadNexTripsErrorListener
    }

    fun setChangeRefreshingListener(refreshingListener: OnChangeRefreshingListener) {
        mRefreshingListener = refreshingListener
    }

    class NexTripsViewModelFactory(private val stopId: Int?, private val timestop: Timestop?, private val context: Context) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            NexTripsViewModel(stopId, timestop, context) as T
    }

    private inner class InitLoadNexTripsTask(): AsyncTask<Void, Void, List<NexTrip>?>() {
        override fun doInBackground(vararg params: Void): List<NexTrip>? {
            var nexTrips: List<NexTrip>? = null
            val stopId = mStopId
            val timestop = mTimestop
            if (stopId != null || timestop != null) {
                DbAdapter().apply {
                    open(mContext)
                    val lastUpdate = if (stopId != null) {
                        getLastUpdate(stopId)
                    } else {
                        getLastTimestopUpdate(timestop!!.timestopId, timestop.routeId, timestop.directionId)
                    }
                    if (lastUpdate != null) {
                        mDbLastUpdate = lastUpdate
                        val suppressLocations = unixTime - lastUpdate >= SECONDS_BEFORE_SUPPRESS_LOCATIONS
                        nexTrips = if (stopId != null) {
                            getNexTrips(stopId, SECONDS_BEFORE_NOW_TO_IGNORE, suppressLocations)
                        } else {
                            getTimestopNexTrips(timestop!!.timestopId, timestop.routeId,
                                    timestop.directionId, SECONDS_BEFORE_NOW_TO_IGNORE, suppressLocations)
                        }
                    }
                    close()
                }
            }
            return nexTrips
        }

        override fun onPostExecute(result: List<NexTrip>?) {
            val curTime = unixTime
            mDbNexTrips = result?.let { filterOldNexTrips(it, curTime, mDbLastUpdate) }
            val doDownload = (result == null || curTime - mDbLastUpdate >= MIN_SECONDS_BETWEEN_REFRESH)

            // display the database results now in case internet connection is slow
            if (!mDbNexTrips.isNullOrEmpty()) {
                if (!doDownload) {
                    mNexTripsLoaded = true
                }
                mNexTrips.value = mDbNexTrips
                mLastUpdate = mDbLastUpdate
            }

            // download nexTrips if no or not-fresh results in database
            if (doDownload) {
                val stopId = mStopId
                val timestop = mTimestop
                if (stopId != null || timestop != null) {
                    if (stopId != null) {
                        mDownloadNexTripsTask = DownloadNexTripsTask(this@NexTripsViewModel, stopId)
                    } else if (timestop != null) {
                        mDownloadNexTripsTask =DownloadTimepointDeparturesTask(this@NexTripsViewModel,
                                timestop.routeId, timestop.directionId, timestop.timestopId)
                    }
                    mDownloadNexTripsTask!!.execute()
                }
            // show results from database if they exist and are fresh
            } else {
                mNexTripsLoaded = true
                // mNexTrips.value = filterOldNexTrips(result, unixTime, mLastUpdate)

                // actually, we already showed database results above if they
                // existed, so only show them here if they are empty
                if (mDbNexTrips.isNullOrEmpty()) {
                    mNexTrips.value = mDbNexTrips
                    mLastUpdate = mDbLastUpdate
                }
                mLoadingNexTrips = false
                mRefreshingListener?.setRefreshing(false)
            }
        }
    }

    private inner class StoreNexTripsInDbTask(private val nexTrips: List<NexTrip>): AsyncTask<Void, Void, Void>() {
        override fun doInBackground(vararg params: Void): Void? {
            val stopId = mStopId
            val timestop = mTimestop
            if (stopId != null || timestop != null) {
                DbAdapter().run {
                    openReadWrite(mContext)
                    if (stopId != null) {
                        updateNexTrips(stopId, nexTrips, mLastUpdate)
                    } else {
                        updateTimestopNexTrips(timestop!!.timestopId, timestop.routeId, timestop.directionId, nexTrips, mLastUpdate)
                    }
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
            val stopId = mStopId
            val timestop = mTimestop
            if (stopId != null || timestop != null) {
                DbAdapter().run {
                    open(mContext)
                    doShowRoutes = if (stopId != null) {
                        getDoShowRoutes(stopId)
                    } else {
                        getTimestopDoShowRoutes(timestop!!.timestopId, timestop.routeId, timestop.directionId)
                    }
                    close()
                }
            }
            return doShowRoutes
        }

        override fun onPostExecute(result: Map<Pair<String?, String?>, Boolean>) {
            mDoShowRoutes.value = result
        }
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

    private inner class LoadShapesTask(): AsyncTask<Void, Void, Map<Int, List<LatLng>>>() {
        override fun doInBackground(vararg params: Void): Map<Int, List<LatLng>> {
            val shapes: MutableMap<Int, List<LatLng>> = mutableMapOf()
            DbAdapter().apply {
                open(mContext)
                val shapeIds = mNexTrips.value?.filter {
                    it.shapeId != null
                }?.map {
                    it.shapeId!!
                }?.toSet() ?: setOf()

                for (shapeId in shapeIds) {
                        val shape = getShape(shapeId)
                        if (!shape.isEmpty()) {
                            shapes[shapeId] = shape
                        }
                }
                close()
            }
            return shapes
        }

        override fun onPostExecute(shapes: Map<Int, List<LatLng>>) {
            mShapes.value = shapes
            mShapesLoaded = true
        }
    }

    private inner class LoadShapeTask(private val shapeId: Int): AsyncTask<Void, Void, List<LatLng>>() {
        override fun doInBackground(vararg params: Void): List<LatLng> {
            var shape: List<LatLng>
            DbAdapter().apply {
                open(mContext)
                shape = getShape(shapeId)
                close()
            }
            return shape
        }

        override fun onPostExecute(shape: List<LatLng>) {
            if (!shape.isEmpty()) {
                mShapes.value = (mShapes.value ?: mapOf()) + Pair(shapeId, shape)
                // mFindingShapeFor.remove(shapeId)
            } else {
                DownloadShapeTask(this@NexTripsViewModel, mContext, shapeId).execute()
            }
        }
    }

    override fun onDownloadedShapeId(nexTrip: NexTrip, shapeId: Int) {
        mNexTrips.value?.let { oldNexTrips ->
            val newNexTrips = oldNexTrips.map { oldNexTrip ->
                if (nexTrip.tripId == oldNexTrip.tripId) {
                    NexTrip.setShapeId(oldNexTrip, shapeId)
                } else {
                    oldNexTrip
                }
            }
            mNexTrips.value = newNexTrips
            StoreNexTripsInDbTask(newNexTrips).execute()
        }
    }

    override fun onDownloadedShape(shapeId: Int, shape: List<LatLng>) {
        mShapes.value = (mShapes.value ?: mapOf()) + Pair(shapeId, shape)
    }

    companion object {
        private val MIN_SECONDS_BETWEEN_REFRESH: Long = 15
        // don't display NexTrips that were due this long or more before now
        private val SECONDS_BEFORE_NOW_TO_IGNORE = 60
        private val SECONDS_BEFORE_SUPPRESS_LOCATIONS = 30

        private fun filterOldNexTrips(nexTrips: List<NexTrip>, curTime: Long, lastUpdate: Long): List<NexTrip> {
            val results: MutableList<NexTrip> = mutableListOf()
            val ignoreCutoffTime = curTime - SECONDS_BEFORE_NOW_TO_IGNORE
            val suppressLocations = (curTime - lastUpdate) >= SECONDS_BEFORE_SUPPRESS_LOCATIONS

            for (nexTrip in nexTrips) {
                if (nexTrip.departureTime != null &&
                        nexTrip.departureTime >= ignoreCutoffTime) {
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
