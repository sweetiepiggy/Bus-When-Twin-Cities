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

package com.sweetiepiggy.buswhentwincities;

import android.os.AsyncTask;
import android.os.Bundle;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class StopIdActivity extends AppCompatActivity
                            implements DownloadNexTripsTask.OnDownloadedListener {
    private static final long MIN_SECONDS_BETWEEN_REFRESH = 30;
    private RecyclerView mResultsRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private String mStopId = null;
    private List<NexTrip> mNexTrips = null;
    private DownloadNexTripsTask mDownloadNexTripsTask = null;
    private long mLastUpdate = 0;
    private boolean mIsFavorite = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stop_id);

        if (savedInstanceState == null) {
            Bundle b = getIntent().getExtras();
            if (b != null) {
                loadState(b);
            }
        } else {
            loadState(savedInstanceState);
        }

        DbAdapter dbHelper = new DbAdapter();
        dbHelper.open(this);
        mIsFavorite = dbHelper.isFavStop(mStopId);
        dbHelper.close();

        setTitle(getResources().getString(R.string.stop) + " #" + mStopId);

        mResultsRecyclerView = (RecyclerView) findViewById(R.id.results_recycler_view);

        mLayoutManager = new LinearLayoutManager(this);
        mResultsRecyclerView.setLayoutManager(mLayoutManager);
        mResultsRecyclerView.addItemDecoration(new DividerItemDecoration(mResultsRecyclerView.getContext(),
                                                                         DividerItemDecoration.VERTICAL));

        mNexTrips = new ArrayList<NexTrip>();
        mAdapter = new StopIdAdapter(getApplicationContext(), mNexTrips);
        mResultsRecyclerView.setAdapter(mAdapter);

        mDownloadNexTripsTask = new DownloadNexTripsTask(this, this, mStopId);
        mDownloadNexTripsTask.execute();

        // FloatingActionButton fab = findViewById(R.id.fab);
        // fab.setOnClickListener(new View.OnClickListener() {
        //      @Override
        //      public void onClick(View view) {
        //          if (mDownloadNexTripsTask.getStatus() == AsyncTask.Status.FINISHED) {
        //              mDownloadNexTripsTask = new DownloadNexTripsTask(StopIdActivity.this,
        //                                                               StopIdActivity.this,
        //                                                               mStopId);
        //              mDownloadNexTripsTask.execute();
        //          }
        //      }
        //  });
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putString("stopId", mStopId);
    }

    private void loadState(Bundle b) {
        mStopId = b.getString("stopId");
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        loadState(savedInstanceState);
    }

    @Override
    public void onDestroy() {
        if (mDownloadNexTripsTask != null) {
            mDownloadNexTripsTask.cancel(true);
        }
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_stop_id, menu);
        menu.findItem(R.id.action_favorite)
            .setIcon(ContextCompat.getDrawable(this, mIsFavorite
                                               ? android.R.drawable.btn_star_big_on
                                               : android.R.drawable.btn_star_big_off));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.action_refresh:
            if (mDownloadNexTripsTask.getStatus() == AsyncTask.Status.FINISHED
                && getUnixTime() - mLastUpdate >= MIN_SECONDS_BETWEEN_REFRESH) {
                mDownloadNexTripsTask = new DownloadNexTripsTask(this, this, mStopId);
                mDownloadNexTripsTask.execute();
            }
            return true;
        case R.id.action_favorite:
            mIsFavorite = !mIsFavorite;
            DbAdapter dbHelper = new DbAdapter();
            dbHelper.openReadWrite(this);
            if (mIsFavorite) {
                dbHelper.createFavStop(mStopId, null);
            } else {
                dbHelper.deleteFavStop(mStopId);
            }
            dbHelper.close();
            item.setIcon(ContextCompat.getDrawable(this, mIsFavorite
                                                   ? android.R.drawable.btn_star_big_on
                                                   : android.R.drawable.btn_star_big_off));
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    public void onDownloaded(List<NexTrip> nexTrips) {
        mNexTrips.clear();
        mLastUpdate = getUnixTime();
        mNexTrips.addAll(nexTrips);
        mAdapter.notifyDataSetChanged();
    }

    private long getUnixTime() {
        return Calendar.getInstance().getTimeInMillis() / 1000L;
    }
}

