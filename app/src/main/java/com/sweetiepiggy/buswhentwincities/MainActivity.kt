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
import android.os.Bundle
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.sweetiepiggy.buswhentwincities.ui.favoritestopids.FavoriteStopIdsFragment

class MainActivity : AppCompatActivity() {
    private var mFavFragment: Fragment? = null
    private var mSearchFragment: Fragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (savedInstanceState == null) {
            val fragment = if (hasAnyFavorites()) {
                mFavFragment = FavoriteStopIdsFragment.newInstance()
                mFavFragment
            } else {
                mSearchFragment = SearchStopIdFragment.newInstance()
                mSearchFragment
            }
            supportFragmentManager.beginTransaction()
                    .replace(R.id.container, fragment!!)
                    .commitNow()
        }

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        val bnv = findViewById<BottomNavigationView>(R.id.bnv)
        bnv.setOnNavigationItemSelectedListener(
                BottomNavigationView.OnNavigationItemSelectedListener { item ->
                    var fragment: Fragment? = null
                    when (item.itemId) {
                        R.id.action_search -> {
                            if (mSearchFragment == null){
                                mSearchFragment = SearchStopIdFragment.newInstance();
                            }
                            fragment = mSearchFragment
                        }
                        R.id.action_favorites -> {
                            if (mFavFragment == null){
                                mFavFragment = FavoriteStopIdsFragment.newInstance();
                            }
                            fragment = mFavFragment
                        }
                        else -> return@OnNavigationItemSelectedListener false
                    }

                    supportFragmentManager.beginTransaction()
                            .replace(R.id.container, fragment!!)
                            .commitNow()
                    true
                }
        )
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val intent: Intent
        when (item.itemId) {
            R.id.action_about -> {
                intent = Intent(applicationContext, AboutActivity::class.java)
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

    fun hasAnyFavorites(): Boolean {
        return true
    }

    companion object {
        private val SOURCE_URL = "https://github.com/sweetiepiggy/Bus-When-Twin-Cities"
    }
}
