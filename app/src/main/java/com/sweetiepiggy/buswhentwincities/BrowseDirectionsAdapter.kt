/*
    Copyright (C) 2019,2021 Sweetie Piggy Apps <sweetiepiggyapps@gmail.com>

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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class BrowseDirectionsAdapter(private val mDirectionListener: OnClickDirectionListener,
                              private val mCtxt: Context, private val mRouteId: String?,
                              private val mDirections: MutableList<Pair<Int, String>>) :
            RecyclerView.Adapter<BrowseDirectionsAdapter.BrowseDirectionsViewHolder>() {

    interface OnClickDirectionListener {
        fun onClickDirection(routeId: String?, directionId: Int)
    }

    inner class BrowseDirectionsViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val mDescriptionTextView: TextView = v.findViewById<TextView>(R.id.description)
        init {
            v.setOnClickListener {
                mDirectionListener.onClickDirection(mRouteId,
                                                    mDirections[adapterPosition].first)
            }
        }
    }

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BrowseDirectionsAdapter.BrowseDirectionsViewHolder {
        val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.result_item, parent, false)
        return BrowseDirectionsViewHolder(v)
    }

    override fun onBindViewHolder(holder: BrowseDirectionsViewHolder, position: Int) {
        holder.mDescriptionTextView.text =
            PresentableNexTrip.translateDirectionBound(mDirections[position].second,
                                                       mCtxt.resources)
    }

    override fun getItemCount(): Int = mDirections.size

    override fun getItemId(position: Int): Long =
        mDirections[position].first.toLong()
}
