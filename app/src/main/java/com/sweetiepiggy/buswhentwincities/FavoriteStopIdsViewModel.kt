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
    private val mFavoriteStops: MutableLiveData<List<FavoriteStop>> by lazy {
        MutableLiveData<List<FavoriteStop>>().also {
            loadFavoriteStops()
        }
    }

    fun getFavoriteStops(): LiveData<List<FavoriteStop>> = mFavoriteStops

    fun loadFavoriteStops() {
        LoadFavoriteStopsTask().execute()
    }

    fun setFavoriteStops(favoriteStops: List<FavoriteStop>) {
            mFavoriteStops.value = favoriteStops
    }

    sealed class FavoriteStop {
        data class FavoriteStopId(val stopId: Int, val stopDesc: String, val position: Int): FavoriteStop()
        data class FavoriteTimestop(val timestop: Timestop, val stopDesc: String, val position: Int): FavoriteStop()

        companion object {
            fun stopId(f: FavoriteStop): Int? =
                when (f) {
                    is FavoriteStopId -> f.stopId
                    is FavoriteTimestop -> null
                }

            fun stopDesc(f: FavoriteStop): String =
                when (f) {
                    is FavoriteStopId -> f.stopDesc
                    is FavoriteTimestop -> f.stopDesc
                }

            fun position(f: FavoriteStop): Int =
                when (f) {
                    is FavoriteStopId -> f.position
                    is FavoriteTimestop -> f.position
                }
        }
    }

    private inner class LoadFavoriteStopsTask() : AsyncTask<Void, Void, List<FavoriteStop>>() {
        override fun doInBackground(vararg params: Void): List<FavoriteStop> {
            val dbHelper = DbAdapter()
            dbHelper.open(getApplication())
            val favoriteStops = ArrayList<FavoriteStop>()

            val c = dbHelper.fetchFavStops()
            val stopIdIndex = c.getColumnIndex(DbAdapter.KEY_STOP_ID)
            val stopDescIndex = c.getColumnIndex(DbAdapter.KEY_STOP_DESCRIPTION)
            val positionIndex = c.getColumnIndex(DbAdapter.KEY_POSITION)
            while (c.moveToNext()) {
                favoriteStops.add(FavoriteStop.FavoriteStopId(
                    c.getInt(stopIdIndex),
                    c.getString(stopDescIndex),
                    c.getInt(positionIndex)
                ))
            }
            c.close()

            val c1 = dbHelper.fetchFavTimestops()
            val timestopIdIndex = c1.getColumnIndex(DbAdapter.KEY_TIMESTOP_ID)
            val routeIdIndex = c1.getColumnIndex(DbAdapter.KEY_ROUTE)
            val directionIdIndex = c1.getColumnIndex(DbAdapter.KEY_ROUTE_DIRECTION)
            val stopDescIndex1 = c1.getColumnIndex(DbAdapter.KEY_STOP_DESCRIPTION)
            val positionIndex1 = c1.getColumnIndex(DbAdapter.KEY_POSITION)
            while (c1.moveToNext()) {
                val timestopId = c1.getString(timestopIdIndex)
                val routeId = c1.getString(routeIdIndex)
                val direction = NexTrip.Direction.from(c1.getInt(directionIdIndex))
                val stopDesc = c1.getString(stopDescIndex1)
                val position = c1.getInt(positionIndex1)
                if (timestopId != null && routeId != null && direction != null) {
                    favoriteStops.add(FavoriteStop.FavoriteTimestop(
                        Timestop(timestopId, routeId, direction), stopDesc, position))
                }
            }
            c1.close()

            dbHelper.close()
            return favoriteStops.sortedWith(compareBy({ -FavoriteStop.position(it) }))
        }

        override fun onPostExecute(result: List<FavoriteStop>) {
            setFavoriteStops(result)
        }
    }
}
