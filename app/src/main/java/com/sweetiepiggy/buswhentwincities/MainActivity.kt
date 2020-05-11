/*
    Copyright (C) 2019-2020 Sweetie Piggy Apps <sweetiepiggyapps@gmail.com>

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
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import com.sweetiepiggy.buswhentwincities.ui.favoritestopids.FavoriteStopIdsFragment

class MainActivity : AppCompatActivity(), FavoriteStopIdsAdapter.OnClickFavoriteListener, SearchStopIdFragment.OnSearchStopIdListener, BottomNavigationView.OnNavigationItemSelectedListener {
    private var mBnvIdx = BNV_UNINITIALIZED
    private var mFavStopIdsFragment: FavoriteStopIdsFragment? = null
    private var mSearchFragment: Fragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        if (savedInstanceState == null) {
            intent.extras?.let { loadState(it) }
        } else {
            loadState(savedInstanceState)
        }

        setSupportActionBar(findViewById<Toolbar>(R.id.toolbar))

        if (mSearchFragment == null) {
            mSearchFragment = SearchStopIdFragment.newInstance()
            supportFragmentManager.beginTransaction()
                    .add(R.id.container, mSearchFragment!!)
                    .detach(mSearchFragment!!)
                    .commit()
        }
        if (mFavStopIdsFragment == null) {
            mFavStopIdsFragment = FavoriteStopIdsFragment.newInstance()
            supportFragmentManager.beginTransaction()
                    .add(R.id.container, mFavStopIdsFragment!!)
                    .detach(mFavStopIdsFragment!!)
                    .commit()
        }

        val bnv = findViewById<BottomNavigationView>(R.id.bnv)

        when (mBnvIdx) {
            BNV_FAV -> {
                bnv.menu.findItem(R.id.action_favorites)?.isChecked = true
                selectBnvFav()
            }
            BNV_SEARCH -> {
                bnv.menu.findItem(R.id.action_search)?.isChecked = true
                selectBnvSearch()
            }
            else -> SelectDefaultBnv().execute()
        }

        bnv.setOnNavigationItemSelectedListener(this)

        Snackbar.make(findViewById<View>(android.R.id.content), resources.getString(R.string.wear_mask), Snackbar.LENGTH_INDEFINITE).apply {
            setAction(resources.getString(R.string.dismiss), object : View.OnClickListener {
                          override fun onClick(v: View) {}
            })
            show()
        }
    }

    public override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)
        savedInstanceState.putInt(KEY_BNV_IDX, mBnvIdx)
        mFavStopIdsFragment?.let { supportFragmentManager.putFragment(savedInstanceState, KEY_BNV_FAV, it)}
        mSearchFragment?.let { supportFragmentManager.putFragment(savedInstanceState, KEY_BNV_SEARCH, it)}
    }

    private fun loadState(b: Bundle) {
        mBnvIdx = b.getInt(KEY_BNV_IDX, BNV_UNINITIALIZED)
        mFavStopIdsFragment = supportFragmentManager.getFragment(b, KEY_BNV_FAV) as FavoriteStopIdsFragment?
        mSearchFragment = supportFragmentManager.getFragment(b, KEY_BNV_SEARCH)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        when (item.itemId) {
            R.id.action_about -> {
                startActivity(Intent(this, AboutActivity::class.java))
                true
            }
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            R.id.action_contact -> {
                startActivity(Intent(this, ContactActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }

    override fun onNavigationItemSelected(item: MenuItem): Boolean =
        when (item.itemId) {
            R.id.action_search -> {
                mBnvIdx = BNV_SEARCH
                selectBnvSearch()
                true
            }
            R.id.action_favorites -> {
                mBnvIdx = BNV_FAV
                selectBnvFav()
                true
            }
            else -> false
        }

    override fun onClickFavorite(favStop: FavoriteStopIdsViewModel.FavoriteStop) {
        val b = Bundle().apply {
            when (favStop) {
                is FavoriteStopIdsViewModel.FavoriteStop.FavoriteStopId -> {
                    putInt(StopIdActivity.KEY_STOP_ID, favStop.stopId)
                    putString(StopIdActivity.KEY_STOP_DESC, favStop.stopDesc)
                }
                is FavoriteStopIdsViewModel.FavoriteStop.FavoriteTimestop -> {
                    putString(StopIdActivity.KEY_ROUTE_ID, favStop.timestop.routeId)
                    putInt(StopIdActivity.KEY_DIRECTION_ID, NexTrip.getDirectionId(favStop.timestop.direction))
                    putString(StopIdActivity.KEY_TIMESTOP_ID, favStop.timestop.timestopId)
                    putString(StopIdActivity.KEY_STOP_DESC, favStop.stopDesc)
                }
            }
            putBoolean(StopIdActivity.KEY_IS_FAVORITE, true)
        }
        startStopIdActivity(b)
    }

    override fun onMoveFavorite(fromPosition: Int, toPosition: Int) {
        object : AsyncTask<Void, Void, Void>() {
            override fun doInBackground(vararg params: Void): Void? {
                DbAdapter().apply {
                    openReadWrite(applicationContext)
                    moveFavStop(fromPosition, toPosition)
                    close()
                }
                return null
            }
            override fun onPostExecute(result: Void?) { }
        }.execute()
    }

    override fun onPromptDeleteFavorite(removedStop: FavoriteStopIdsViewModel.FavoriteStop, position: Int, recyclerViewPosition: Int) {
        AlertDialog.Builder(this).apply {
            setMessage(FavoriteStopIdsViewModel.FavoriteStop.stopDesc(removedStop))
            setTitle(resources.getString(R.string.remove_favorite))
            setPositiveButton(resources.getString(R.string.remove)) { _, _ ->
                object : AsyncTask<Void, Void, Void>() {
                    override fun doInBackground(vararg params: Void): Void? {
                        DbAdapter().apply {
                            openReadWrite(applicationContext)
                            deleteFavStopAtPosition(position)
                            close()
                        }
                        return null
                    }
                    override fun onPostExecute(result: Void?) {
                        mFavStopIdsFragment?.onDeleteFavorite()
//                        mFavStopIdsFragment?.updateFavoriteStopIdsMessage()
                    }
                }.execute()
            }
            setNegativeButton(android.R.string.cancel) { _, _ ->
                mFavStopIdsFragment?.onCancelDeleteFavorite(removedStop, recyclerViewPosition)
            }
            setOnCancelListener() {
                mFavStopIdsFragment?.onCancelDeleteFavorite(removedStop, recyclerViewPosition)
            }
        }.show()
    }

    override fun onSearchStopId(stopId: Int) {
        val b = Bundle().apply {
            putInt(StopIdActivity.KEY_STOP_ID, stopId)
        }
        startStopIdActivity(b)
    }

    override fun onSearchRouteId(routeId: String) {
        val intent = Intent(this, BrowseDirectionsActivity::class.java).apply {
            putExtras(Bundle().apply {
                putString(BrowseDirectionsActivity.KEY_ROUTE_ID, routeId)
            })
        }
        startActivityForResult(intent, ACTIVITY_BROWSE_ROUTES)
    }

    override fun onBrowseRoutes() {
        val intent = Intent(this, BrowseRoutesActivity::class.java)
        startActivityForResult(intent, ACTIVITY_BROWSE_ROUTES)
    }

    private fun startStopIdActivity(b: Bundle) {
        val intent = Intent(this, StopIdActivity::class.java).apply {
            putExtras(b)
        }
        startActivityForResult(intent, ACTIVITY_STOP_ID)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            ACTIVITY_STOP_ID -> {
                // if (resultCode == RESULT_OK) {
                    mFavStopIdsFragment?.refresh()
                // }
            }
            ACTIVITY_BROWSE_ROUTES -> {
                mFavStopIdsFragment?.refresh()
            }
        }
    }

    private fun selectBnvSearch() {
        findViewById<View>(R.id.progressBar).setVisibility(View.INVISIBLE)
        supportFragmentManager.beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .detach(mFavStopIdsFragment!!)
                .attach(mSearchFragment!!)
                .commitAllowingStateLoss()
    }

    private fun selectBnvFav() {
        supportFragmentManager.beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .detach(mSearchFragment!!)
                .attach(mFavStopIdsFragment!!)
                .commitAllowingStateLoss()
    }

    private inner class SelectDefaultBnv(): AsyncTask<Void, Void, Int>() {
        override fun doInBackground(vararg params: Void): Int {
            val dbHelper = DbAdapter()
            dbHelper.open(applicationContext)
            val hasAnyFavorites = dbHelper.hasAnyFavorites()
            dbHelper.close()
            return if (hasAnyFavorites) BNV_FAV else BNV_SEARCH
        }

        override fun onPostExecute(result: Int) {
            mBnvIdx = result
            val bnv = findViewById<BottomNavigationView>(R.id.bnv)
            when (mBnvIdx) {
                BNV_FAV -> {
                    bnv.menu.findItem(R.id.action_favorites)?.isChecked = true
                    selectBnvFav()
                }
                BNV_SEARCH -> {
                    bnv.menu.findItem(R.id.action_search)?.isChecked = true
                    selectBnvSearch()
                }
            }
            DeletePastDueNexTripsTask().execute()
        }
    }

    private inner class DeletePastDueNexTripsTask(): AsyncTask<Void, Void, Void>() {
        override fun doInBackground(vararg params: Void): Void? {
            DbAdapter().run {
                openReadWrite(applicationContext)
                deletePastDueNexTrips(SECONDS_BEFORE_NOW_TO_DELETE)
                close()
            }
            return null
        }
        override fun onPostExecute(result: Void?) { }
    }

    companion object {
        private val KEY_BNV_IDX = "bnvIdx"
        private val KEY_BNV_FAV = "bnvFav"
        private val KEY_BNV_SEARCH = "bnvSearch"

        private val BNV_UNINITIALIZED = 0
        private val BNV_FAV = 1
        private val BNV_SEARCH = 2

        private val ITEM_IDX_FAV = 0
        private val ITEM_IDX_SEARCH = 1

        private val ACTIVITY_STOP_ID = 0
        private val ACTIVITY_BROWSE_ROUTES = 1

        // delete NexTrips that were due two minutes or more before now
        private val SECONDS_BEFORE_NOW_TO_DELETE = 120
    }
}
