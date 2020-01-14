/*
    Copyright (C) 2019-2020 Sweetie Piggy Apps <sweetiepiggyapps@gmail.com>

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
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView

class StopIdAdapter(private val mCtxt: Context) : RecyclerView.Adapter<StopIdAdapter.StopIdViewHolder>() {
    private var mClickMapListener: OnClickMapListener? = null
    private var mNexTrips: List<PresentableNexTrip> = listOf()
    private var mDoShowRoutes: Map<Pair<String?, String?>, Boolean> = mapOf()

    interface OnClickMapListener {
        fun onClickMap(vehicleBlockNumber: Int?)
    }

    inner class StopIdViewHolder(v: View) : RecyclerView.ViewHolder(v), View.OnCreateContextMenuListener {
        val routeTextView: TextView = v.findViewById<TextView>(R.id.route)
        val directionTextView: TextView = v.findViewById<TextView>(R.id.direction)
        val gpsImageView: ImageView = v.findViewById<ImageView>(R.id.gps_icon)
        val descriptionTextView: TextView = v.findViewById<TextView>(R.id.description)
        val departureTextTextView: TextView = v.findViewById<TextView>(R.id.departure_text)
        val departureTimeTextView: TextView = v.findViewById<TextView>(R.id.departure_time)
        val scheduledTextView: TextView = v.findViewById<TextView>(R.id.scheduled)
        val fullView: CardView = v.findViewById<CardView>(R.id.full_view)
        val minimalView: View = v.findViewById<View>(R.id.minimal_view)
        val minimalRouteTextView: TextView = v.findViewById<TextView>(R.id.minimal_route)
        val minimalDescriptionTextView: TextView = v.findViewById<TextView>(R.id.minimal_description)
        val minimalDepartureTextTextView: TextView = v.findViewById<TextView>(R.id.minimal_departure_text)

        init {
            fullView.setOnClickListener {
                /* FIXME: adapterPosition can be out of mNexTrips array bounds,
                	maybe race condition when calling setNexTrips() / notifyAdapter() ? */
                val nexTrip = mNexTrips[adapterPosition]
                if (doShowLocation(nexTrip)) {
                    mClickMapListener!!.onClickMap(nexTrip.blockNumber)
                }
            }
            fullView.setOnCreateContextMenuListener(this)
       }

       override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?) {
           // menu.setHeaderTitle("Header Title")
           // menu.add(Menu.NONE, ACTION_PIN, adapterPosition, R.string.context_menu_pin)
           menu.add(Menu.NONE, ACTION_HIDE, adapterPosition, R.string.context_menu_hide)
       }
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
        val gpsImage = if (nexTrip.isActual ||
                           (nexTrip.position != null && (nexTrip.minutesUntilDeparture?.let {
                                it < NexTrip.MINUTES_BEFORE_TO_SHOW_LOC
                            } ?: false))) {
            if (nexTrip.position != null) {
                // show as gps fixed if position is known and isActual (or if arrival is soon)
                R.drawable.ic_gps_fixed_black_24dp
            } else if (nexTrip.locationSuppressed) {
                // show as gps not fixed if position is suppressed isActual
                R.drawable.ic_gps_not_fixed_black_24dp
            } else {
                // show as gps off if position is not known and isActual
                R.drawable.ic_gps_off_black_24dp
            }
        } else if (nexTrip.position != null || nexTrip.locationSuppressed) {
            // show as gps not fixed if position is known and !isActual
            R.drawable.ic_gps_not_fixed_black_24dp
        } else {
            // show as scheduled if position is not known and !isActual
            R.drawable.ic_schedule_black_24dp
        }
        // holder.gpsImageView.visibility = if (gpsImage == R.drawable.ic_schedule_black_24dp)
        //     View.GONE else View.VISIBLE
        holder.gpsImageView.setImageResource(gpsImage)

        holder.minimalRouteTextView.text = nexTrip.routeAndTerminal
        holder.minimalDescriptionTextView.text = nexTrip.description
        holder.minimalDepartureTextTextView.text = nexTrip.departureText

        if (mDoShowRoutes.get(Pair(nexTrip.route, nexTrip.terminal)) ?: true) {
            holder.minimalView.visibility = View.GONE
            holder.fullView.visibility = View.VISIBLE
        } else {
            holder.fullView.visibility = View.GONE
            holder.minimalView.visibility = View.VISIBLE
        }
    }

    override fun getItemCount(): Int {
        return mNexTrips.size
    }

    fun setNexTrips(nexTrips: List<PresentableNexTrip>) {
        mNexTrips = nexTrips
    }

    fun setDoShowRoutes(doShowRoutes: Map<Pair<String?, String?>, Boolean>) {
        mDoShowRoutes = doShowRoutes
    }

    fun setOnClickMapListener(clickMapListener: StopIdAdapter.OnClickMapListener) {
        mClickMapListener = clickMapListener
    }

    private fun doShowLocation(nexTrip: PresentableNexTrip) =
        nexTrip.position != null && (nexTrip.isActual || (nexTrip.minutesUntilDeparture?.let { it < NexTrip.MINUTES_BEFORE_TO_SHOW_LOC } ?: false))

    companion object {
        val ACTION_PIN = 0
        val ACTION_HIDE = 1
    }
}
