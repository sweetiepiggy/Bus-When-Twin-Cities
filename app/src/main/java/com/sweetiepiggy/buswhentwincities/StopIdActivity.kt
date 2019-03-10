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
import android.view.View
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
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.viewpager.widget.ViewPager
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.snackbar.Snackbar.LENGTH_INDEFINITE
import java.util.*

class StopIdActivity : AppCompatActivity(), StopIdAdapter.OnClickMapListener, ActionBar.TabListener, NexTripsViewModel.OnLoadNexTripsErrorListener {
    private var mStopIdPagerAdapter: StopIdPagerAdapter? = null
    private var mStopId: String? = null
    private var mStopDesc: String? = null
    private var mNexTripsFragment: NexTripsFragment? = null
    private var mMapFragment: MyMapFragment? = null
    private var mMenu: Menu? = null
    private var mIsFavorite = false
    private var mDualPane = false
    private lateinit var mNexTripsModel: NexTripsViewModel

    private val unixTime: Long
        get() = Calendar.getInstance().timeInMillis / 1000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stop_id)

        if (savedInstanceState == null) {
            intent.extras?.let { loadState(it) }
        } else {
            loadState(savedInstanceState)
        }

        mDualPane = findViewById<ViewPager>(R.id.pager) == null

        mNexTripsModel = ViewModelProviders.of(this, NexTripsViewModel.NexTripsViewModelFactory(mStopId))
                .get(NexTripsViewModel::class.java)
        mNexTripsModel.setLoadNexTripsErrorListener(this)
        mNexTripsModel.getNexTrips().observe(this, Observer<List<NexTrip>>{ _ -> })

        if (mDualPane) {
            mNexTripsFragment = NexTripsFragment.newInstance()
            supportFragmentManager.beginTransaction()
                    .add(R.id.nextrips_container, mNexTripsFragment!!)
                    .commit()
            mMapFragment = MyMapFragment.newInstance()
            supportFragmentManager.beginTransaction()
                    .add(R.id.map_container, mMapFragment!!)
                    .commit()
        } else {
            supportActionBar?.navigationMode = NAVIGATION_MODE_TABS
            val listTab = supportActionBar?.newTab()?.setIcon(R.drawable.ic_baseline_view_list_24px)
            val mapTab = supportActionBar?.newTab()?.setIcon(R.drawable.ic_baseline_map_24px)
            listTab?.setTabListener(this)
            mapTab?.setTabListener(this)
            supportActionBar?.addTab(listTab)
            supportActionBar?.addTab(mapTab)
            mStopIdPagerAdapter = StopIdPagerAdapter(supportFragmentManager, this)
            findViewById<ViewPager>(R.id.pager)!!.adapter = mStopIdPagerAdapter
        }

        title = resources.getString(R.string.stop) + " #" + mStopId

        LoadIsFavorite().execute()
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        mMenu = menu
        menuInflater.inflate(R.menu.menu_stop_id, menu)
        menu.findItem(R.id.action_favorite).icon = ContextCompat.getDrawable(this,
        	if (mIsFavorite) IS_FAV_ICON else IS_NOT_FAV_ICON)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_refresh -> {
                mNexTripsModel.loadNexTrips()
                return true
            }
            R.id.action_favorite -> {
                if (mIsFavorite) {
                    mIsFavorite = false
                    item.icon = ContextCompat.getDrawable(this, IS_NOT_FAV_ICON)
                    title = resources.getString(R.string.stop) + " #" + mStopId
                    mStopId?.let { stopId ->
                        object : AsyncTask<Void, Void, Void>() {
                            override fun doInBackground(vararg params: Void): Void? {
                                DbAdapter().apply {
                                    openReadWrite(applicationContext)
                                    deleteFavStop(stopId)
                                    close()
                                }
                                return null
                            }
                            override fun onPostExecute(result: Void?) {}
                        }.execute()
                    }
                } else {
                    val builder = AlertDialog.Builder(this)
                    val favStopIdDialog = layoutInflater.inflate(R.layout.dialog_fav_stop_id, null)
                    builder.setView(favStopIdDialog)
                    builder.setPositiveButton(android.R.string.ok) { _, _ ->
                        mIsFavorite = true
                        item.icon = ContextCompat.getDrawable(this, IS_FAV_ICON)
                        val stopName = favStopIdDialog.findViewById<EditText>(R.id.stop_name)?.text.toString()
                        title = resources.getString(R.string.stop) + " #" + mStopId +
            	        	(if (!stopName.isNullOrEmpty()) " ($stopName)" else "")
                        mStopId?.let { stopId ->
                            object : AsyncTask<Void, Void, Void>() {
                                override fun doInBackground(vararg params: Void): Void? {
                                    DbAdapter().apply {
                                        openReadWrite(applicationContext)
                                        createFavStop(stopId, stopName)
                                        close()
                                    }
                                    return null
                                }
                                override fun onPostExecute(result: Void?) {}
                            }.execute()
                        }
                    }
                    builder.setNegativeButton(android.R.string.cancel) { _, _ -> }
                            .setTitle(R.string.enter_stop_name_dialog_title)
                            .show()
                }

                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onLoadNexTripsError(e: DownloadNexTripsTask.DownloadError) {
        val message: String? =
            when (e) {
                is DownloadNexTripsTask.DownloadError.UnknownHost -> getResources().getString(R.string.unknown_host)
                is DownloadNexTripsTask.DownloadError.FileNotFound -> getResources().getString(R.string.file_not_found) + ":\n${e.message}"
                is DownloadNexTripsTask.DownloadError.TimedOut -> getResources().getString(R.string.timed_out) + ":\n${e.message}"
                is DownloadNexTripsTask.DownloadError.Unauthorized -> getResources().getString(R.string.unauthorized)
                is DownloadNexTripsTask.DownloadError.OtherDownloadError -> e.message
            }
        Snackbar.make(findViewById<View>(R.id.coordinator_layout), message ?: "", LENGTH_INDEFINITE)
                .setAction(getResources().getString(R.string.dismiss), object : View.OnClickListener {
                    override fun onClick(v: View) {}
                })
                .show()
    }

    override fun onClickMap(nexTrip: NexTrip) {
        val b = Bundle()
        b.putString("routeAndTerminal", nexTrip.route + nexTrip.terminal)
        b.putString("departureText", nexTrip.departureText)
        b.putDouble("vehicleLatitude", nexTrip.vehicleLatitude)
        b.putDouble("vehicleLongitude", nexTrip.vehicleLongitude)
        if (!mDualPane) {
            findViewById<ViewPager>(R.id.pager)!!.setCurrentItem(ITEM_IDX_MAP, false)
            // supportActionBar?.let { it.selectTab(it.getTabAt(ITEM_IDX_MAP)) }
            supportActionBar?.setSelectedNavigationItem(ITEM_IDX_MAP)
        }
        mMapFragment?.updateVehicle(b)
    }

    inner class StopIdPagerAdapter(fm: FragmentManager, private val mClickMapListener: StopIdAdapter.OnClickMapListener) : FragmentPagerAdapter(fm) {
        override fun getCount(): Int = 2

        override fun getItem(i: Int): Fragment? {
            return when (i) {
                ITEM_IDX_NEXTRIPS -> {
                    val fragment = NexTripsFragment.newInstance()
                    mNexTripsFragment = fragment
                    fragment
                }
                ITEM_IDX_MAP -> {
                    val fragment = MyMapFragment.newInstance()
                    mMapFragment = fragment
                    fragment
                }
                else -> null
            }
        }
    }

    override fun onTabSelected(tab: ActionBar.Tab, ft: FragmentTransaction) {
        if (!mDualPane) findViewById<ViewPager>(R.id.pager)!!.setCurrentItem(tab.getPosition())
    }

    override fun onTabUnselected(tab: ActionBar.Tab, ft: FragmentTransaction) {
    }

    override fun onTabReselected(tab: ActionBar.Tab, ft: FragmentTransaction) {
    }

    private inner class LoadIsFavorite(): AsyncTask<Void, Void, Pair<Boolean, String?>>() {
        override fun doInBackground(vararg params: Void): Pair<Boolean, String?> {
            val dbHelper = DbAdapter()
            dbHelper.open(applicationContext)
            val isFavorite = mStopId?.let { dbHelper.isFavStop(it) } ?: false
            val stopDesc = mStopId?.let { dbHelper.getStopDesc(it) }
            dbHelper.close()
            return Pair(isFavorite, stopDesc)
        }

        override fun onPostExecute(result: Pair<Boolean, String?>) {
            val (isFavorite, stopDesc) = result
            mIsFavorite = isFavorite
            mStopDesc = stopDesc

            title = resources.getString(R.string.stop) + " #" + mStopId +
            	(if (!stopDesc.isNullOrEmpty()) " ($stopDesc)" else "")
            mMenu?.findItem(R.id.action_favorite)?.icon = ContextCompat.getDrawable(applicationContext,
        		if (isFavorite) IS_FAV_ICON else IS_NOT_FAV_ICON)
        }
    }

    companion object {
        private val MIN_SECONDS_BETWEEN_REFRESH: Long = 30
        private val IS_FAV_ICON = R.drawable.ic_baseline_favorite_24px
        private val IS_NOT_FAV_ICON = R.drawable.ic_baseline_favorite_border_24px
        private val KEY_STOP_ID = "stopId"
        private val KEY_NEXTRIPS_FRAGMENT = "nexTripsFragment"
        private val KEY_MAP_FRAGMENT = "mapFragment"
        private val ITEM_IDX_NEXTRIPS = 0
        private val ITEM_IDX_MAP = 1
    }
}

