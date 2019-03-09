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

package com.sweetiepiggy.buswhentwincities.ui.favoritestopids

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sweetiepiggy.buswhentwincities.DbAdapter
import com.sweetiepiggy.buswhentwincities.FavoriteStopIdsAdapter
import com.sweetiepiggy.buswhentwincities.R
import java.util.*

class FavoriteStopIdsFragment : Fragment() {

    companion object {
        fun newInstance() = FavoriteStopIdsFragment()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.favorite_stop_ids_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val favoritesRecyclerView = getActivity()?.findViewById<RecyclerView>(R.id.favoritesRecyclerView) as RecyclerView
        val context = getActivity()?.getApplicationContext()

        favoritesRecyclerView.layoutManager = LinearLayoutManager(context)
        favoritesRecyclerView.addItemDecoration(DividerItemDecoration(favoritesRecyclerView.context,
                DividerItemDecoration.VERTICAL))

        if (context != null) {
            val dbHelper = DbAdapter()
            dbHelper.open(context)
            val favoriteStopIds = ArrayList<Pair<String, String>>()
            val c = dbHelper.fetchFavStops()
            val stopIdIndex = c.getColumnIndex(DbAdapter.KEY_STOP_ID)
            val stopDescIndex = c.getColumnIndex(DbAdapter.KEY_STOP_DESCRIPTION)
            while (c.moveToNext()) {
                favoriteStopIds.add(Pair(c.getString(stopIdIndex), c.getString(stopDescIndex)))
            }
            c.close()
            dbHelper.close()
            favoritesRecyclerView.adapter = FavoriteStopIdsAdapter(context, favoriteStopIds)
        }
    }

}
