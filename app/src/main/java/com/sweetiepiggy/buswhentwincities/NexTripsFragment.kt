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


class NexTripsFragment : Fragment() {
    private var mClickMapListener: StopIdAdapter.OnClickMapListener? = null
    private var mAdapter: StopIdAdapter? = null
    private lateinit var mLayoutManager: RecyclerView.LayoutManager
    private var mNexTrips: MutableList<NexTrip> = ArrayList<NexTrip>()
    private lateinit var mModel: NexTripsViewModel

    companion object {
        fun newInstance(): NexTripsFragment = NexTripsFragment()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        android.util.Log.d("abc", "got here4: setOnClickMapListener: mAdapter is null == " + (mAdapter == null).toString())
        mClickMapListener = context as StopIdAdapter.OnClickMapListener
        mAdapter?.setOnClickMapListener(mClickMapListener!!)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mModel = activity?.run {
            ViewModelProviders.of(this).get(NexTripsViewModel::class.java)
        } ?: throw Exception("Invalid Activity")
        android.util.Log.d("abc", "NexTripsFragment observing")
        mModel.getNexTrips().observe(this, Observer<List<NexTrip>>{ updateNexTrips(it) })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.nextrips_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        mLayoutManager = LinearLayoutManager(getContext())
        android.util.Log.d("abc", "got here5: onActivityCreated")

        getActivity()?.findViewById<RecyclerView>(R.id.results_recycler_view)?.let { resultsRecyclerView ->
            resultsRecyclerView.layoutManager = mLayoutManager
            resultsRecyclerView.addItemDecoration(DividerItemDecoration(resultsRecyclerView.context,
            	    DividerItemDecoration.VERTICAL))
            android.util.Log.d("abc", "got here: mNexTrips.isEmpty() = " + mNexTrips.isEmpty())
            android.util.Log.d("abc", "got here6: context is null == " + (getContext() == null).toString())
            mAdapter = getContext()?.let { StopIdAdapter(it, mNexTrips) }
            android.util.Log.d("abc", "got here7: mAdapter is null == " + (mAdapter == null).toString())
            resultsRecyclerView.adapter = mAdapter
            mClickMapListener?.let { mAdapter!!.setOnClickMapListener(it) }
        }
    }

    public override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)
        android.util.Log.d("abc", "got here: saving fragment instance state")
    }

    fun updateNexTrips(nexTrips: List<NexTrip>) {
        android.util.Log.d("abc", "got here2: nexTrips.isEmpty() = " + nexTrips.isEmpty())
        mNexTrips.clear()
        mNexTrips.addAll(nexTrips)
        android.util.Log.d("abc", "got here3: mNexTrips.isEmpty() = " + mNexTrips.isEmpty())
        mAdapter?.notifyDataSetChanged()

        val resultsRecyclerView = getActivity()?.findViewById<RecyclerView>(R.id.results_recycler_view)
        val noResultsView = getActivity()?.findViewById<View>(R.id.no_results_textview)
        if (nexTrips.isEmpty()) {
            resultsRecyclerView?.setVisibility(View.GONE)
            noResultsView?.setVisibility(View.VISIBLE)
        } else {
            noResultsView?.setVisibility(View.GONE)
            resultsRecyclerView?.setVisibility(View.VISIBLE)
        }
    }
}
