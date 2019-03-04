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

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (savedInstanceState != null) {
            restoreSavedState(savedInstanceState)
        }

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        (findViewById<View>(R.id.stopIdEntry) as EditText)
                .setOnEditorActionListener(object : TextView.OnEditorActionListener {
                    override fun onEditorAction(v: TextView, actionId: Int, event: KeyEvent): Boolean {
                        if (actionId == EditorInfo.IME_ACTION_DONE) {
                            startStopIdActivity()
                            return true
                        } else {
                            return false
                        }
                    }
                })

        val fab = findViewById<FloatingActionButton>(R.id.fab)
        fab.setOnClickListener { startStopIdActivity() }

        val bnv = findViewById<BottomNavigationView>(R.id.bnv)
        bnv.menu.findItem(R.id.action_search).isChecked = true
        bnv.setOnNavigationItemSelectedListener(
                BottomNavigationView.OnNavigationItemSelectedListener { item ->
                    val intent: Intent
                    when (item.itemId) {
                        R.id.action_search -> return@OnNavigationItemSelectedListener true
                        R.id.action_favorites -> {
                            bnv.menu.findItem(R.id.action_search).isChecked = true
                            intent = Intent(applicationContext, FavoritesActivity::class.java)
                            startActivity(intent)
                            return@OnNavigationItemSelectedListener true
                        }
                    }
                    false
                }
        )
    }

    public override fun onSaveInstanceState(savedInstanceState: Bundle) {
        savedInstanceState.putString("stopId",
                (findViewById<View>(R.id.stopIdEntry) as EditText).text.toString())
    }

    public override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        restoreSavedState(savedInstanceState)
    }

    private fun restoreSavedState(savedInstanceState: Bundle) {
        val stopId = savedInstanceState.getString("stopId")
        (findViewById<View>(R.id.stopIdEntry) as EditText).setText(stopId)
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

    private fun startStopIdActivity() {
        val stopId = (findViewById<View>(R.id.stopIdEntry) as EditText).text.toString()
        if (stopId.length == 0) {
            (findViewById<View>(R.id.stopIdEntry) as EditText).error = resources.getString(R.string.enter_stop_id)
        } else {
            val intent = Intent(applicationContext, StopIdActivity::class.java)
            val b = Bundle()
            b.putString("stopId", stopId)
            intent.putExtras(b)
            startActivity(intent)
        }
    }

    companion object {
        private val SOURCE_URL = "https://github.com/sweetiepiggy/Bus-When-Twin-Cities"
    }
}
