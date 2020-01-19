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

import android.view.*
import android.view.KeyEvent.ACTION_DOWN
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.*
import androidx.recyclerview.widget.RecyclerView

class FavoriteStopIdsAdapter(private val mFavoriteListener: OnClickFavoriteListener,
		private val mFavStops: MutableList<FavoriteStopIdsViewModel.FavoriteStop>) :
			RecyclerView.Adapter<FavoriteStopIdsAdapter.FavoriteStopIdsViewHolder>() {

    val mItemTouchHelper = ItemTouchHelper(FavoriteStopIdsItemTouchHelperCallback())

    interface OnClickFavoriteListener {
        fun onClickFavorite(favStop: FavoriteStopIdsViewModel.FavoriteStop)
        fun onMoveFavorite(fromPosition: Int, toPosition: Int)
        fun onPromptDeleteFavorite(removedStop: FavoriteStopIdsViewModel.FavoriteStop, position: Int, recyclerViewPosition: Int)
    }

    inner class FavoriteStopIdsViewHolder(v: View) : RecyclerView.ViewHolder(v), View.OnCreateContextMenuListener {
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
            v.findViewById<CardView>(R.id.card_view).setOnClickListener {
                mFavoriteListener.onClickFavorite(mFavStops[adapterPosition])
            }
            v.findViewById<CardView>(R.id.card_view).setOnCreateContextMenuListener(this)
        }

        override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?) {
            // menu.setHeaderTitle("Header Title")
            menu.add(Menu.NONE, ACTION_EDIT, adapterPosition, R.string.context_menu_edit)
            menu.add(Menu.NONE, ACTION_REMOVE, adapterPosition, R.string.context_menu_remove)
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
        holder.mStopIdTextView.text = FavoriteStopIdsViewModel.FavoriteStop.stopId(mFavStops[position])?.toString()
        holder.mStopDescTextView.text = FavoriteStopIdsViewModel.FavoriteStop.stopDesc(mFavStops[position])
    }

    override fun getItemCount(): Int = mFavStops.size

    /* FIXME: position is not a stable ID, when moving positions then returning
       from another activity this causes spurious moves */
    override fun getItemId(position: Int): Long = FavoriteStopIdsViewModel.FavoriteStop.position(mFavStops[position]).toLong()

    private inner class FavoriteStopIdsItemTouchHelperCallback : ItemTouchHelper.Callback() {
        override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int =
        	// makeMovementFlags(UP or DOWN, START or END)
        	makeMovementFlags(UP or DOWN, 0)

        override fun isLongPressDragEnabled() = false

        override fun isItemViewSwipeEnabled() = false

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
            val removedStop = mFavStops.removeAt(position)
            notifyItemRemoved(position)
            // note: positions in adapter are reversed from positions in database
            mFavoriteListener.onPromptDeleteFavorite(removedStop, mFavStops.size - position, position)
        }
    }

    fun onCancelDeleteFavorite(removedStop: FavoriteStopIdsViewModel.FavoriteStop, position: Int) {
        mFavStops.add(position, removedStop)
        notifyItemInserted(position)
    }

    companion object {
        val ACTION_EDIT = 0
        val ACTION_REMOVE = 1
    }
}
