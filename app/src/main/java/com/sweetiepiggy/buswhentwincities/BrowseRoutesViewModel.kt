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
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class BrowseRoutesViewModel : ViewModel(), DownloadRoutesTask.OnDownloadedRoutesListener {
    private var mDownloadErrorListener: OnDownloadErrorListener? = null
    private var mRefreshingListener: OnChangeRefreshingListener? = null

    data class Route(val description: String, val providerId: Int, val routeId: String)

    private val mRoutes: MutableLiveData<List<Route>> by lazy {
        MutableLiveData<List<Route>>().also { loadRoutes() }
    }

    fun getRoutes(): LiveData<List<Route>> = mRoutes

    fun setDownloadErrorListener(downloadErrorListener: OnDownloadErrorListener) {
        mDownloadErrorListener = downloadErrorListener
    }

    fun setChangeRefreshingListener(refreshingListener: OnChangeRefreshingListener) {
        mRefreshingListener = refreshingListener
    }

    private fun loadRoutes() {
        mRefreshingListener?.setRefreshing(true)
        DownloadRoutesTask(this).execute()
    }

    override fun onDownloadedRoutes(routes: List<Route>) {
        mRoutes.value = routes
        mRefreshingListener?.setRefreshing(false)
    }

    override fun onDownloadedRoutesError(err: MetroTransitDownloader.DownloadError) {
        mRefreshingListener?.setRefreshing(false)
        mDownloadErrorListener?.onDownloadError(err)
    }
}

