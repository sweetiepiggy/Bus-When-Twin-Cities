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

class BrowseRoutesViewModel : ViewModel() {
    data class Route(val description: String, val providerId: Int, val route: Int)

    private val mRoutes: MutableLiveData<List<Route>> by lazy {
        MutableLiveData<List<Route>>().also { loadRoutes() }
    }

    fun getRoutes(): LiveData<List<Route>> = mRoutes

    private fun loadRoutes() {
        LoadRoutesTask().execute()
    }

    private inner class LoadRoutesTask() : AsyncTask<Void, Void, List<Route>>() {
        override fun doInBackground(vararg params: Void): List<Route> {
            return listOf(
                Route("METRO Blue Line", 8, 901),
                Route("METRO Green Line", 8, 902),
                Route("METRO Red Line", 8, 903),
                Route("METRO A Line", 8, 921),
                Route("METRO C Line", 8, 923),
                Route("Northstar Commuter Rail", 8, 888),
                Route("2 - Franklin Av - Riverside Av - U of M - 8th St SE", 8, 2),
                Route("3 - U of M - Como Ave - Energy Park Dr - Maryland Av", 8, 3),
                Route("4 - New Brighton - Johnson St - Bryant Av - Southtown", 8, 4)
            )
        }

        override fun onPostExecute(result: List<Route>) {
            mRoutes.value = result
        }
    }
}
