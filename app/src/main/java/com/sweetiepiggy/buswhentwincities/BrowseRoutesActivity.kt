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
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar

class BrowseRoutesActivity : AppCompatActivity(), BrowseRoutesAdapter.OnClickRouteListener, OnDownloadErrorListener, OnChangeRefreshingListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.browse_routes_activity)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                    .replace(R.id.container, BrowseRoutesFragment.newInstance(this, this, this))
                    .commitNow()
        }

        setSupportActionBar(findViewById<Toolbar>(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        title = resources.getString(R.string.select_route)
    }

    override fun onClickRoute(route: BrowseRoutesViewModel.Route) {
        val intent = Intent(this, BrowseDirectionsActivity::class.java).apply {
            putExtras(Bundle().apply {
                putString(BrowseDirectionsActivity.KEY_ROUTE_ID, route.routeId)
            })
        }
        startActivity(intent)
    }

    override fun onDownloadError(err: MetroTransitDownloader.DownloadError) {
        if (isFinishing()) {
            return
        }
        val message: String? =
            when (err) {
                is MetroTransitDownloader.DownloadError.UnknownHost -> resources.getString(R.string.unknown_host)
                is MetroTransitDownloader.DownloadError.FileNotFound -> resources.getString(R.string.file_not_found) + ":\n${err.message}"
                is MetroTransitDownloader.DownloadError.TimedOut -> resources.getString(R.string.timed_out) + ":\n${err.message}"
                is MetroTransitDownloader.DownloadError.OtherDownloadError -> err.message
            }
        AlertDialog.Builder(this).apply {
            setTitle(resources.getString(android.R.string.dialog_alert_title))
            message?.let { setMessage(it) }
            setPositiveButton(resources.getString(R.string.dismiss)) { _, _ -> }
        }.show()
    }

    override fun setRefreshing(refreshing: Boolean) {
        findViewById<View>(R.id.progressBar)?.setVisibility(if (refreshing) View.VISIBLE else View.INVISIBLE)
    }
}
