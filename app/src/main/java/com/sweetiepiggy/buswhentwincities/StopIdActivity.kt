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

import android.os.AsyncTask
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.Menu
import android.view.MenuItem

import java.util.ArrayList
import java.util.Calendar

class StopIdActivity : AppCompatActivity(), DownloadNexTripsTask.OnDownloadedListener {
    private var mResultsRecyclerView: RecyclerView? = null
    private var mAdapter: RecyclerView.Adapter<*>? = null
    private var mLayoutManager: RecyclerView.LayoutManager? = null
    private var mStopId: String? = null
    private var mNexTrips: MutableList<NexTrip>? = null
    private var mDownloadNexTripsTask: DownloadNexTripsTask? = null
    private var mLastUpdate: Long = 0
    private var mIsFavorite = false

    private val unixTime: Long
        get() = Calendar.getInstance().timeInMillis / 1000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stop_id)

        if (savedInstanceState == null) {
            val b = intent.extras
            if (b != null) {
                loadState(b)
            }
        } else {
            loadState(savedInstanceState)
        }

        val dbHelper = DbAdapter()
        dbHelper.open(this)
        mIsFavorite = dbHelper.isFavStop(mStopId)
        dbHelper.close()

        title = resources.getString(R.string.stop) + " #" + mStopId

        mResultsRecyclerView = findViewById<View>(R.id.results_recycler_view)

        mLayoutManager = LinearLayoutManager(this)
        mResultsRecyclerView!!.layoutManager = mLayoutManager
        mResultsRecyclerView!!.addItemDecoration(DividerItemDecoration(mResultsRecyclerView!!.context,
                DividerItemDecoration.VERTICAL))

        mNexTrips = ArrayList()
        mAdapter = StopIdAdapter(applicationContext, mNexTrips)
        mResultsRecyclerView!!.adapter = mAdapter

        mDownloadNexTripsTask = DownloadNexTripsTask(this, this, mStopId)
        mDownloadNexTripsTask!!.execute()

        // FloatingActionButton fab = findViewById(R.id.fab);
        // fab.setOnClickListener(new View.OnClickListener() {
        //      @Override
        //      public void onClick(View view) {
        //          if (mDownloadNexTripsTask.getStatus() == AsyncTask.Status.FINISHED) {
        //              mDownloadNexTripsTask = new DownloadNexTripsTask(StopIdActivity.this,
        //                                                               StopIdActivity.this,
        //                                                               mStopId);
        //              mDownloadNexTripsTask.execute();
        //          }
        //      }
        //  });
    }

    public override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)
        savedInstanceState.putString("stopId", mStopId)
    }

    private fun loadState(b: Bundle) {
        mStopId = b.getString("stopId")
    }

    public override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        loadState(savedInstanceState)
    }

    public override fun onDestroy() {
        if (mDownloadNexTripsTask != null) {
            mDownloadNexTripsTask!!.cancel(true)
        }
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_stop_id, menu)
        menu.findItem(R.id.action_favorite).icon = ContextCompat.getDrawable(this, if (mIsFavorite)
            android.R.drawable.btn_star_big_on
        else
            android.R.drawable.btn_star_big_off)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_refresh -> {
                if (mDownloadNexTripsTask!!.status == AsyncTask.Status.FINISHED && unixTime - mLastUpdate >= MIN_SECONDS_BETWEEN_REFRESH) {
                    mDownloadNexTripsTask = DownloadNexTripsTask(this, this, mStopId)
                    mDownloadNexTripsTask!!.execute()
                }
                return true
            }
            R.id.action_favorite -> {
                mIsFavorite = !mIsFavorite
                val dbHelper = DbAdapter()
                dbHelper.openReadWrite(this)
                if (mIsFavorite) {
                    dbHelper.createFavStop(mStopId, null)
                } else {
                    dbHelper.deleteFavStop(mStopId)
                }
                dbHelper.close()
                item.icon = ContextCompat.getDrawable(this, if (mIsFavorite)
                    android.R.drawable.btn_star_big_on
                else
                    android.R.drawable.btn_star_big_off)
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onDownloaded(nexTrips: List<NexTrip>) {
        mNexTrips!!.clear()
        mLastUpdate = unixTime
        mNexTrips!!.addAll(nexTrips)
        mAdapter!!.notifyDataSetChanged()
    }

    companion object {
        private val MIN_SECONDS_BETWEEN_REFRESH: Long = 30
    }
}

