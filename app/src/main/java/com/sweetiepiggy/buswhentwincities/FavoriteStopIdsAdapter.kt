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

import android.view.KeyEvent.ACTION_DOWN
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.DOWN
import androidx.recyclerview.widget.ItemTouchHelper.UP
import androidx.recyclerview.widget.RecyclerView

class FavoriteStopIdsAdapter(private val mFavoriteListener: OnClickFavoriteListener,
		private val mFavStops: MutableList<FavoriteStopIdsViewModel.FavoriteStopId>) :
			RecyclerView.Adapter<FavoriteStopIdsAdapter.FavoriteStopIdsViewHolder>() {

    val mItemTouchHelper = ItemTouchHelper(FavoriteStopIdsItemTouchHelperCallback())

    interface OnClickFavoriteListener {
        fun onClickFavorite(favStop: FavoriteStopIdsViewModel.FavoriteStopId)
        fun onMoveFavorite(fromPosition: Int, toPosition: Int)
        fun onDeleteFavorite(position: Int)
    }

    inner class FavoriteStopIdsViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        var mStopIdTextView: TextView = v.findViewById<TextView>(R.id.stop_id)
        var mStopDescTextView: TextView = v.findViewById<TextView>(R.id.stop_desc)
        var mReorderView: View = v.findViewById<View>(R.id.reorder).apply {
            setOnTouchListener onTouchListener@{ _, event ->
                if (event.getActionMasked() == ACTION_DOWN) {
                    mItemTouchHelper.startDrag(this@FavoriteStopIdsViewHolder)
                }
                return@onTouchListener false
            }
        }

        init {
            v.setOnClickListener {
                mFavoriteListener.onClickFavorite(mFavStops[adapterPosition])
            }
        }
    }

    init {
        setHasStableIds(true)
    }

    fun attachToRecyclerView(v: RecyclerView) = mItemTouchHelper.attachToRecyclerView(v)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoriteStopIdsAdapter.FavoriteStopIdsViewHolder {
        val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.fav_stop_id_item, parent, false)
        return FavoriteStopIdsViewHolder(v)
    }

    override fun onBindViewHolder(holder: FavoriteStopIdsViewHolder, position: Int) {
        holder.mStopIdTextView.text = mFavStops[position].stopId.toString()
        holder.mStopDescTextView.text = mFavStops[position].stopDesc
    }

    override fun getItemCount(): Int = mFavStops.size

    override fun getItemId(position: Int): Long = mFavStops[position].stopId.toLong()

    private inner class FavoriteStopIdsItemTouchHelperCallback : ItemTouchHelper.Callback() {
        override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int =
        	// makeMovementFlags(UP or DOWN, START or END)
        	makeMovementFlags(UP or DOWN, 0)

        override fun isLongPressDragEnabled() = false

        override fun isItemViewSwipeEnabled() = true

        override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder,
        		target: RecyclerView.ViewHolder): Boolean {

            val fromPosition = viewHolder.getAdapterPosition()
            val toPosition = target.getAdapterPosition()
            // val updatedToPosition = toPosition - if (fromPosition < toPosition) 1 else 0

            mFavStops.add(toPosition, mFavStops.removeAt(fromPosition))
            // note: positions in adapter are reversed from positions in database
			mFavoriteListener.onMoveFavorite(mFavStops.size - fromPosition - 1,
                    mFavStops.size - toPosition - 1)
            return true
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            val position = viewHolder.getAdapterPosition()
            mFavStops.removeAt(position)
            notifyItemRemoved(position)
            // note: positions in adapter are reversed from positions in database
            mFavoriteListener.onDeleteFavorite(mFavStops.size - position)
        }
    }
}
