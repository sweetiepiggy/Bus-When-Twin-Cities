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
import android.widget.EditText
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton


class SearchStopIdFragment : Fragment() {
    private lateinit var mSearchStopIdListener: OnSearchStopIdListener

    companion object {
        fun newInstance() = SearchStopIdFragment()
    }

    interface OnSearchStopIdListener {
        fun onSearchStopId(stopId: Int)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mSearchStopIdListener = context as OnSearchStopIdListener
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.search_stop_id_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        // getActivity()?.findViewById<EditText>(R.id.stopIdEntry)
        //         ?.setOnEditorActionListener(object : TextView.OnEditorActionListener {
        //             override fun onEditorAction(v: TextView, actionId: Int, event: KeyEvent): Boolean {
        //                 if (actionId == EditorInfo.IME_ACTION_DONE) {
        //                     startStopIdActivity()
        //                     return true
        //                 } else {
        //                     return false
        //                 }
        //             }
        //         })

        val fab = getActivity()?.findViewById<FloatingActionButton>(R.id.fab)
        fab?.setOnClickListener { startStopIdActivity() }
    }

    private fun startStopIdActivity() {
        getActivity()?.findViewById<EditText>(R.id.stopIdEntry)?.let { stopIdEntry ->
            val stopIdStr = stopIdEntry.text.toString()
            if (stopIdStr.length == 0) {
                stopIdEntry.error = resources.getString(R.string.enter_stop_id)
            } else {
                try {
                    mSearchStopIdListener.onSearchStopId(stopIdStr.toInt())
                } catch (e: NumberFormatException) {
                    stopIdEntry.error = resources.getString(R.string.must_be_number)
                }
            }
        }
    }
}
