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

import android.app.Application
import android.os.AsyncTask
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class StopSearchHistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val mSearchedStops: MutableLiveData<List<SearchedStop>> by lazy {
        MutableLiveData<List<SearchedStop>>().also {
            loadSearchedStops()
        }
    }

    fun getSearchedStops(): LiveData<List<SearchedStop>> = mSearchedStops

    fun loadSearchedStops() {
        LoadSearchedStopsTask().execute()
    }

    fun setSearchedStops(searchedStops: List<SearchedStop>) {
            mSearchedStops.value = searchedStops
    }

    data class SearchedStop(val stopId: Int, val searchDatetime : Int)

    private inner class LoadSearchedStopsTask() : AsyncTask<Void, Void, List<SearchedStop>>() {
        override fun doInBackground(vararg params: Void): List<SearchedStop> {
            val dbHelper = DbAdapter()
            dbHelper.open(getApplication())
            val searchedStops = ArrayList<SearchedStop>()

            val c = dbHelper.fetchStopSearchHistory()
            val stopIdIndex = c.getColumnIndex(DbAdapter.KEY_STOP_SEARCH_ID)
            val searchDatetimeIndex = c.getColumnIndex(DbAdapter.KEY_STOP_SEARCH_DATETIME)
            while (c.moveToNext()) {
                searchedStops.add(SearchedStop(
                    c.getInt(stopIdIndex),
                    c.getInt(searchDatetimeIndex)
                ))
            }
            c.close()

            dbHelper.close()
            return searchedStops.sortedWith(compareBy({ -it.searchDatetime }))
        }

        override fun onPostExecute(result: List<SearchedStop>) {
            setSearchedStops(result)
        }
    }
}
