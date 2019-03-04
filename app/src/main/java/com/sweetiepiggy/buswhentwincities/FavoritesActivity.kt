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
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.appcompat.widget.Toolbar
import android.view.Menu
import android.view.MenuItem

import java.util.ArrayList

import com.sweetiepiggy.buswhentwincities.DbAdapter.KEY_STOP_ID

class FavoritesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorites)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        val bnv = findViewById<BottomNavigationView>(R.id.bnv)
        bnv.menu.findItem(R.id.action_favorites).isChecked = true
        bnv.setOnNavigationItemSelectedListener(
                BottomNavigationView.OnNavigationItemSelectedListener { item ->
                    val intent: Intent
                    when (item.itemId) {
                        R.id.action_search -> {
                            bnv.menu.findItem(R.id.action_favorites).isChecked = true
                            intent = Intent(applicationContext, MainActivity::class.java)
                            startActivity(intent)
                            return@OnNavigationItemSelectedListener true
                        }
                        R.id.action_favorites -> return@OnNavigationItemSelectedListener true
                    }
                    false
                }
        )

        val favoritesRecyclerView = findViewById<View>(R.id.favoritesRecyclerView) as RecyclerView

        favoritesRecyclerView.layoutManager = LinearLayoutManager(this)
        favoritesRecyclerView.addItemDecoration(DividerItemDecoration(favoritesRecyclerView.context,
                DividerItemDecoration.VERTICAL))

        val dbHelper = DbAdapter()
        dbHelper.open(this)
        val favoriteStopIds = ArrayList<String>()
        val c = dbHelper.fetchFavStops()
        val columnIndex = c.getColumnIndex(KEY_STOP_ID)
        while (c.moveToNext()) {
            favoriteStopIds.add(c.getString(columnIndex))
        }
        c.close()
        dbHelper.close()
        favoritesRecyclerView.adapter = FavoriteStopIdsAdapter(applicationContext,
                favoriteStopIds)
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

    companion object {
        private val SOURCE_URL = "https://github.com/sweetiepiggy/Bus-When-Twin-Cities"
    }
}

