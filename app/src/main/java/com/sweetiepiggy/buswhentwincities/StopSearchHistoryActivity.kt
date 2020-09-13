/*
    Copyright (C) 2020 Sweetie Piggy Apps <sweetiepiggyapps@gmail.com>

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
import android.os.AsyncTask
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class StopSearchHistoryActivity : AppCompatActivity(), StopSearchHistoryAdapter.OnClickListener {
    private lateinit var mAdapter: StopSearchHistoryAdapter
    private lateinit var mModel: StopSearchHistoryViewModel
    private var mDoUpdateHistory = true
    private val mHistory: MutableList<StopSearchHistoryViewModel.SearchedStop> = ArrayList<StopSearchHistoryViewModel.SearchedStop>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stop_search_history)
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        mAdapter = StopSearchHistoryAdapter(this, mHistory)
        findViewById<RecyclerView>(R.id.historyRecyclerView)?.apply {
            layoutManager = LinearLayoutManager(context)
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            mAdapter.attachToRecyclerView(this)
            adapter = mAdapter
        }

        mModel = ViewModelProvider(this).get(StopSearchHistoryViewModel::class.java)

        mModel.getSearchedStops().observe(this,
            Observer<List<StopSearchHistoryViewModel.SearchedStop>>{ updateHistory(it) })
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_history, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        when (item.itemId) {
            R.id.action_clear_history -> {
                AlertDialog.Builder(this).apply {
                    setTitle(resources.getString(R.string.clear_search_history))
                    setPositiveButton(resources.getString(R.string.clear)) { _, _ ->
                        object : AsyncTask<Void, Void, Void>() {
                            override fun doInBackground(vararg params: Void): Void? {
                                DbAdapter().apply {
                                    openReadWrite(this@StopSearchHistoryActivity)
                                    clearStopSearchHistory()
                                    close()
                                }
                                return null
                            }
                            override fun onPostExecute(result: Void?) {
                                mDoUpdateHistory = true
                                updateHistory(listOf())
                            }
                        }.execute()
                    }
                    setNegativeButton(android.R.string.cancel) { _, _ -> }
                }.show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }

    override fun onClick(stop: StopSearchHistoryViewModel.SearchedStop) {
        val b = Bundle().apply {
            putInt(StopIdActivity.KEY_STOP_ID, stop.stopId)
        }
        val intent = Intent(this, StopIdActivity::class.java).apply {
            putExtras(b)
        }
        startActivity(intent)
    }

    override fun onDelete(removedStop: StopSearchHistoryViewModel.SearchedStop, position: Int, recyclerViewPosition: Int) {
        object : AsyncTask<Void, Void, Void>() {
            override fun doInBackground(vararg params: Void): Void? {
                DbAdapter().apply {
                    openReadWrite(this@StopSearchHistoryActivity)
                    deleteStopSearchHistory(removedStop.stopId)
                    close()
                }
                return null
            }
            override fun onPostExecute(result: Void?) {
                // mModel.setSearchedStops(mSearchedStops)
            }
        }.execute()
    }

    override fun onContextItemSelected(item: MenuItem): Boolean =
        when (item.itemId) {
            StopSearchHistoryAdapter.ACTION_REMOVE -> {
                val position = item.order
                val removedStop = mHistory.removeAt(position)
                mAdapter.notifyItemRemoved(position)
                onDelete(removedStop, mHistory.size - position, position)
                true
            }
            else -> super.onContextItemSelected(item)
        }

    private fun updateHistory(history: List<StopSearchHistoryViewModel.SearchedStop>) {
        if (mDoUpdateHistory) {
            findViewById<View>(R.id.progressBar)?.setVisibility(View.INVISIBLE)
            mHistory.apply {
                clear()
                addAll(history)
            }
            mAdapter.notifyDataSetChanged()
            mDoUpdateHistory = false
        }
        updateHistoryMessage()
    }

    fun updateHistoryMessage() {
        val historyRecyclerView = findViewById<View>(R.id.historyRecyclerView)
        val noHistoryView = findViewById<View>(R.id.no_history_textview)
        if (mHistory.isEmpty()) {
            historyRecyclerView?.setVisibility(View.INVISIBLE)
            noHistoryView?.setVisibility(View.VISIBLE)
        } else {
            noHistoryView?.setVisibility(View.INVISIBLE)
            historyRecyclerView?.setVisibility(View.VISIBLE)
        }
    }
}
