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
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.ActionBar.NAVIGATION_MODE_TABS
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.fragment.app.FragmentTransaction
import androidx.viewpager.widget.ViewPager
import java.util.*

class StopIdActivity : AppCompatActivity(), DownloadNexTripsTask.OnDownloadedListener, StopIdAdapter.OnClickMapListener, ActionBar.TabListener {
    private var mViewPager: ViewPager? = null
    private var mStopIdPagerAdapter: StopIdPagerAdapter? = null
    private var mStopId: String? = null
    private var mStopDesc: String? = null
    private var mDownloadNexTripsTask: DownloadNexTripsTask? = null
    private var mNexTripsFragment: NexTripsFragment? = null
    private var mMapFragment: MyMapFragment? = null
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

        mViewPager = findViewById(R.id.pager)

        if (mViewPager != null) {
            supportActionBar?.navigationMode = NAVIGATION_MODE_TABS
            val listTab = supportActionBar?.newTab()?.setIcon(R.drawable.ic_baseline_view_list_24px)
            val mapTab = supportActionBar?.newTab()?.setIcon(R.drawable.ic_baseline_map_24px)
            listTab?.setTabListener(this)
            mapTab?.setTabListener(this)
            supportActionBar?.addTab(listTab)
            supportActionBar?.addTab(mapTab)
            mStopIdPagerAdapter = StopIdPagerAdapter(supportFragmentManager, this)
            mViewPager!!.adapter = mStopIdPagerAdapter
        } else {
            mNexTripsFragment = NexTripsFragment.newInstance()
            mNexTripsFragment!!.setOnClickMapListener(this)
            mMapFragment = MyMapFragment.newInstance()
            supportFragmentManager.beginTransaction()
                    .add(R.id.nextrips_container, mNexTripsFragment!!)
                    .commit()
            supportFragmentManager.beginTransaction()
                    .add(R.id.map_container, mMapFragment!!)
                    .commit()
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
                mStopId?.let { stopId ->
                    if (mDownloadNexTripsTask!!.status == AsyncTask.Status.FINISHED &&
                			unixTime - mLastUpdate >= MIN_SECONDS_BETWEEN_REFRESH) {
                        mDownloadNexTripsTask = DownloadNexTripsTask(this, this, stopId)
                        mDownloadNexTripsTask!!.execute()
                    }
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
        mLastUpdate = unixTime
        mNexTripsFragment?.updateNexTrips(nexTrips)
    }

    override fun onClickMap(nexTrip: NexTrip) {
        val b = Bundle()
        b.putString("routeAndTerminal", nexTrip.route + nexTrip.terminal)
        b.putString("departureText", nexTrip.departureText)
        b.putDouble("vehicleLatitude", nexTrip.vehicleLatitude)
        b.putDouble("vehicleLongitude", nexTrip.vehicleLongitude)
        mViewPager?.setCurrentItem(ITEM_IDX_MAP, false)
        // supportActionBar?.let { it.selectTab(it.getTabAt(ITEM_IDX_MAP)) }
        supportActionBar?.setSelectedNavigationItem(ITEM_IDX_MAP)
        mMapFragment?.updateVehicle(b)
    }

    inner class StopIdPagerAdapter(fm: FragmentManager, private val mClickMapListener: StopIdAdapter.OnClickMapListener) : FragmentPagerAdapter(fm) {
        override fun getCount(): Int = 2

        override fun getItem(i: Int): Fragment? {
            return when (i) {
                ITEM_IDX_NEXTRIPS -> {
                    val fragment = NexTripsFragment.newInstance()
                    android.util.Log.d("a", "got here: getItem(0)")
                    fragment.setOnClickMapListener(mClickMapListener)
                    mNexTripsFragment = fragment
                    fragment
                }
                ITEM_IDX_MAP -> {
                    val fragment = MyMapFragment.newInstance()
                    mMapFragment = fragment
                    fragment
                }
                else ->null
            }
        }
    }

    override fun onTabSelected(tab: ActionBar.Tab, ft: FragmentTransaction) {
        android.util.Log.d("a", "got here: onTabSelected " + tab.getPosition().toString())
        mViewPager?.setCurrentItem(tab.getPosition())
    }

    override fun onTabUnselected(tab: ActionBar.Tab, ft: FragmentTransaction) {
    }

    override fun onTabReselected(tab: ActionBar.Tab, ft: FragmentTransaction) {
    }

    companion object {
        private val MIN_SECONDS_BETWEEN_REFRESH: Long = 30
        private val IS_FAV_ICON = R.drawable.ic_baseline_favorite_24px
        private val IS_NOT_FAV_ICON = R.drawable.ic_baseline_favorite_border_24px
        private val KEY_STOP_ID = "stopId"
        private val ITEM_IDX_NEXTRIPS = 0
        private val ITEM_IDX_MAP = 1
    }
}

