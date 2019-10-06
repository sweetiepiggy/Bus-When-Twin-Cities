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
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar

class BrowseDirectionsActivity : AppCompatActivity(), BrowseDirectionsAdapter.OnClickDirectionListener {

    private var mRouteId: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.browse_routes_activity)
        if (savedInstanceState == null) {
            intent.extras?.let { loadState(it) }
            val fragment = BrowseDirectionsFragment.newInstance().apply {
                setArguments(Bundle().apply {
                    mRouteId?.let { putInt(BrowseDirectionsFragment.KEY_ROUTE_ID, it) }
                })
            }
            supportFragmentManager.beginTransaction()
                    .replace(R.id.container, fragment)
                    .commitNow()
        } else {
            loadState(savedInstanceState)
        }

        setSupportActionBar(findViewById<Toolbar>(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        title = resources.getString(R.string.select_direction)
    }

    public override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)
        mRouteId?.let { savedInstanceState.putInt(KEY_ROUTE_ID, it) }
    }

    private fun loadState(b: Bundle) {
        if (b.containsKey(KEY_ROUTE_ID)) {
            mRouteId = b.getInt(KEY_ROUTE_ID)
        }
    }

    override fun onClickDirection(routeId: Int?, direction: NexTrip.Direction) {
        val intent = Intent(this, BrowseTimestopsActivity::class.java).apply {
            putExtras(Bundle().apply {
                routeId?.let { putInt(BrowseTimestopsFragment.KEY_ROUTE_ID, it) }
                putInt(BrowseTimestopsFragment.KEY_DIRECTION_ID, NexTrip.getDirectionId(direction))
            })
        }
        startActivity(intent)
    }

    companion object {
        val KEY_ROUTE_ID = "routeId"
    }
}
