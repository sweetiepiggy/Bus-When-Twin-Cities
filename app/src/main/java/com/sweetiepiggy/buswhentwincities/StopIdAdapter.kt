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

class StopIdAdapter(private val mCtxt: Context) : RecyclerView.Adapter<StopIdAdapter.StopIdViewHolder>() {
    private var mClickMapListener: OnClickMapListener? = null
    private var mNexTrips: List<PresentableNexTrip> = listOf()
    private var mHiddenRoutes: MutableSet<String> = mutableSetOf()

    interface OnClickMapListener {
        fun onClickMap(vehicleBlockNumber: Int?)
    }

    inner class StopIdViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val routeTextView: TextView = v.findViewById<TextView>(R.id.route)
        val directionTextView: TextView = v.findViewById<TextView>(R.id.direction)
        val descriptionTextView: TextView = v.findViewById<TextView>(R.id.description)
        val departureTextTextView: TextView = v.findViewById<TextView>(R.id.departure_text)
        val departureTimeTextView: TextView = v.findViewById<TextView>(R.id.departure_time)
        val scheduledTextView: TextView = v.findViewById<TextView>(R.id.scheduled)
        val mapButton: ImageButton = v.findViewById<ImageButton>(R.id.map_button).apply {
            setOnClickListener {
                mClickMapListener!!.onClickMap(mNexTrips[adapterPosition].blockNumber)
            }
        }
        val fullView: View = v.findViewById<View>(R.id.full_view)
        val minimalView: View = v.findViewById<View>(R.id.minimal_view)
        val minimalRouteTextView: TextView = v.findViewById<TextView>(R.id.minimal_route)
        val minimalDescriptionTextView: TextView = v.findViewById<TextView>(R.id.minimal_description)
        val minimalDepartureTextTextView: TextView = v.findViewById<TextView>(R.id.minimal_departure_text)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StopIdAdapter.StopIdViewHolder =
    	StopIdViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.stop_id_item, parent, false)
        )

    override fun onBindViewHolder(holder: StopIdViewHolder, position: Int) {
        val nexTrip = mNexTrips[position]
        holder.routeTextView.text = nexTrip.routeAndTerminal
        holder.directionTextView.text = nexTrip.routeDirectionStr
        holder.descriptionTextView.text = nexTrip.description
        holder.departureTextTextView.text = nexTrip.departureText
        holder.departureTimeTextView.text = nexTrip.departureTime
        holder.scheduledTextView.text =
        	mCtxt.resources.getString(if (nexTrip.isActual) R.string.real_time else R.string.scheduled)
        holder.departureTimeTextView.visibility =
        	if (nexTrip.departureTime == null) View.GONE else View.VISIBLE
        holder.mapButton.visibility = if (nexTrip.isActual && nexTrip.position != null)
        	View.VISIBLE else View.GONE

        holder.minimalRouteTextView.text = nexTrip.routeAndTerminal
        holder.minimalDescriptionTextView.text = nexTrip.description
        holder.minimalDepartureTextTextView.text = nexTrip.departureText

        if (mHiddenRoutes.contains(nexTrip.routeAndTerminal)) {
            holder.fullView.visibility = View.GONE
            holder.minimalView.visibility = View.VISIBLE
        } else {
            holder.minimalView.visibility = View.GONE
            holder.fullView.visibility = View.VISIBLE
        }
    }

    override fun getItemCount(): Int {
        return mNexTrips.size
    }

    fun setNexTrips(nexTrips: List<PresentableNexTrip>) {
        mNexTrips = nexTrips
    }

    fun setHiddenRoutes(hiddenRoutes: MutableSet<String>) {
        mHiddenRoutes = hiddenRoutes
    }

    fun setOnClickMapListener(clickMapListener: StopIdAdapter.OnClickMapListener) {
        mClickMapListener = clickMapListener
    }
}
