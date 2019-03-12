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
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.sweetiepiggy.buswhentwincities.ui.favoritestopids.FavoriteStopIdsFragment
import java.security.InvalidParameterException

class MainActivity : AppCompatActivity(), FavoriteStopIdsAdapter.OnClickFavoriteListener, SearchStopIdFragment.OnSearchStopIdListener, BottomNavigationView.OnNavigationItemSelectedListener {
    private var mBnvIdx = BNV_UNINITIALIZED
    private var mFavStopIdsFragment: FavoriteStopIdsFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (savedInstanceState == null) {
            intent.extras?.let { loadState(it) }
        } else {
            loadState(savedInstanceState)
        }

        setSupportActionBar(findViewById<Toolbar>(R.id.toolbar))

        findViewById<ViewPager>(R.id.pager)!!.adapter = MainPagerAdapter(supportFragmentManager)

        val bnv = findViewById<BottomNavigationView>(R.id.bnv)
        bnv.setOnNavigationItemSelectedListener(this)

        when (mBnvIdx) {
            BNV_FAV -> {
                bnv.menu.findItem(R.id.action_favorite)?.isChecked = true
                selectBnvFav()
            }
            BNV_SEARCH -> {
                bnv.menu.findItem(R.id.action_search)?.isChecked = true
                selectBnvSearch()
            }
            else -> SelectDefaultBnv().execute()
        }
    }

    public override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)
        savedInstanceState.putInt(KEY_BNV_IDX, mBnvIdx)
    }

    private fun loadState(b: Bundle) {
        mBnvIdx = b.getInt(KEY_BNV_IDX)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        when (item.itemId) {
            R.id.action_about -> {
                val intent = Intent(this, AboutActivity::class.java)
                startActivity(intent)
                true
            }
            R.id.action_source -> {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(Uri.parse(SOURCE_URL), "text/html")
                }
                startActivity(Intent.createChooser(intent, null))
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

    override fun onClickFavorite(stopId: Int) {
        startStopIdActivity(stopId)
    }

    override fun onSearchStopId(stopId: Int) {
        startStopIdActivity(stopId)
    }

    private fun startStopIdActivity(stopId: Int) {
        val b = Bundle().apply {
            putInt(StopIdActivity.KEY_STOP_ID, stopId)
        }
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
        }
    }

    private fun selectBnvSearch() {
        findViewById<View>(R.id.fab).setVisibility(View.VISIBLE)
        findViewById<ViewPager>(R.id.pager).setCurrentItem(ITEM_IDX_SEARCH)
    }

    private fun selectBnvFav() {
        findViewById<View>(R.id.fab).setVisibility(View.GONE)
        findViewById<ViewPager>(R.id.pager).setCurrentItem(ITEM_IDX_FAV)
    }

    private fun hasAnyFavorites(): Boolean {
        val dbHelper = DbAdapter()
        dbHelper.open(applicationContext)
        val ret = dbHelper.hasAnyFavorites()
        dbHelper.close()
        return ret
    }

    private inner class SelectDefaultBnv(): AsyncTask<Void, Void, Int>() {
        override fun doInBackground(vararg params: Void): Int {
            return if (hasAnyFavorites()) BNV_FAV else BNV_SEARCH
        }

        override fun onPostExecute(result: Int) {
            mBnvIdx = result
            val bnv = findViewById<BottomNavigationView>(R.id.bnv)
            when (mBnvIdx) {
                BNV_FAV -> {
                    bnv.menu.findItem(R.id.action_favorite)?.isChecked = true
                    selectBnvFav()
                }
                BNV_SEARCH -> {
                    bnv.menu.findItem(R.id.action_search)?.isChecked = true
                    selectBnvSearch()
                }
            }
        }
    }

    private inner class MainPagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {
        override fun getCount(): Int = 2

        override fun getItem(p0: Int): Fragment =
            when (p0) {
                ITEM_IDX_FAV -> FavoriteStopIdsFragment.newInstance()
                ITEM_IDX_SEARCH -> SearchStopIdFragment.newInstance()
                else -> throw InvalidParameterException("getItem() parameter out of range")
            }

        override fun instantiateItem(container: ViewGroup, i: Int): Any {
            val fragment = super.instantiateItem(container, i)
            if (i == ITEM_IDX_FAV) mFavStopIdsFragment = fragment as FavoriteStopIdsFragment
            return fragment
        }
    }

    companion object {
        private val SOURCE_URL = "https://github.com/sweetiepiggy/Bus-When-Twin-Cities"
        private val KEY_BNV_IDX = "bnvIdx"

        private val BNV_UNINITIALIZED = 0
        private val BNV_FAV = 1
        private val BNV_SEARCH = 2

        private val ITEM_IDX_FAV = 0
        private val ITEM_IDX_SEARCH = 1

        private val ACTIVITY_STOP_ID = 0
    }
}
