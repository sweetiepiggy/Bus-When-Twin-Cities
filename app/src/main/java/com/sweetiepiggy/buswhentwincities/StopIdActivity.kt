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
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.content.res.AppCompatResources.getDrawable
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.lifecycle.ViewModelProviders
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import java.security.InvalidParameterException
import java.util.*

class StopIdActivity : AppCompatActivity(), StopIdAdapter.OnClickMapListener, NexTripsViewModel.OnLoadNexTripsErrorListener {
    private var mStopId: Int? = null
    private var mStopDesc: String? = null
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

        setSupportActionBar(findViewById<Toolbar>(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        mDualPane = findViewById<ViewPager>(R.id.pager) == null

        mNexTripsModel = ViewModelProviders.of(this, NexTripsViewModel.NexTripsViewModelFactory(mStopId))
                .get(NexTripsViewModel::class.java)
        mNexTripsModel.setLoadNexTripsErrorListener(this)

        if (mDualPane) {
            supportFragmentManager.beginTransaction()
                    .add(R.id.nextrips_container, NexTripsFragment.newInstance())
                    .commit()
            mMapFragment = MyMapFragment.newInstance()
            supportFragmentManager.beginTransaction()
                    .add(R.id.map_container, mMapFragment!!)
                    .commit()
        } else {
            val viewPager = findViewById<ViewPager>(R.id.pager)
            viewPager.adapter = StopIdPagerAdapter(supportFragmentManager, this)
            findViewById<TabLayout>(R.id.tab_layout).run {
                setupWithViewPager(viewPager)
                getTabAt(ITEM_IDX_NEXTRIPS)?.setIcon(R.drawable.ic_baseline_view_list_24px)
                getTabAt(ITEM_IDX_MAP)?.setIcon(R.drawable.ic_baseline_map_24px)
            }
        }

        title = resources.getString(R.string.stop) + " #" + mStopId.toString()

        LoadIsFavorite().execute()
    }

    public override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)
        mStopId?.let { savedInstanceState.putInt(KEY_STOP_ID, it) }
    }

    private fun loadState(b: Bundle) {
        mStopId = b.getInt(KEY_STOP_ID)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        mMenu = menu
        menuInflater.inflate(R.menu.menu_stop_id, menu)
        menu.findItem(R.id.action_favorite).icon =
        	getDrawable(this, if (mIsFavorite) IS_FAV_ICON else IS_NOT_FAV_ICON)
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
                    item.icon = getDrawable(this, IS_NOT_FAV_ICON)
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
                        item.icon = getDrawable(this, IS_FAV_ICON)
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

    override fun onLoadNexTripsError(err: DownloadNexTripsTask.DownloadError) {
        val resources = getResources()
        val message: String? =
            when (err) {
                is DownloadNexTripsTask.DownloadError.UnknownHost -> resources.getString(R.string.unknown_host)
                is DownloadNexTripsTask.DownloadError.FileNotFound -> resources.getString(R.string.file_not_found) + ":\n${err.message}"
                is DownloadNexTripsTask.DownloadError.TimedOut -> resources.getString(R.string.timed_out) + ":\n${err.message}"
                is DownloadNexTripsTask.DownloadError.Unauthorized -> resources.getString(R.string.unauthorized)
                is DownloadNexTripsTask.DownloadError.OtherDownloadError -> err.message
            }
            val alert = AlertDialog.Builder(this).apply {
                setTitle(resources.getString(android.R.string.dialog_alert_title))
                message?.let { setMessage(it) }
                setPositiveButton(resources.getString(R.string.dismiss)) { _, _ -> }
                show()
            }
    }

    override fun onClickMap(vehicleBlockNumber: Int?) {
        if (!mDualPane) {
            findViewById<ViewPager>(R.id.pager)!!.setCurrentItem(ITEM_IDX_MAP, false)
        }
        vehicleBlockNumber?.let { mMapFragment!!.selectVehicle(it) }
    }

    inner class StopIdPagerAdapter(fm: FragmentManager, private val mClickMapListener: StopIdAdapter.OnClickMapListener) : FragmentPagerAdapter(fm) {
        override fun getCount(): Int = 2

        override fun getItem(p0: Int): Fragment =
            when (p0) {
                ITEM_IDX_NEXTRIPS -> NexTripsFragment.newInstance()
                ITEM_IDX_MAP -> MyMapFragment.newInstance()
                else -> throw InvalidParameterException("getItem() parameter out of range")
            }

        override fun instantiateItem(container: ViewGroup, i: Int): Any {
            val fragment = super.instantiateItem(container, i)
            if (i == ITEM_IDX_MAP) mMapFragment = fragment as MyMapFragment
            return fragment
        }
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
            mMenu?.findItem(R.id.action_favorite)?.icon = getDrawable(applicationContext,
        		if (isFavorite) IS_FAV_ICON else IS_NOT_FAV_ICON)
        }
    }

    companion object {
        val KEY_STOP_ID = "stopId"

        private val MIN_SECONDS_BETWEEN_REFRESH: Long = 30
        private val IS_FAV_ICON = R.drawable.ic_baseline_favorite_24px
        private val IS_NOT_FAV_ICON = R.drawable.ic_baseline_favorite_border_24px
        private val ITEM_IDX_NEXTRIPS = 0
        private val ITEM_IDX_MAP = 1
    }
}

