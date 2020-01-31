/*
    Copyright (C) 2020 Sweetie Piggy Apps <sweetiepiggyapps@gmail.com>

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

class ContactActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contact)

        setSupportActionBar(findViewById<Toolbar>(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        findViewById<Button>(R.id.phone_button)?.run {
            text = PHONE_NUMBER
            setOnClickListener {
                startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${PHONE_NUMBER}")))
            }
        }
        findViewById<Button>(R.id.text_for_info_button)?.run {
            text = TEXT_FOR_INFO_NUMBER
            setOnClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("sms:")).apply {
                    putExtra("address", TEXT_FOR_INFO_NUMBER)
                })
            }
        }
        findViewById<Button>(R.id.text_for_safety_button)?.run {
            text = TEXT_FOR_SAFETY_NUMBER
            setOnClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("sms:")).apply {
                    putExtra("address", TEXT_FOR_SAFETY_NUMBER)
                })
            }
        }
        findViewById<Button>(R.id.comment_button)?.run {
            setOnClickListener { openLink(COMMENT_URL) }
        }
        findViewById<Button>(R.id.fb_button)?.run {
            text = FACEBOOK_DISPLAY_URL
            setOnClickListener { openLink(FACEBOOK_URL) }
        }
        findViewById<Button>(R.id.twitter_button)?.run {
            text = "@${TWITTER_HANDLE}"
            setOnClickListener { openLink(TWITTER_URL) }
        }
    }

    private fun openLink(url: String) =
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))

    companion object {
        private val PHONE_NUMBER = "612-373-3333"
        private val TEXT_FOR_INFO_NUMBER = "612-444-1161"
        private val TEXT_FOR_SAFETY_NUMBER = "612-900-0411"
        private val FACEBOOK_DISPLAY_URL = "facebook.com/MetroTransitMN"
        private val TWITTER_HANDLE = "MetroTransitMN"

        private val COMMENT_URL = "https://www.metrotransit.org/comment-on-metro-transit-service"
        private val FACEBOOK_URL = "https://www.facebook.com/MetroTransitMN"
        private val TWITTER_URL = "https://twitter.com/${TWITTER_HANDLE}"
    }
}

