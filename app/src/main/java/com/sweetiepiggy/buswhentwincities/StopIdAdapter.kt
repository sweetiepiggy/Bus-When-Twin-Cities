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
import android.content.Intent
import android.os.Bundle
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView

class StopIdAdapter(private val mCtxt: Context, private val mNexTrips: List<NexTrip>) : RecyclerView.Adapter<StopIdAdapter.StopIdViewHolder>() {

    inner class StopIdViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        var mRouteTextView: TextView
        var mDirectionTextView: TextView
        var mDescriptionTextView: TextView
        var mDepartureTextTextView: TextView
        var mDepartureTimeTextView: TextView
        var mScheduledTextView: TextView
        var mMapButton: ImageButton

        init {
            mRouteTextView = v.findViewById<View>(R.id.route) as TextView
            mDirectionTextView = v.findViewById<View>(R.id.direction) as TextView
            mDescriptionTextView = v.findViewById<View>(R.id.description) as TextView
            mDepartureTextTextView = v.findViewById<View>(R.id.departure_text) as TextView
            mDepartureTimeTextView = v.findViewById<View>(R.id.departure_time) as TextView
            mScheduledTextView = v.findViewById<View>(R.id.scheduled) as TextView
            mMapButton = v.findViewById<View>(R.id.map_button) as ImageButton
            mMapButton.setOnClickListener {
                // NexTrip nexTrip = mNexTrips.get(getAdapterPosition());
                // String latitudeStr = Double.toString(nexTrip.getVehicleLatitude());
                // String longitudeStr = Double.toString(nexTrip.getVehicleLongitude());
                // Uri uri = Uri.parse("geo:" + latitudeStr + "," + longitudeStr + "?q="
                //                     + Uri.encode(latitudeStr + "," + longitudeStr + "("
                //                                  + mCtxt.getResources().getString(R.string.bus)
                //                                  + " " + nexTrip.getRoute()
                //                                  + nexTrip.getTerminal() + ")"));
                // Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                // intent.setPackage("com.google.android.apps.maps");
                // intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                // mCtxt.startActivity(intent);
                val nexTrip = mNexTrips[adapterPosition]
                val intent = Intent(mCtxt, MapsActivity::class.java)
                val b = Bundle()
                b.putString("routeAndTerminal", nexTrip.route + nexTrip.terminal)
                b.putString("departureText", nexTrip.departureText)
                b.putDouble("vehicleLatitude", nexTrip.vehicleLatitude)
                b.putDouble("vehicleLongitude", nexTrip.vehicleLongitude)
                intent.putExtras(b)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                mCtxt.startActivity(intent)
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
        holder.mDirectionTextView.text = nexTrip.routeDirection
        holder.mDescriptionTextView.text = nexTrip.description
        holder.mDepartureTextTextView.text = nexTrip.departureText
        if (nexTrip.departureTime!!.isEmpty()) {
            holder.mDepartureTimeTextView.visibility = View.GONE
        } else {
            holder.mDepartureTimeTextView.visibility = View.VISIBLE
            holder.mDepartureTimeTextView.text = nexTrip.departureTime
        }

        holder.mMapButton.visibility = if (nexTrip.isActual) View.VISIBLE else View.GONE

        // holder.mMapButton.setVisibility(View.GONE);
        holder.mScheduledTextView.text = mCtxt.resources.getString(if (nexTrip.isActual)
            R.string.real_time
        else
            R.string.scheduled)
    }

    override fun getItemCount(): Int {
        return mNexTrips.size
    }

}
