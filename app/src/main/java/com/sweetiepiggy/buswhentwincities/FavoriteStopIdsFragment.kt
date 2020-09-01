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

package com.sweetiepiggy.buswhentwincities.ui.favoritestopids

import android.content.Context
import android.os.AsyncTask
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.play.core.review.ReviewManagerFactory
import com.sweetiepiggy.buswhentwincities.*

class FavoriteStopIdsFragment : Fragment(), FavoriteStopIdsAdapter.OnClickFavoriteListener {
    private lateinit var mClickFavoriteListener: FavoriteStopIdsAdapter.OnClickFavoriteListener
    private lateinit var mAdapter: FavoriteStopIdsAdapter
    private lateinit var mModel: FavoriteStopIdsViewModel
    private var mModelIsInit = false
    private var mDoUpdateFavoriteStops = true
    private val mFavoriteStops: MutableList<FavoriteStopIdsViewModel.FavoriteStop> = ArrayList<FavoriteStopIdsViewModel.FavoriteStop>()

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

        mAdapter = FavoriteStopIdsAdapter(mClickFavoriteListener, mFavoriteStops)
        getActivity()?.findViewById<RecyclerView>(R.id.favoritesRecyclerView)?.apply {
            layoutManager = LinearLayoutManager(context)
            // addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            mAdapter.attachToRecyclerView(this)
            adapter = mAdapter
        }

        mModel = activity?.run {
            ViewModelProvider(this).get(FavoriteStopIdsViewModel::class.java)
        } ?: throw Exception("Invalid Activity")
        mModelIsInit = true

        mModel.getFavoriteStops().observe(this,
        	Observer<List<FavoriteStopIdsViewModel.FavoriteStop>>{ updateFavoriteStops(it) })
    }

    fun refresh() {
        if (mModelIsInit) {
            mDoUpdateFavoriteStops = true
            mModel.loadFavoriteStops()
        }
    }

    override fun onClickFavorite(favStop: FavoriteStopIdsViewModel.FavoriteStop) {
        mClickFavoriteListener.onClickFavorite(favStop)
    }

    override fun onMoveFavorite(fromPosition: Int, toPosition: Int) {
        mClickFavoriteListener.onMoveFavorite(fromPosition, toPosition)
        mModel.setFavoriteStops(mFavoriteStops)
    }

    override fun onPromptDeleteFavorite(removedStop: FavoriteStopIdsViewModel.FavoriteStop, position: Int, recyclerViewPosition: Int) {
        mClickFavoriteListener.onPromptDeleteFavorite(removedStop, position, recyclerViewPosition)
    }

    fun onDeleteFavorite() {
        mModel.setFavoriteStops(mFavoriteStops)
    }

    fun onCancelDeleteFavorite(removedStop: FavoriteStopIdsViewModel.FavoriteStop, recyclerViewPosition: Int) {
        mAdapter.onCancelDeleteFavorite(removedStop, recyclerViewPosition)
    }

    override fun onContextItemSelected(item: MenuItem): Boolean =
        when (item.itemId) {
            FavoriteStopIdsAdapter.ACTION_EDIT -> {
                val position = item.order
                val favStop = mFavoriteStops[position]
                AlertDialog.Builder(context!!).apply {
                    val favStopIdDialog = layoutInflater.inflate(R.layout.dialog_fav_stop_id, null)
                    setTitle(R.string.edit_stop_name_dialog_title)
                    setView(favStopIdDialog)
                    favStopIdDialog.findViewById<TextInputEditText>(R.id.stop_name)?.setText(FavoriteStopIdsViewModel.FavoriteStop.stopDesc(favStop))
                    setPositiveButton(android.R.string.ok) { _, _ ->
                        val stopDesc = favStopIdDialog.findViewById<TextInputEditText>(R.id.stop_name)?.text.toString()
                        val newFavStop = FavoriteStopIdsViewModel.FavoriteStop.updateDesc(favStop, stopDesc)
                        mFavoriteStops[position] = newFavStop
                        mAdapter.notifyDataSetChanged()

                        object : AsyncTask<Void, Void, Void>() {
                            override fun doInBackground(vararg params: Void): Void? {
                                DbAdapter().apply {
                                    openReadWrite(context)
                                    when (newFavStop) {
                                        is FavoriteStopIdsViewModel.FavoriteStop.FavoriteStopId ->
                                            updateFavStop(newFavStop.stopId, stopDesc)
                                        is FavoriteStopIdsViewModel.FavoriteStop.FavoriteTimestop -> {
                                            val timestop = newFavStop.timestop
                                            updateFavTimestop(timestop.timestopId, stopDesc)}
                                    }
                                    close()
                                }
                                return null
                            }
                            override fun onPostExecute(result: Void?) {}
                        }.execute()
                    }
                    setNegativeButton(android.R.string.cancel) { _, _ -> }
                }.show()
                true
            }

            FavoriteStopIdsAdapter.ACTION_REMOVE -> {
                val position = item.order
                val removedStop = mFavoriteStops.removeAt(position)
                mAdapter.notifyItemRemoved(position)
                onPromptDeleteFavorite(removedStop, mFavoriteStops.size - position, position)
                true
            }
            else -> super.onContextItemSelected(item)
        }

    private fun updateFavoriteStops(favoriteStops: List<FavoriteStopIdsViewModel.FavoriteStop>) {
        val doRequestReview = mDoUpdateFavoriteStops
            && favoriteStops.size > 2 && favoriteStops.size > mFavoriteStops.size

        if (mDoUpdateFavoriteStops) {
            activity?.findViewById<View>(R.id.progressBar)?.setVisibility(View.INVISIBLE)
            mFavoriteStops.apply {
                clear()
                addAll(favoriteStops)
            }
            mAdapter.notifyDataSetChanged()
            mDoUpdateFavoriteStops = false
        }
        updateFavoriteStopIdsMessage()

        val ctxt = context
        val actvty = activity
        if (doRequestReview && ctxt != null && actvty != null) {
            // https://developer.android.com/guide/playcore/in-app-review/kotlin-java
            ReviewManagerFactory.create(ctxt).apply {
                requestReviewFlow().addOnCompleteListener { request ->
                    if (request.isSuccessful) {
                        launchReviewFlow(actvty, request.result)
                            .addOnCompleteListener { _ -> }
                    }
                }
            }
        }
    }

    fun updateFavoriteStopIdsMessage() {
        val resultsRecyclerView = activity?.findViewById<View>(R.id.favoritesRecyclerView)
        val noResultsView = activity?.findViewById<View>(R.id.no_results_textview)
        if (mFavoriteStops.isEmpty()) {
            resultsRecyclerView?.setVisibility(View.INVISIBLE)
            noResultsView?.setVisibility(View.VISIBLE)
        } else {
            noResultsView?.setVisibility(View.INVISIBLE)
            resultsRecyclerView?.setVisibility(View.VISIBLE)
        }
    }
}
