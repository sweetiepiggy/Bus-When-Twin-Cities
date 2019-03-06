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

import android.app.FragmentTransaction.TRANSIT_FRAGMENT_OPEN
import android.os.AsyncTask
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentTransaction.TRANSIT_FRAGMENT_CLOSE
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.*

class StopIdActivity : AppCompatActivity(), DownloadNexTripsTask.OnDownloadedListener, StopIdAdapter.OnClickMapListener {
    private lateinit var mResultsRecyclerView: RecyclerView
    private lateinit var mAdapter: RecyclerView.Adapter<*>
    private lateinit var mLayoutManager: RecyclerView.LayoutManager
    private var mStopId: String? = null
    private var mStopDesc: String? = null
    private lateinit var mNexTrips: MutableList<NexTrip>
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

            if (findViewById<View>(R.id.container_by_side) != null) {
                val mapFragment = MyMapFragment.newInstance()
                supportFragmentManager.beginTransaction()
            	        .add(R.id.container_by_side, mapFragment)
                        .commit()
            }
        } else {
            loadState(savedInstanceState)
        }

        val dbHelper = DbAdapter()
        dbHelper.open(this)
        val stopId = mStopId
        mIsFavorite = if (stopId != null) dbHelper.isFavStop(stopId) else false
        mStopDesc = stopId?.let { dbHelper.getStopDesc(it) }
        dbHelper.close()

        val stopDesc = mStopDesc
        title = resources.getString(R.string.stop) + " #" + mStopId +
        	(if (stopDesc != null && !stopDesc.isEmpty()) " ($stopDesc)" else "")

        mResultsRecyclerView = findViewById<RecyclerView>(R.id.results_recycler_view)

        mLayoutManager = LinearLayoutManager(this)
        mResultsRecyclerView.layoutManager = mLayoutManager
        mResultsRecyclerView.addItemDecoration(DividerItemDecoration(mResultsRecyclerView.context,
                DividerItemDecoration.VERTICAL))

        mNexTrips = ArrayList<NexTrip>()
        mAdapter = StopIdAdapter(applicationContext, mNexTrips, this)
        mResultsRecyclerView.adapter = mAdapter

        if (stopId != null) {
            mDownloadNexTripsTask = DownloadNexTripsTask(this, this, stopId)
            mDownloadNexTripsTask?.execute()
        }
    }

    public override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)
        savedInstanceState.putString(KEY_STOP_ID, mStopId)
    }

    private fun loadState(b: Bundle) {
        mStopId = b.getString(KEY_STOP_ID)
    }

    public override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        loadState(savedInstanceState)
    }

    public override fun onDestroy() {
        mDownloadNexTripsTask?.cancel(true)
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_stop_id, menu)
        menu.findItem(R.id.action_favorite).icon = ContextCompat.getDrawable(this,
        	if (mIsFavorite) IS_FAV_ICON else IS_NOT_FAV_ICON)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_refresh -> {
                val stopId = mStopId
                if (stopId != null && mDownloadNexTripsTask!!.status == AsyncTask.Status.FINISHED &&
                		unixTime - mLastUpdate >= MIN_SECONDS_BETWEEN_REFRESH) {
                    mDownloadNexTripsTask = DownloadNexTripsTask(this, this, stopId)
                    mDownloadNexTripsTask!!.execute()
                }
                return true
            }
            R.id.action_favorite -> {
                if (mIsFavorite) {
                    mStopId?.let { stopId ->
                        val dbHelper = DbAdapter()
                        dbHelper.openReadWrite(this)
                        dbHelper.deleteFavStop(stopId)
                        dbHelper.close()
                    }
                    item.icon = ContextCompat.getDrawable(this, IS_NOT_FAV_ICON)
                    mIsFavorite = false
                } else {
                    val builder = AlertDialog.Builder(this)
                    val favStopIdDialog = layoutInflater.inflate(R.layout.dialog_fav_stop_id, null)
                    builder.setView(favStopIdDialog)
                    builder.setPositiveButton(android.R.string.ok) { _, _ ->
                        val stopName = favStopIdDialog.findViewById<EditText>(R.id.stop_name)?.text.toString()
                        mStopId?.let { stopId ->
                            val dbHelper = DbAdapter()
                            dbHelper.openReadWrite(this)
                            dbHelper.createFavStop(stopId, stopName)
                            dbHelper.close()
                        }
                        item.icon = ContextCompat.getDrawable(this, IS_FAV_ICON)
                        mIsFavorite = true
                    }
                    builder.setNegativeButton(android.R.string.cancel) { _, _ -> }
                    builder.setTitle(R.string.enter_stop_name_dialog_title)
                    builder.show()
                }

                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onDownloaded(nexTrips: List<NexTrip>) {
        mNexTrips.clear()
        mLastUpdate = unixTime
        mNexTrips.addAll(nexTrips)
        mAdapter.notifyDataSetChanged()
    }

    override fun onClickMap(nexTrip: NexTrip) {
        val b = Bundle()
        b.putString("routeAndTerminal", nexTrip.route + nexTrip.terminal)
        b.putString("departureText", nexTrip.departureText)
        b.putDouble("vehicleLatitude", nexTrip.vehicleLatitude)
        b.putDouble("vehicleLongitude", nexTrip.vehicleLongitude)
        val mapFragment = supportFragmentManager.findFragmentById(R.id.container_by_side) as MyMapFragment?
        if (mapFragment == null) {
            val f = MyMapFragment.newInstance()
            f.setArguments(b)
            supportFragmentManager.beginTransaction()
            		.replace(R.id.container, f)
                    .setTransition(TRANSIT_FRAGMENT_OPEN)
            		.addToBackStack(null)
            		.commit()
        } else {
            mapFragment.updateVehicle(b)
        }
    }

    companion object {
        private val MIN_SECONDS_BETWEEN_REFRESH: Long = 30
        private val IS_FAV_ICON = R.drawable.ic_baseline_favorite_24px
        private val IS_NOT_FAV_ICON = R.drawable.ic_baseline_favorite_border_24px
        private val KEY_STOP_ID = "stopId"
    }
}

