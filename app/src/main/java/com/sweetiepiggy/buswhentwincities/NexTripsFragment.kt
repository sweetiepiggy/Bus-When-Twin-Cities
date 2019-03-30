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
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import java.util.*

class NexTripsFragment : Fragment() {
    private lateinit var mClickMapListener: StopIdAdapter.OnClickMapListener
    private lateinit var mAdapter: StopIdAdapter
    private lateinit var mResultsRecyclerView: RecyclerView
    private lateinit var mSwipeRefreshLayout: SwipeRefreshLayout
    private var mNexTrips: List<PresentableNexTrip> = listOf()
    private var mDoShowRoutes: Map<String?, Boolean> = mapOf()

    companion object {
        fun newInstance(): NexTripsFragment = NexTripsFragment()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mClickMapListener = context as StopIdAdapter.OnClickMapListener
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.nextrips_fragment, container, false)
		mResultsRecyclerView = v.findViewById<RecyclerView>(R.id.results_recycler_view)!!
        mSwipeRefreshLayout = v.findViewById<SwipeRefreshLayout>(R.id.swiperefresh)!!
        return v
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val model = activity?.run {
            ViewModelProviders.of(this).get(NexTripsViewModel::class.java)
        } ?: throw Exception("Invalid Activity")
        model.getNexTrips().observe(this, Observer<List<NexTrip>>{ updateNexTrips(it) })

        mResultsRecyclerView.layoutManager = LinearLayoutManager(context)
        mAdapter = StopIdAdapter(context!!)
        mAdapter.setDoShowRoutes(mDoShowRoutes)
        mResultsRecyclerView.adapter = mAdapter
        mAdapter.setOnClickMapListener(mClickMapListener)
        mSwipeRefreshLayout.setOnRefreshListener { model.loadNexTrips() }
        model.getDoShowRoutes().observe(this, Observer<Map<String?, Boolean>>{ updateDoShowRoutes(it) })
    }

    fun updateNexTrips(nexTrips: List<NexTrip>) {
        val timeInMillis = Calendar.getInstance().timeInMillis
        val newNexTrips = nexTrips.map { PresentableNexTrip(it, timeInMillis, context!!) }

        val nexTripChanges = NexTripChange.getNexTripChanges(mNexTrips, newNexTrips, mDoShowRoutes)

        mNexTrips = newNexTrips
        mAdapter.setNexTrips(newNexTrips)
        notifyAdapter(nexTripChanges)

        mSwipeRefreshLayout.setRefreshing(false)
        activity?.findViewById<View>(R.id.progressBar)?.setVisibility(View.INVISIBLE)

        val noResultsView = activity?.findViewById<View>(R.id.no_results_textview)
        if (nexTrips.isEmpty()) {
            mSwipeRefreshLayout.setVisibility(View.GONE)
            noResultsView?.setVisibility(View.VISIBLE)
        } else {
            noResultsView?.setVisibility(View.GONE)
            mSwipeRefreshLayout.setVisibility(View.VISIBLE)
        }
    }

    fun updateDoShowRoutes(doShowRoutes: Map<String?, Boolean>) {
        mDoShowRoutes = doShowRoutes
        mAdapter.setDoShowRoutes(doShowRoutes)
    }

    fun setRefreshing(refreshing: Boolean) {
        activity?.findViewById<View>(R.id.progressBar)?.setVisibility(
            if (refreshing) View.VISIBLE else View.INVISIBLE
        )
    }

    fun onChangeHiddenRoutes(changedRoutes: Set<String>) {
        val itemChanges = mutableListOf<NexTripChange.ItemChanged>()
        for ((idx, nexTrip) in mNexTrips.listIterator().withIndex()) {
            if (changedRoutes.contains(nexTrip.routeAndTerminal)) {
                itemChanges.add(NexTripChange.ItemChanged(idx))
            }
        }

        notifyAdapter(NexTripChange.groupChanges(itemChanges))
    }

    private fun notifyAdapter(nexTripChanges: List<NexTripChange>) =
        mAdapter.run {
            nexTripChanges.forEach {
                when (it) {
                    is NexTripChange.ItemInserted      -> notifyItemInserted(it.pos)
                    is NexTripChange.ItemMoved         -> notifyItemMoved(it.fromPos, it.toPos)
                    is NexTripChange.ItemRangeInserted -> notifyItemRangeInserted(it.posStart, it.itemCount)
                    is NexTripChange.ItemRangeRemoved  -> notifyItemRangeRemoved(it.posStart, it.itemCount)
                    is NexTripChange.ItemRemoved       -> notifyItemRemoved(it.pos)
                    is NexTripChange.ItemChanged       -> notifyItemChanged(it.pos)
                    is NexTripChange.ItemRangeChanged  -> notifyItemRangeChanged(it.posStart, it.itemCount)
                }
            }
        }
}
