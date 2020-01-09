/*
    Copyright (C) 2019-2020 Sweetie Piggy Apps <sweetiepiggyapps@gmail.com>

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
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class BrowseDirectionsFragment : Fragment() {

    private var mRouteId: String? = null
    private val mDirections: MutableList<NexTrip.Direction> = ArrayList<NexTrip.Direction>()
    private lateinit var mAdapter: BrowseDirectionsAdapter
    private lateinit var mClickDirectionListener: BrowseDirectionsAdapter.OnClickDirectionListener
    private lateinit var mDownloadErrorListener: OnDownloadErrorListener
    private lateinit var mRefreshingListener: OnChangeRefreshingListener

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mClickDirectionListener = context as BrowseDirectionsAdapter.OnClickDirectionListener
        mDownloadErrorListener = context as OnDownloadErrorListener
        mRefreshingListener = context as OnChangeRefreshingListener
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.results_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        if (savedInstanceState == null) {
            arguments?.let { loadState(it) }
        } else {
            loadState(savedInstanceState)
        }

        mAdapter = BrowseDirectionsAdapter(mClickDirectionListener, context!!, mRouteId, mDirections)

        getActivity()?.findViewById<RecyclerView>(R.id.results_recycler_view)?.apply {
            layoutManager = LinearLayoutManager(context)
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            adapter = mAdapter
        }

        val model = activity?.run {
            ViewModelProvider(this, BrowseDirectionsViewModel.BrowseDirectionsViewModelFactory(mRouteId)).get(BrowseDirectionsViewModel::class.java)
        } ?: throw Exception("Invalid Activity")
        model.run {
            setDownloadErrorListener(mDownloadErrorListener)
            setChangeRefreshingListener(mRefreshingListener)
        }
        model.getDirections().observe(this, Observer<List<NexTrip.Direction>>{
            updateDirections(it)
        })
    }

    public override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)
        mRouteId?.let { savedInstanceState.putString(KEY_ROUTE_ID, it) }
    }

    private fun loadState(b: Bundle) {
        if (b.containsKey(KEY_ROUTE_ID)) {
            mRouteId = b.getString(KEY_ROUTE_ID)
        }
    }

    private fun updateDirections(directions: List<NexTrip.Direction>) {
        mDirections.apply {
            clear()
            addAll(directions)
        }
        mAdapter.notifyDataSetChanged()
    }

    companion object {
        fun newInstance() = BrowseDirectionsFragment()

        val KEY_ROUTE_ID = "routeId"
    }
}
