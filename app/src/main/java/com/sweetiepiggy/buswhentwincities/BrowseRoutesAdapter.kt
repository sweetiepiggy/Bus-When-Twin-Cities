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

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class BrowseRoutesAdapter(private val mRouteListener: OnClickRouteListener,
        private val mRoutes: MutableList<BrowseRoutesViewModel.Route>) :
            RecyclerView.Adapter<BrowseRoutesAdapter.BrowseRoutesViewHolder>() {

    interface OnClickRouteListener {
        fun onClickRoute(route: BrowseRoutesViewModel.Route)
    }

    inner class BrowseRoutesViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val mDescriptionTextView: TextView = v.findViewById<TextView>(R.id.description)
        init {
            v.setOnClickListener {
                mRouteListener.onClickRoute(mRoutes[adapterPosition])
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BrowseRoutesAdapter.BrowseRoutesViewHolder {
        val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.result_item, parent, false)
        return BrowseRoutesViewHolder(v)
    }

    override fun onBindViewHolder(holder: BrowseRoutesViewHolder, position: Int) {
        holder.mDescriptionTextView.text = mRoutes[position].description
    }

    override fun getItemCount(): Int = mRoutes.size
}
