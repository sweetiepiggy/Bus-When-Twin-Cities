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
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import java.util.*

class NexTripsViewModel(private val mStopId: String?) : ViewModel(), DownloadNexTripsTask.OnDownloadedListener {
    private var mDownloadNexTripsTask: DownloadNexTripsTask? = null
    private var mLoadNexTripsErrorListener: OnLoadNexTripsErrorListener? = null
    private var mLastUpdate: Long = 0

    private val mNexTrips: MutableLiveData<List<NexTrip>> by lazy {
        MutableLiveData<List<NexTrip>>().also {
            loadNexTrips()
        }
    }

    private val unixTime: Long
        get() = Calendar.getInstance().timeInMillis / 1000L

    fun getNexTrips(): LiveData<List<NexTrip>> = mNexTrips

    interface OnLoadNexTripsErrorListener {
        fun onLoadNexTripsError(err: DownloadNexTripsTask.DownloadError)
    }

    fun loadNexTrips() {
        val downloadNextTripsTask = mDownloadNexTripsTask
        if (downloadNextTripsTask != null) {
            if (downloadNextTripsTask.status == AsyncTask.Status.FINISHED &&
		            unixTime - mLastUpdate >= MIN_SECONDS_BETWEEN_REFRESH) {
                mStopId?.let { stopId ->
                    mDownloadNexTripsTask = DownloadNexTripsTask(this, stopId)
                    mDownloadNexTripsTask?.execute()
                }
            } else {
                mNexTrips.value = mNexTrips.value ?: ArrayList<NexTrip>()
            }
        } else {
            mStopId?.let { stopId ->
                mDownloadNexTripsTask = DownloadNexTripsTask(this, stopId)
                mDownloadNexTripsTask!!.execute()
            }
        }
    }

    override fun onDownloaded(nexTrips: List<NexTrip>) {
        mLastUpdate = unixTime
        mNexTrips.value = nexTrips
    }

    override fun onDownloadError(err: DownloadNexTripsTask.DownloadError) {
        if (mLastUpdate == 0L) mNexTrips.value = ArrayList<NexTrip>()
        mLoadNexTripsErrorListener?.onLoadNexTripsError(err)
    }

    override fun onCleared() {
        mDownloadNexTripsTask?.cancel(true)
        super.onCleared()
    }

    fun setLoadNexTripsErrorListener(loadNexTripsErrorListener: OnLoadNexTripsErrorListener) {
        mLoadNexTripsErrorListener = loadNexTripsErrorListener
    }

    class NexTripsViewModelFactory(private val stopId: String?) : ViewModelProvider.NewInstanceFactory() {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return NexTripsViewModel(stopId) as T
        }
    }

    companion object {
        private val MIN_SECONDS_BETWEEN_REFRESH: Long = 30
    }
}
