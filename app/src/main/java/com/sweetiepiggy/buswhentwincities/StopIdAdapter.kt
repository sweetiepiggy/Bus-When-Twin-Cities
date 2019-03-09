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

import android.content.Context
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.*

class StopIdAdapter(private val mCtxt: Context, private val mNexTrips: List<NexTrip>) : RecyclerView.Adapter<StopIdAdapter.StopIdViewHolder>() {
    private var mClickMapListener: OnClickMapListener? = null

    interface OnClickMapListener {
        fun onClickMap(nexTrip: NexTrip)
    }

    inner class StopIdViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        var mRouteTextView: TextView
        var mDirectionTextView: TextView
        var mDescriptionTextView: TextView
        var mDepartureTextTextView: TextView
        var mDepartureTimeTextView: TextView
        var mScheduledTextView: TextView
        var mMapButton: ImageButton

        init {
            mRouteTextView = v.findViewById<TextView>(R.id.route)
            mDirectionTextView = v.findViewById<TextView>(R.id.direction)
            mDescriptionTextView = v.findViewById<TextView>(R.id.description)
            mDepartureTextTextView = v.findViewById<TextView>(R.id.departure_text)
            mDepartureTimeTextView = v.findViewById<TextView>(R.id.departure_time)
            mScheduledTextView = v.findViewById<TextView>(R.id.scheduled)
            mMapButton = v.findViewById<ImageButton>(R.id.map_button)
            mMapButton.setOnClickListener {
                mClickMapListener!!.onClickMap(mNexTrips[adapterPosition])
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StopIdAdapter.StopIdViewHolder {
        val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.stop_id_item, parent, false)
        return StopIdViewHolder(v)
    }

    override fun onBindViewHolder(holder: StopIdViewHolder, position: Int) {
        val nexTrip = mNexTrips[position]
        holder.mRouteTextView.text = nexTrip.route + nexTrip.terminal
        holder.mDirectionTextView.text = nexTrip.routeDirection?.let { translateDirection(it) }
        holder.mDescriptionTextView.text = nexTrip.description

        val departureTimeInMillis = parseDepartureTime(nexTrip.departureTime)
        val millisUntilDeparture = departureTimeInMillis - Calendar.getInstance().timeInMillis
        val minutesUntilDeparture = millisUntilDeparture / 1000 / 60
        if (departureTimeInMillis < 0 || millisUntilDeparture < 0) {
            holder.mDepartureTextTextView.text = translateDepartureText(nexTrip.departureText)
            holder.mDepartureTimeTextView.visibility = View.GONE
        } else if (minutesUntilDeparture < 60) {
            val resources = mCtxt?.resources
            holder.mDepartureTextTextView.text = if (minutesUntilDeparture < 1)
                 resources?.getString(R.string.due) ?: "Due"
            else
                 java.lang.Long.toString(minutesUntilDeparture) +
                         " " + (resources?.getString(R.string.minutes) ?: "min")
            holder.mDepartureTimeTextView.visibility = View.VISIBLE
            holder.mDepartureTimeTextView.text = DateFormat.getTimeFormat(mCtxt).format(Date(departureTimeInMillis))
        } else {
            holder.mDepartureTextTextView.text = DateFormat.getTimeFormat(mCtxt).format(Date(departureTimeInMillis))
            holder.mDepartureTimeTextView.visibility = View.GONE
         }

         holder.mMapButton.visibility = if (nexTrip.isActual) View.VISIBLE else View.GONE

         holder.mScheduledTextView.text = mCtxt.resources.getString(if (nexTrip.isActual)
             R.string.real_time
         else
             R.string.scheduled)
    }

    override fun getItemCount(): Int {
        return mNexTrips.size
    }

    fun setOnClickMapListener(clickMapListener: StopIdAdapter.OnClickMapListener) {
        mClickMapListener = clickMapListener
    }

    internal fun parseDepartureTime(departureTime: String?): Long {
        if (departureTime != null && departureTime.startsWith("/Date(")) {
            var timezoneIdx = departureTime.indexOf('-', 6)
            if (timezoneIdx < 0) {
                timezoneIdx = departureTime.indexOf('+', 6)
                if (timezoneIdx < 0) {
                    return -1
                }
            }
            return java.lang.Long.parseLong(departureTime.substring(6, timezoneIdx))
        }
        return -1
    }

    internal fun translateDepartureText(departureText: String?): String? {
        return if (departureText != null && departureText.endsWith(" Min")) {
            departureText.substring(0, departureText.length - 3) + (mCtxt?.resources?.getString(R.string.minutes) ?: "min")
        } else {
            departureText
        }
    }

    internal fun translateDirection(dir: String): String {
        val resources = mCtxt?.resources
        return if (resources != null) {
            when (dir) {
                "SOUTHBOUND" -> resources.getString(R.string.south)
                "EASTBOUND" -> resources.getString(R.string.east)
                "WESTBOUND" -> resources.getString(R.string.west)
                "NORTHBOUND" -> resources.getString(R.string.north)
                else -> dir
            }
        } else {
            dir
        }
    }
}
