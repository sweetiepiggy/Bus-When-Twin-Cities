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

class StopIdAdapter(private val mCtxt: Context, private val mNexTrips: List<PresentableNexTrip>) : RecyclerView.Adapter<StopIdAdapter.StopIdViewHolder>() {
    private var mClickMapListener: OnClickMapListener? = null

    interface OnClickMapListener {
        fun onClickMap(vehicleBlockNumber: Int?)
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
                mClickMapListener!!.onClickMap(mNexTrips[adapterPosition].blockNumber)
            }
        }
    }

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StopIdAdapter.StopIdViewHolder {
        val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.stop_id_item, parent, false)
        return StopIdViewHolder(v)
    }

    override fun onBindViewHolder(holder: StopIdViewHolder, position: Int) {
        val nexTrip = mNexTrips[position]
        holder.mRouteTextView.text = nexTrip.routeAndTerminal
        holder.mDirectionTextView.text = nexTrip.routeDirection
        holder.mDescriptionTextView.text = nexTrip.description
        holder.mDepartureTextTextView.text = nexTrip.departureText
        holder.mDepartureTimeTextView.text = nexTrip.departureTime
        holder.mScheduledTextView.text =
        	mCtxt.resources.getString(if (nexTrip.isActual) R.string.real_time else R.string.scheduled)
        holder.mDepartureTimeTextView.visibility =
        	if (nexTrip.departureTime == null) View.GONE else View.VISIBLE
        holder.mMapButton.visibility = if (nexTrip.isActual) View.VISIBLE else View.GONE
    }

    override fun getItemCount(): Int {
        return mNexTrips.size
    }

    override fun getItemId(position: Int): Long =
    	mNexTrips[position].blockNumber?.toLong() ?: RecyclerView.NO_ID

    fun setOnClickMapListener(clickMapListener: StopIdAdapter.OnClickMapListener) {
        mClickMapListener = clickMapListener
    }
}
