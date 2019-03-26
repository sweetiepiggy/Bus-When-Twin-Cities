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

package com.sweetiepiggy.buswhentwincities.ui.favoritestopids

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sweetiepiggy.buswhentwincities.FavoriteStopIdsAdapter
import com.sweetiepiggy.buswhentwincities.FavoriteStopIdsViewModel
import com.sweetiepiggy.buswhentwincities.R

class FavoriteStopIdsFragment : Fragment() {
    private lateinit var mClickFavoriteListener: FavoriteStopIdsAdapter.OnClickFavoriteListener
    private lateinit var mAdapter: FavoriteStopIdsAdapter
    private lateinit var mModel: FavoriteStopIdsViewModel
    private val mFavoriteStopIds: MutableList<FavoriteStopIdsViewModel.FavoriteStopId> = ArrayList<FavoriteStopIdsViewModel.FavoriteStopId>()

    companion object {
        fun newInstance() = FavoriteStopIdsFragment()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mClickFavoriteListener = context as FavoriteStopIdsAdapter.OnClickFavoriteListener
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.favorite_stop_ids_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        mAdapter = FavoriteStopIdsAdapter(mClickFavoriteListener, mFavoriteStopIds)
        getActivity()?.findViewById<RecyclerView>(R.id.favoritesRecyclerView)?.apply {
            layoutManager = LinearLayoutManager(context)
            // addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            mAdapter.attachToRecyclerView(this)
            adapter = mAdapter
        }

        mModel = activity?.run {
            ViewModelProviders.of(this).get(FavoriteStopIdsViewModel::class.java)
        } ?: throw Exception("Invalid Activity")

        mModel.getFavoriteStopIds().observe(this,
        	Observer<List<FavoriteStopIdsViewModel.FavoriteStopId>>{ updateFavoriteStopIds(it) })
    }

    fun refresh() {
        mModel.loadFavoriteStopIds()
    }

    private fun updateFavoriteStopIds(favoriteStopIds: List<FavoriteStopIdsViewModel.FavoriteStopId>) {
        mFavoriteStopIds.clear()
        mFavoriteStopIds.addAll(favoriteStopIds)
        mAdapter.notifyDataSetChanged()

        val resultsRecyclerView = activity?.findViewById<View>(R.id.favoritesRecyclerView)
        val noResultsView = activity?.findViewById<View>(R.id.no_results_textview)
        if (favoriteStopIds.isEmpty()) {
            resultsRecyclerView?.setVisibility(View.GONE)
            noResultsView?.setVisibility(View.VISIBLE)
        } else {
            noResultsView?.setVisibility(View.GONE)
            resultsRecyclerView?.setVisibility(View.VISIBLE)
        }
    }
}
