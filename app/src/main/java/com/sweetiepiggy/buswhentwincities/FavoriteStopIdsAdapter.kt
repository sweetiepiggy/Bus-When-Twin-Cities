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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class FavoriteStopIdsAdapter(private val mClickFavoriteListener: OnClickFavoriteListener, private val mFavStops: List<FavoriteStopIdsViewModel.FavoriteStopId>) : RecyclerView.Adapter<FavoriteStopIdsAdapter.FavoriteStopIdsViewHolder>() {

    interface OnClickFavoriteListener {
        fun onClickFavorite(stopId: String)
    }

    inner class FavoriteStopIdsViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        var mStopIdTextView: TextView
        var mStopDescTextView: TextView

        init {
            mStopIdTextView = v.findViewById<TextView>(R.id.stop_id)
            mStopDescTextView = v.findViewById<TextView>(R.id.stop_desc)
            v.setOnClickListener {
                val stopId = mFavStops[adapterPosition].stopId
                mClickFavoriteListener.onClickFavorite(stopId)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoriteStopIdsAdapter.FavoriteStopIdsViewHolder {
        val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.fav_stop_id_item, parent, false)
        return FavoriteStopIdsViewHolder(v)
    }

    override fun onBindViewHolder(holder: FavoriteStopIdsViewHolder, position: Int) {
        holder.mStopIdTextView.text = mFavStops[position].stopId
        holder.mStopDescTextView.text = mFavStops[position].stopDesc
    }

    override fun getItemCount(): Int {
        return mFavStops.size
    }

    companion object {
        private val KEY_STOP_ID = "stopId"
    }
}
