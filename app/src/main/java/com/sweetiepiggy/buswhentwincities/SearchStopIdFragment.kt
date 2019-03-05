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

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import com.google.android.material.floatingactionbutton.FloatingActionButton


class SearchStopIdFragment : Fragment() {

    companion object {
        fun newInstance() = SearchStopIdFragment()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.search_stop_id_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        if (savedInstanceState != null) {
            restoreSavedState(savedInstanceState)
        }

        getActivity()?.findViewById<EditText>(R.id.stopIdEntry)
                ?.setOnEditorActionListener(object : TextView.OnEditorActionListener {
                    override fun onEditorAction(v: TextView, actionId: Int, event: KeyEvent): Boolean {
                        if (actionId == EditorInfo.IME_ACTION_DONE) {
                            startStopIdActivity()
                            return true
                        } else {
                            return false
                        }
                    }
                })

        val fab = getActivity()?.findViewById<FloatingActionButton>(R.id.fab)
        fab?.setOnClickListener { startStopIdActivity() }
    }

    public override fun onSaveInstanceState(savedInstanceState: Bundle) {
        savedInstanceState.putString("stopId",
    		getActivity()?.findViewById<EditText>(R.id.stopIdEntry)?.text.toString())
        super.onSaveInstanceState(savedInstanceState)
    }

    private fun restoreSavedState(savedInstanceState: Bundle) {
        val stopId = savedInstanceState.getString("stopId")
        getActivity()?.findViewById<EditText>(R.id.stopIdEntry)?.setText(stopId)
    }

    private fun startStopIdActivity() {
        val stopIdEntry = getActivity()?.findViewById<EditText>(R.id.stopIdEntry)
        if (stopIdEntry != null) {
            val stopId = stopIdEntry.text.toString()
            if (stopId.length == 0) {
                stopIdEntry.error = resources.getString(R.string.enter_stop_id)
            } else {
                val intent = Intent(getActivity()?.getApplicationContext(), StopIdActivity::class.java)
                val b = Bundle()
                b.putString("stopId", stopId)
                intent.putExtras(b)
                startActivity(intent)
            }
        }
    }
}
