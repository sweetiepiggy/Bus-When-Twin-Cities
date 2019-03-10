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

import android.app.Application
import android.os.AsyncTask
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class FavoriteStopIdsViewModel(application: Application) : AndroidViewModel(application) {
    private val mFavoriteStopIds: MutableLiveData<List<FavoriteStopId>> by lazy {
        MutableLiveData<List<FavoriteStopId>>().also {
            loadFavoriteStopIds()
        }
    }

    fun getFavoriteStopIds(): LiveData<List<FavoriteStopId>> = mFavoriteStopIds

    fun loadFavoriteStopIds() {
        LoadFavoriteStopIdsTask().execute()
    }

    data class FavoriteStopId(val stopId: String, val stopDesc: String)

    private inner class LoadFavoriteStopIdsTask() : AsyncTask<Void, Void, List<FavoriteStopId>>() {
        override fun doInBackground(vararg params: Void): List<FavoriteStopId> {
            val dbHelper = DbAdapter()
            dbHelper.open(getApplication())
            val favoriteStopIds = ArrayList<FavoriteStopId>()
            val c = dbHelper.fetchFavStops()
            val stopIdIndex = c.getColumnIndex(DbAdapter.KEY_STOP_ID)
            val stopDescIndex = c.getColumnIndex(DbAdapter.KEY_STOP_DESCRIPTION)
            while (c.moveToNext()) {
                favoriteStopIds.add(FavoriteStopId(c.getString(stopIdIndex), c.getString(stopDescIndex)))
            }
            c.close()
            dbHelper.close()
            return favoriteStopIds
        }

        override fun onPostExecute(result: List<FavoriteStopId>) {
            mFavoriteStopIds.value = result
        }
    }
}
