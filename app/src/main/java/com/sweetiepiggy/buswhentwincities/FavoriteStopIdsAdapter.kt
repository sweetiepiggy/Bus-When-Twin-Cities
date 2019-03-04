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
import android.widget.TextView

class FavoriteStopIdsAdapter(private val mContext: Context, private val mFavStopIds: List<String>) : RecyclerView.Adapter<FavoriteStopIdsAdapter.FavoriteStopIdsViewHolder>() {

    inner class FavoriteStopIdsViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        var mStopIdTextView: TextView

        init {
            mStopIdTextView = v.findViewById<View>(R.id.stop_id) as TextView
            v.setOnClickListener {
                val stopId = mFavStopIds[adapterPosition]
                val intent = Intent(mContext, StopIdActivity::class.java)
                val b = Bundle()
                b.putString("stopId", stopId)
                intent.putExtras(b)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                mContext.startActivity(intent)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoriteStopIdsAdapter.FavoriteStopIdsViewHolder {
        val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.fav_stop_id_item, parent, false)
        return FavoriteStopIdsViewHolder(v)
    }

    override fun onBindViewHolder(holder: FavoriteStopIdsViewHolder, position: Int) {
        holder.mStopIdTextView.text = mFavStopIds[position]
    }

    override fun getItemCount(): Int {
        return mFavStopIds.size
    }

}
