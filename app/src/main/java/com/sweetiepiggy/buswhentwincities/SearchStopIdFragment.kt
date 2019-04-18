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
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout


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

        activity?.findViewById<TextInputEditText>(R.id.stopIdEntry)?.setOnEditorActionListener { _, actionId, _ ->
            return@setOnEditorActionListener when (actionId) {
                EditorInfo.IME_ACTION_SEARCH -> {
                    startStopIdActivity()
                    true
                }
                else -> false
            }
        }
        activity?.findViewById<TextInputEditText>(R.id.stopIdEntry)?.let { stopIdEntry ->
            stopIdEntry.setOnFocusChangeListener(object : View.OnFocusChangeListener {
                override fun onFocusChange(v: View, hasFocus: Boolean) =
            	stopIdEntry.setHint(if (hasFocus) resources.getString(R.string.stop_id_hint) else "")
            })
        }

        val fab = activity?.findViewById<FloatingActionButton>(R.id.fab)
        fab?.setOnClickListener { startStopIdActivity() }
    }

    private fun startStopIdActivity() {
        getActivity()?.findViewById<TextInputLayout>(R.id.stopIdTextInput)?.let { stopIdTextInput ->
            val stopIdStr = stopIdTextInput.editText?.text.toString()
            if (stopIdStr.length == 0) {
                stopIdTextInput.error = resources.getString(R.string.enter_stop_id)
            } else {
                try {
                    val stopId = stopIdStr.toInt()
                    stopIdTextInput.error = null
                    mSearchStopIdListener.onSearchStopId(stopId)
                } catch (e: NumberFormatException) {
                    stopIdTextInput.error = resources.getString(R.string.must_be_number)
                }
            }
        }
    }
}
