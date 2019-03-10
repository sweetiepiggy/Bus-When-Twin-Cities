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
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.sweetiepiggy.buswhentwincities.ui.favoritestopids.FavoriteStopIdsFragment

class MainActivity : AppCompatActivity(), FavoriteStopIdsAdapter.OnClickFavoriteListener, BottomNavigationView.OnNavigationItemSelectedListener {
    private var mBnvIdx = BNV_UNINITIALIZED

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
        mBnvIdx = b.getInt(KEY_BNV_IDX) ?: BNV_UNINITIALIZED
    }

    public override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        loadState(savedInstanceState)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val intent: Intent
        when (item.itemId) {
            R.id.action_about -> {
                intent = Intent(this, AboutActivity::class.java)
                startActivity(intent)
                return true
            }
            R.id.action_source -> {
                intent = Intent(Intent.ACTION_VIEW)
                intent.setDataAndType(Uri.parse(SOURCE_URL), "text/html")
                startActivity(Intent.createChooser(intent, null))
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
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

    override fun onClickFavorite(stopId: String) {
        val b = Bundle().apply {
            putString(KEY_STOP_ID, stopId)
        }
        val intent = Intent(this, StopIdActivity::class.java).apply {
            putExtras(b)
        }
        startActivity(intent)
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

    private class MainPagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {
        override fun getCount(): Int = 2

        override fun getItem(i: Int): Fragment? =
            when (i) {
                ITEM_IDX_FAV -> FavoriteStopIdsFragment.newInstance()
                ITEM_IDX_SEARCH -> SearchStopIdFragment.newInstance()
                else -> null
            }
    }

    companion object {
        private val SOURCE_URL = "https://github.com/sweetiepiggy/Bus-When-Twin-Cities"
        private val KEY_STOP_ID = "stopId"
        private val KEY_BNV_IDX = "bnvIdx"

        private val BNV_UNINITIALIZED = 0
        private val BNV_FAV = 1
        private val BNV_SEARCH = 2

        private val ITEM_IDX_FAV = 0
        private val ITEM_IDX_SEARCH = 1
    }
}
