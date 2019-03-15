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
import java.util.*

class NexTripsFragment : Fragment() {
    private lateinit var mClickMapListener: StopIdAdapter.OnClickMapListener
    private lateinit var mAdapter: StopIdAdapter
    private lateinit var mResultsRecyclerView: RecyclerView
    private var mNexTrips: List<PresentableNexTrip> = ArrayList<PresentableNexTrip>()

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
        return v
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val model = activity?.run {
            ViewModelProviders.of(this).get(NexTripsViewModel::class.java)
        } ?: throw Exception("Invalid Activity")
        model.getNexTrips().observe(this, Observer<List<NexTrip>>{ updateNexTrips(it) })

        mResultsRecyclerView.layoutManager = LinearLayoutManager(context)
        mResultsRecyclerView.addItemDecoration(DividerItemDecoration(mResultsRecyclerView.context,
        		DividerItemDecoration.VERTICAL))
        mAdapter = StopIdAdapter(context!!)
        mResultsRecyclerView.adapter = mAdapter
        mAdapter.setOnClickMapListener(mClickMapListener)
    }

    fun updateNexTrips(nexTrips: List<NexTrip>) {
        val timeInMillis = Calendar.getInstance().timeInMillis
        val newNexTrips = nexTrips.map { PresentableNexTrip(it, timeInMillis, context!!) }

        val nexTripChanges = NexTripChange.getNexTripChanges(mNexTrips, newNexTrips)

        mNexTrips = newNexTrips
        mAdapter.setNexTrips(newNexTrips)

            nexTripChanges.forEach {
                when (it) {
                    is NexTripChange.ItemInserted      -> android.util.Log.d("abc", "got here: inserted ${it.pos}")
                    is NexTripChange.ItemMoved         -> android.util.Log.d("abc", "got here: moved ${it.fromPos} ${it.toPos}")
                    is NexTripChange.ItemRangeInserted -> android.util.Log.d("abc", "got here: rangeinserted ${it.posStart} ${it.itemCount}")
                    is NexTripChange.ItemRangeRemoved  -> android.util.Log.d("abc", "got here: rangeremoved ${it.posStart} ${it.itemCount}")
                    is NexTripChange.ItemRemoved       -> android.util.Log.d("abc", "got here: removed ${it.pos}")
                    is NexTripChange.ItemChanged       -> android.util.Log.d("abc", "got here: changed ${it.pos}")
                    is NexTripChange.ItemRangeChanged  -> android.util.Log.d("abc", "got here: rangechanged ${it.posStart} ${it.itemCount}")
                }
            }

       mAdapter.apply {
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

//        for ((newNexTrip, newIdx) in newNexTrips.listIterator().withIndex()) {
//            val oldItr = mNexTrips.listIterator().withIndex()
//        }

//        for ((oldNexTrip, oldIdx) in mNexTrips.listIterator().withIndex()) {
//            val newItr = newNexTrips.listIterator().withIndex()
//            var found = false
//            while (!found && newItr.hasNext()) {
//                val newNexTrip = newItr.next().value
//                found = newNexTrip.blockNumber == oldNexTrip.blockNumber
//            }
//            if (found) {
//                val newIdx = 
//            } else {
//            }
//        }

//    	val oldItr = mNexTrips.listIterator()
//    	val newItr = newNexTrips.listIterator()
//        var oldIdx = 0
//
//        while (oldItr.hasNext() && newItr.hasNext()) {
//            var oldNexTrip = oldItr.next()
//            var newNexTrip = newItr.next()
//            var done = false
//
//            while (!done && oldNexTrip.blockNumber == newNexTrip.blockNumber) {
//                if (!oldNexTrip.equals(newNexTrip)) {
//                    mAdapter.notifyItemChanged(oldIdx)
//                }
//                done = !oldIdx.hasNext() || !newItr.hasNext()
//                oldNexTrip = oldItr.next()
//                newNexTrip = newItr.next()
//                oldIdx += 1
//            }
//
//            oldIdx += 1
//        }

//        mNexTrips.clear()
//        mNexTrips.addAll(newNexTrips)
        // mAdapter.notifyDataSetChanged()

        val noResultsView = activity?.findViewById<View>(R.id.no_results_textview)
        if (nexTrips.isEmpty()) {
            mResultsRecyclerView.setVisibility(View.GONE)
            noResultsView?.setVisibility(View.VISIBLE)
        } else {
            noResultsView?.setVisibility(View.GONE)
            mResultsRecyclerView.setVisibility(View.VISIBLE)
        }
    }

//        val oldItr = mNexTrips.listIterator()
//        val newItr = newNexTrips.listIterator()
//        var cnt = 0
//
//        while (oldItr.hasNext() && newItr.hasNext()) {
//            var oldNexTrip = oldItr.next()
//            var newNexTrip = newItr.next()
//
//            // these nexTrips no longer exist
//            while (oldNexTrip.blockNumber != newNexTrip.blockNumber &&
//            		oldNexTrip.departureTimeInMillis < newNexTrip.departureTimeInMillis) {
//                oldItr.remove()
//                oldNexTrip = oldItr.next()
//                cnt += 1
//            }
//            mAdapter.notifyItemRangeRemoved(0, cnt)
//            cnt = 0
//
//            // these nexTrips are new
//            while (oldNexTrip.blockNumber != newNexTrip.blockNumber &&
//            		newNexTrip.departureTimeInMillis < oldNexTrip.departureTimeInMillis) {
//                oldI
//            }
//        }
}
