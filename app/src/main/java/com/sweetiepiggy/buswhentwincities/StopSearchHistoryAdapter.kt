/*
    Copyright (C) 2020 Sweetie Piggy Apps <sweetiepiggyapps@gmail.com>

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

import android.view.*
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.*
import androidx.recyclerview.widget.RecyclerView

class StopSearchHistoryAdapter(private val mListener: OnClickListener,
        private val mSearchedStops: MutableList<StopSearchHistoryViewModel.SearchedStop>) :
            RecyclerView.Adapter<StopSearchHistoryAdapter.StopSearchHistoryViewHolder>() {

    val mItemTouchHelper = ItemTouchHelper(StopSearchHistoryItemTouchHelperCallback())

    interface OnClickListener {
        fun onClick(stop: StopSearchHistoryViewModel.SearchedStop)
        fun onDelete(removedStop: StopSearchHistoryViewModel.SearchedStop, position: Int, recyclerViewPosition: Int)
    }

    inner class StopSearchHistoryViewHolder(v: View) : RecyclerView.ViewHolder(v), View.OnCreateContextMenuListener {
        var mDescriptionTextView: TextView = v.findViewById<TextView>(R.id.description)
        init {
            v.setOnClickListener {
                mListener.onClick(mSearchedStops[adapterPosition])
            }
            v.setOnCreateContextMenuListener(this)
        }

        override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?) {
            menu.add(Menu.NONE, ACTION_REMOVE, adapterPosition, R.string.context_menu_remove)
        }
    }

    init {
        setHasStableIds(true)
    }

    fun attachToRecyclerView(v: RecyclerView) = mItemTouchHelper.attachToRecyclerView(v)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StopSearchHistoryAdapter.StopSearchHistoryViewHolder {
        val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.result_item, parent, false)
        return StopSearchHistoryViewHolder(v)
    }

    override fun onBindViewHolder(holder: StopSearchHistoryViewHolder, position: Int) {
        holder.mDescriptionTextView.text = mSearchedStops[position].stopId?.toString()
    }

    override fun getItemCount(): Int = mSearchedStops.size

    override fun getItemId(position: Int): Long = mSearchedStops[position].searchDatetime.toLong()

    private inner class StopSearchHistoryItemTouchHelperCallback : ItemTouchHelper.Callback() {
        override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int =
            makeMovementFlags(0, START or END)

        override fun isLongPressDragEnabled() = false

        override fun isItemViewSwipeEnabled() = true

        override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder): Boolean {
            return false
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            val position = viewHolder.getAdapterPosition()
            val removedStop = mSearchedStops.removeAt(position)
            notifyItemRemoved(position)
            // note: positions in adapter are reversed from positions in database
            mListener.onDelete(removedStop, mSearchedStops.size - position, position)
        }
    }

    companion object {
        val ACTION_REMOVE = 0
    }
}

