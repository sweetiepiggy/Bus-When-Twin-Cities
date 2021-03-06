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

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate.*
import androidx.preference.PreferenceManager

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // see: https://developer.android.com/guide/topics/ui/settings/use-saved-values
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        when (sharedPreferences.getString("nightmode", "")) {
            "yes"           -> setDefaultNightMode(MODE_NIGHT_YES)
            "no"            -> setDefaultNightMode(MODE_NIGHT_NO)
            "follow_system" -> setDefaultNightMode(MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }

}

