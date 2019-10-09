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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class BrowseTimestopsViewModel(private val mRouteId: String?, private val mDirectionId: Int?) : ViewModel(), DownloadTimestopsTask.OnDownloadedTimestopsListener {
    private var mDownloadErrorListener: OnDownloadErrorListener? = null
    private var mRefreshingListener: OnChangeRefreshingListener? = null

    data class Timestop(val description: String, val timestopId: String)

    private val mTimestops: MutableLiveData<List<Timestop>> by lazy {
        MutableLiveData<List<Timestop>>().also { loadTimestops() }
    }

    fun getTimestops(): LiveData<List<Timestop>> = mTimestops

    fun setDownloadErrorListener(downloadErrorListener: OnDownloadErrorListener) {
        mDownloadErrorListener = downloadErrorListener
    }

    fun setChangeRefreshingListener(refreshingListener: OnChangeRefreshingListener) {
        mRefreshingListener = refreshingListener
    }

    private fun loadTimestops() {
        mRefreshingListener?.setRefreshing(true)
        mRouteId?.let { routeId ->
            NexTrip.Direction.from(mDirectionId)?.let { direction ->
                DownloadTimestopsTask(this, routeId, direction).execute()
            }
        }
    }

    override fun onDownloadedTimestops(timestops: List<Timestop>) {
        mTimestops.value = timestops
        mRefreshingListener?.setRefreshing(false)
    }

    override fun onDownloadedTimestopsError(err: MetroTransitDownloader.DownloadError) {
        mRefreshingListener?.setRefreshing(false)
        mDownloadErrorListener?.onDownloadError(err)
    }

    class BrowseTimestopsViewModelFactory(private val routeId: String?, private val directionId: Int?) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            BrowseTimestopsViewModel(routeId, directionId) as T
    }
}
