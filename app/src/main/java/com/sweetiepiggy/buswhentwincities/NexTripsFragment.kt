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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView


class NexTripsFragment : Fragment() {
    private var mClickMapListener: StopIdAdapter.OnClickMapListener? = null
    private var mAdapter: StopIdAdapter? = null
    private var mResultsRecyclerView: RecyclerView? = null
    private var mNoResultsView: View? = null
    private lateinit var mLayoutManager: RecyclerView.LayoutManager
    private var mNexTrips: MutableList<NexTrip> = ArrayList<NexTrip>()

    companion object {
        fun newInstance() = NexTripsFragment()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.nextrips_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        mResultsRecyclerView = getActivity()?.findViewById<RecyclerView>(R.id.results_recycler_view)
        mNoResultsView = getActivity()?.findViewById<View>(R.id.no_results_textview)

        val context = getActivity()?.getApplicationContext()
        mLayoutManager = LinearLayoutManager(context)
        android.util.Log.d("abc", "got here5: onActivityCreated")

        mResultsRecyclerView?.let { resultsRecyclerView ->
            resultsRecyclerView.layoutManager = mLayoutManager
            resultsRecyclerView.addItemDecoration(DividerItemDecoration(resultsRecyclerView.context,
            	    DividerItemDecoration.VERTICAL))
            android.util.Log.d("abc", "got here: mNexTrips.isEmpty() = " + mNexTrips.isEmpty())
            android.util.Log.d("abc", "got here6: context is null == " + (context == null).toString())
            mAdapter = context?.let { StopIdAdapter(it, mNexTrips) }
            android.util.Log.d("abc", "got here7: mAdapter is null == " + (mAdapter == null).toString())
            resultsRecyclerView.adapter = mAdapter
            mClickMapListener?.let { mAdapter!!.setOnClickMapListener(it) }
        }
    }

    fun setOnClickMapListener(clickMapListener: StopIdAdapter.OnClickMapListener) {
        android.util.Log.d("abc", "got here4: setOnClickMapListener: mAdapter is null == " + (mAdapter == null).toString())
        mAdapter?.setOnClickMapListener(clickMapListener)
        mClickMapListener = clickMapListener
    }

    fun updateNexTrips(nexTrips: List<NexTrip>) {
        android.util.Log.d("abc", "got here2: nexTrips.isEmpty() = " + nexTrips.isEmpty())
        if (mNexTrips == null) {
            mNexTrips = nexTrips.toMutableList()
        } else {
            mNexTrips.clear()
            mNexTrips.addAll(nexTrips)
            android.util.Log.d("abc", "got here3: mNexTrips.isEmpty() = " + mNexTrips.isEmpty())
            mAdapter?.notifyDataSetChanged()
        }

        if (nexTrips.isEmpty()) {
            mResultsRecyclerView?.setVisibility(View.GONE)
            mNoResultsView?.setVisibility(View.VISIBLE)
        } else {
            mNoResultsView?.setVisibility(View.GONE)
            mResultsRecyclerView?.setVisibility(View.VISIBLE)
        }
    }
}
