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
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar

class AboutActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        findViewById<Button>(R.id.patreon_button)?.setOnClickListener {
            openLink(PATREON_URL)
        }
        findViewById<Button>(R.id.facebook_button)?.setOnClickListener {
            openLink(FACEBOOK_URL)
        }
        findViewById<Button>(R.id.twitter_button)?.setOnClickListener {
            openLink(TWITTER_URL)
        }
        findViewById<Button>(R.id.github_button)?.setOnClickListener {
            openLink(GITHUB_URL)
        }
        findViewById<Button>(R.id.rate_button)?.setOnClickListener {
            openLink(RATE_URL)
        }
        findViewById<Button>(R.id.license_button)?.setOnClickListener {
            openLink(LICENSE_URL)
        }
    }

    private fun openLink(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }

    companion object {
        private val PATREON_URL = "https://patreon.com/sweetiepiggyapps"
        private val FACEBOOK_URL = "https://www.facebook.com/Bus-When-Twin-Cities-2295035200768909/"
        private val TWITTER_URL = "https://twitter.com/sweetiepiggyapp"
        private val GITHUB_URL = "https://github.com/sweetiepiggy"
        private val RATE_URL = "https://play.google.com/store/apps/details?id=com.sweetiepiggy.buswhentwincities"
        private val LICENSE_URL = "https://www.gnu.org/licenses/gpl.html"
    }
}
