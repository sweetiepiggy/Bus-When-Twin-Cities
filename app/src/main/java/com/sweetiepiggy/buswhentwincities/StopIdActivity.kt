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

import android.content.DialogInterface
import android.os.AsyncTask
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources.getDrawable
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.viewpager.widget.ViewPager
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.snackbar.Snackbar.LENGTH_LONG
import com.google.android.material.tabs.TabLayout
import java.security.InvalidParameterException
import java.util.*

class StopIdActivity : AppCompatActivity(), StopIdAdapter.OnClickMapListener, NexTripsViewModel.OnLoadNexTripsErrorListener {
    private var mStopId: Int? = null
    private var mStopDesc: String? = null
    private var mMapFragment: MyMapFragment? = null
    private var mNexTripsFragment: NexTripsFragment? = null
    private var mMenu: Menu? = null
    private var mIsFavorite: Boolean? = null
    private var mDualPane = false
    private lateinit var mNexTripsModel: NexTripsViewModel
    private var mNexTrips: List<NexTrip> = listOf()
    private var mDoShowRoutes: MutableMap<Pair<String?, String?>, Boolean> = mutableMapOf()
    private var mFilteredButWasntFavorite = false
    private var mDoShowRoutesInitDone = false

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

        mNexTripsModel = ViewModelProviders.of(this,
        	NexTripsViewModel.NexTripsViewModelFactory(mStopId, applicationContext)
        ).get(NexTripsViewModel::class.java)
        mNexTripsModel.setLoadNexTripsErrorListener(this)
        mNexTripsModel.getNexTrips().observe(this, Observer<List<NexTrip>>{ updateRoutes(it) })
        mNexTripsModel.getDoShowRoutes().observe(this, Observer<Map<Pair<String?, String?>, Boolean>>{
            if (!mDoShowRoutesInitDone) {
                initDoShowRoutes(it)
                mDoShowRoutesInitDone = true
            }
        })

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
            val viewPager = findViewById<ViewPager>(R.id.pager)
            viewPager.adapter = StopIdPagerAdapter(supportFragmentManager, this)
            findViewById<TabLayout>(R.id.tab_layout).run {
                setupWithViewPager(viewPager)
                getTabAt(ITEM_IDX_NEXTRIPS)?.setIcon(R.drawable.ic_baseline_view_list_24px)
                getTabAt(ITEM_IDX_MAP)?.setIcon(R.drawable.ic_baseline_map_24px)
            }
        }

        title = makeTitle(mStopId, mStopDesc)

        findViewById<View>(R.id.fab)?.setOnClickListener {
            mNexTripsFragment?.setRefreshing(true)
            mNexTripsModel.loadNexTrips()
        }
    }

    public override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)
        mStopId?.let { savedInstanceState.putInt(KEY_STOP_ID, it) }
        mIsFavorite?.let { savedInstanceState.putBoolean(KEY_IS_FAVORITE, it) }
        mStopDesc?.let { savedInstanceState.putString(KEY_STOP_DESC, it) }
    }

    private fun loadState(b: Bundle) {
        if (b.containsKey(KEY_STOP_ID)) {
            mStopId = b.getInt(KEY_STOP_ID)
        }
        if (b.containsKey(KEY_STOP_DESC)) {
            mStopDesc = b.getString(KEY_STOP_DESC)
        }
        if (b.containsKey(KEY_IS_FAVORITE)) {
            mIsFavorite = b.getBoolean(KEY_IS_FAVORITE)
        } else {
            LoadIsFavorite().execute()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        mMenu = menu
        menuInflater.inflate(R.menu.menu_stop_id, menu)
        menu.findItem(R.id.action_favorite).icon =
            getDrawable(this, if (mIsFavorite ?: false) IS_FAV_ICON else IS_NOT_FAV_ICON)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        when (item.itemId) {
            R.id.action_filter -> { onClickFilter(); true }
            R.id.action_favorite -> { onClickFavorite(item); true }
            else -> super.onOptionsItemSelected(item)
        }

    override fun onLoadNexTripsError(err: DownloadNexTripsTask.DownloadError) {
        if (isFinishing()) {
            return
        }
        val message: String? =
            when (err) {
                is DownloadNexTripsTask.DownloadError.UnknownHost -> resources.getString(R.string.unknown_host)
                is DownloadNexTripsTask.DownloadError.FileNotFound -> resources.getString(R.string.file_not_found) + ":\n${err.message}"
                is DownloadNexTripsTask.DownloadError.TimedOut -> resources.getString(R.string.timed_out) + ":\n${err.message}"
                is DownloadNexTripsTask.DownloadError.Unauthorized -> resources.getString(R.string.unauthorized)
                is DownloadNexTripsTask.DownloadError.OtherDownloadError -> err.message
            }
        if (mNexTrips.isEmpty()) {
            AlertDialog.Builder(this).apply {
                setTitle(resources.getString(android.R.string.dialog_alert_title))
                message?.let { setMessage(it) }
                setPositiveButton(resources.getString(R.string.dismiss)) { _, _ -> }
            }.show()
        } else {
            // mNexTripsFragment?.updateNexTrips(mNexTrips)
            Snackbar.make(findViewById<View>(R.id.coordinator_layout), message ?: "", LENGTH_LONG)
            	.setAction(resources.getString(R.string.dismiss), object : View.OnClickListener {
                    override fun onClick(v: View) {}
        		}).show()
        }
    }

    private fun onClickFilter() {
        AlertDialog.Builder(this).apply {
            setTitle(R.string.select_routes)

            val routeAndTerminalPairs = mDoShowRoutes.keys.filter {
                it.first != null
            }.toSortedSet(object : Comparator<Pair<String?, String?>>{
                override fun compare(p1: Pair<String?, String?>, p2: Pair<String?, String?>): Int =
                    (p1.first!! + (p1.second ?: "")).compareTo(p2.first!! + (p2.second ?: ""))
            }).toTypedArray()
            val routeAndTerminals = routeAndTerminalPairs.map { routeAndTerminal ->
                routeAndTerminal.first!! + (routeAndTerminal.second ?: "")
            }.toTypedArray()
            val routeAndTerminalsDoShow = routeAndTerminalPairs.map { mDoShowRoutes[it]!! }.toBooleanArray()
            setMultiChoiceItems(routeAndTerminals, routeAndTerminalsDoShow,
            	DialogInterface.OnMultiChoiceClickListener { _, which, isChecked ->
                    routeAndTerminalsDoShow[which] = isChecked
                })
            setPositiveButton(android.R.string.ok) { _, _ ->
                val changedRoutes = mutableSetOf<Pair<String?, String?>>()
                for ((idx, doShow) in routeAndTerminalsDoShow.iterator().withIndex()) {
                    val routeAndTerminalPair = routeAndTerminalPairs[idx]
                    if (doShow) {
                        if (!(mDoShowRoutes.get(routeAndTerminalPair) ?: false)) {
                            changedRoutes.add(routeAndTerminalPair)
                            mDoShowRoutes[routeAndTerminalPair] = true
                        }
                    } else {
                        if (mDoShowRoutes.get(routeAndTerminalPair) ?: true){
                            changedRoutes.add(routeAndTerminalPair)
                            mDoShowRoutes[routeAndTerminalPair] = false
                        }
                    }
                }
                if (!changedRoutes.isEmpty()) {
                    mNexTripsModel.setDoShowRoutes(mDoShowRoutes)
                    mNexTripsFragment?.onChangeHiddenRoutes(changedRoutes)
                    mMapFragment?.onChangeHiddenRoutes(changedRoutes)
                    if (mIsFavorite ?: false) {
                        StoreDoShowRoutesInDbTask(mDoShowRoutes).execute()
                    }
                    mFilteredButWasntFavorite = true
                }
            }
            setNegativeButton(android.R.string.cancel) { _, _ -> }
        }.show()
    }

    private fun onClickFavorite(item: MenuItem) {
        if (mIsFavorite ?: false) {
            mIsFavorite = false
            item.icon = getDrawable(this, IS_NOT_FAV_ICON)
            title = resources.getString(R.string.stop_number) + mStopId
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
            AlertDialog.Builder(this).apply {
                val favStopIdDialog = layoutInflater.inflate(R.layout.dialog_fav_stop_id, null)
                setTitle(R.string.enter_stop_name_dialog_title)
                setView(favStopIdDialog)
                setPositiveButton(android.R.string.ok) { _, _ ->
                    mIsFavorite = true
                    item.icon = getDrawable(context, IS_FAV_ICON)
                    val stopName = favStopIdDialog.findViewById<EditText>(R.id.stop_name)?.text.toString()
                    title = resources.getString(R.string.stop_number) + mStopId +
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
                    if (mFilteredButWasntFavorite) {
                        StoreDoShowRoutesInDbTask(mDoShowRoutes).execute()
                    }
                }
                setNegativeButton(android.R.string.cancel) { _, _ -> }
            }.show()
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
            when (i) {
                ITEM_IDX_NEXTRIPS -> mNexTripsFragment = fragment as NexTripsFragment
                ITEM_IDX_MAP -> mMapFragment = fragment as MyMapFragment
            }
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

            title = makeTitle(mStopId, stopDesc)
            mMenu?.findItem(R.id.action_favorite)?.icon = getDrawable(applicationContext,
        		if (isFavorite) IS_FAV_ICON else IS_NOT_FAV_ICON)
        }
    }

    private inner class StoreDoShowRoutesInDbTask(private val doShowRoutes: Map<Pair<String?, String?>, Boolean>): AsyncTask<Void, Void, Void?>() {
        override fun doInBackground(vararg params: Void): Void? {
        	mStopId?.let { stopId ->
                DbAdapter().run {
                    openReadWrite(applicationContext)
                    updateDoShowRoutes(stopId, doShowRoutes)
                    close()
                }
            }
            return null
        }

        override fun onPostExecute(result: Void?) { }
    }

    private fun updateRoutes(nexTrips: List<NexTrip>) {
        mNexTrips = nexTrips

        if (!mDoShowRoutesInitDone) {
            return
        }

        val changedRoutes: MutableSet<Pair<String?, String?>> = mutableSetOf()
        val routeAndTerminalPairs = nexTrips.filter {
            it.routeAndTerminal != null
        }.map {
            Pair(it.route, it.terminal)
        }.toSet()
        for (routeAndTerminal in routeAndTerminalPairs) {
            if (!mDoShowRoutes.contains(routeAndTerminal)) {
                mDoShowRoutes[routeAndTerminal] = guessDoShow(routeAndTerminal.first, routeAndTerminal.second)
                changedRoutes.add(routeAndTerminal)
            }
        }
        if (!changedRoutes.isEmpty()) {
            mNexTripsModel.setDoShowRoutes(mDoShowRoutes)
            mNexTripsFragment?.onChangeHiddenRoutes(changedRoutes)
            mMapFragment?.onChangeHiddenRoutes(changedRoutes)
        }
    }

    private fun initDoShowRoutes(doShowRoutes: Map<Pair<String?, String?>, Boolean>) {
        mDoShowRoutes = doShowRoutes.toMutableMap()

        val changedRoutes: MutableSet<Pair<String?, String?>> = mutableSetOf()
        var addedRoutes = false

        val routeAndTerminalPairs = mNexTrips.filter {
            it.routeAndTerminal != null
        }.map {
            Pair(it.route, it.terminal)
        }.toSet()

        for (routeAndTerminal in routeAndTerminalPairs) {
            if (!mDoShowRoutes.contains(routeAndTerminal)) {
                mDoShowRoutes[routeAndTerminal] = guessDoShow(routeAndTerminal.first, routeAndTerminal.second)
                addedRoutes = true
            }

            // routes are shown by default, so the changed routes here are the
            // ones that should be hidden
            if (!mDoShowRoutes[routeAndTerminal]!!){
                changedRoutes.add(routeAndTerminal)
            }
        }

        if (addedRoutes) {
            mNexTripsModel.setDoShowRoutes(mDoShowRoutes)
        }

        if (!changedRoutes.isEmpty()) {
            mNexTripsFragment?.onChangeHiddenRoutes(changedRoutes)
            mMapFragment?.onChangeHiddenRoutes(changedRoutes)
        }
    }

    private fun guessDoShow(route: String?, terminal: String?) = true

    private fun makeTitle(stopId: Int?, stopDesc: String?): String =
        if (stopDesc.isNullOrEmpty())
        	resources.getString(R.string.stop_number) + (stopId?.toString() ?: "")
        else
        	stopDesc + stopId?.let { " (#" + it.toString() + ")" }

    companion object {
        val KEY_STOP_ID = "stopId"
        val KEY_IS_FAVORITE = "isFavorite"
        val KEY_STOP_DESC = "stopDesc"

        private val IS_FAV_ICON = R.drawable.ic_baseline_favorite_24px
        private val IS_NOT_FAV_ICON = R.drawable.ic_baseline_favorite_border_24px
        private val ITEM_IDX_NEXTRIPS = 0
        private val ITEM_IDX_MAP = 1
    }
}

