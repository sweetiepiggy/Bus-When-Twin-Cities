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
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class BrowseRoutesFragment : Fragment() {

    companion object {
        fun newInstance(clickRouteListener: BrowseRoutesAdapter.OnClickRouteListener,
                        downloadErrorListener: OnDownloadErrorListener,
                        refreshingListener: OnChangeRefreshingListener) =
            BrowseRoutesFragment().apply {
                setClickRouteListener(clickRouteListener)
                setDownloadErrorListener(downloadErrorListener)
                setChangeRefreshingListener(refreshingListener)
            }
    }

    private val mRoutes: MutableList<BrowseRoutesViewModel.Route> = ArrayList<BrowseRoutesViewModel.Route>()
    private lateinit var mAdapter: BrowseRoutesAdapter
    private lateinit var mClickRouteListener: BrowseRoutesAdapter.OnClickRouteListener
    private lateinit var mDownloadErrorListener: OnDownloadErrorListener
    private lateinit var mRefreshingListener: OnChangeRefreshingListener

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.results_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        mAdapter = BrowseRoutesAdapter(mClickRouteListener, mRoutes)

        activity?.findViewById<RecyclerView>(R.id.results_recycler_view)?.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = mAdapter
        }

        val model = activity?.run {
            ViewModelProvider(this).get(BrowseRoutesViewModel::class.java)
        } ?: throw Exception("Invalid Activity")
        model.run {
            setDownloadErrorListener(mDownloadErrorListener)
            setChangeRefreshingListener(mRefreshingListener)
        }
        model.getRoutes().observe(this, Observer<List<BrowseRoutesViewModel.Route>>{
            updateRoutes(it)
        })
    }

    fun setClickRouteListener(clickRouteListener: BrowseRoutesAdapter.OnClickRouteListener) {
        mClickRouteListener = clickRouteListener
    }

    fun setDownloadErrorListener(downloadErrorListener: OnDownloadErrorListener) {
        mDownloadErrorListener = downloadErrorListener
    }

    fun setChangeRefreshingListener(refreshingListener: OnChangeRefreshingListener) {
        mRefreshingListener = refreshingListener
    }

    private fun updateRoutes(routes: List<BrowseRoutesViewModel.Route>) {
        mRoutes.apply {
            clear()
            addAll(routes)
        }
        mAdapter.notifyDataSetChanged()
    }
}
