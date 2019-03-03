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

 import android.content.Context;
 import android.content.Intent;
 import android.os.Bundle;
 import androidx.recyclerview.widget.RecyclerView;
 import android.view.LayoutInflater;
 import android.view.View;
 import android.view.ViewGroup;
 import android.widget.TextView;

 import java.util.List;

 public class FavoriteStopIdsAdapter extends RecyclerView.Adapter<FavoriteStopIdsAdapter.FavoriteStopIdsViewHolder> {
    private List<String> mFavStopIds;
    private Context mContext;

    public class FavoriteStopIdsViewHolder extends RecyclerView.ViewHolder {
        public TextView mStopIdTextView;

        public FavoriteStopIdsViewHolder(View v) {
            super(v);
            mStopIdTextView = (TextView) v.findViewById(R.id.stop_id);
            v.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        String stopId = mFavStopIds.get(getAdapterPosition());
                        Intent intent = new Intent(mContext, StopIdActivity.class);
                        Bundle b = new Bundle();
                        b.putString("stopId", stopId);
                        intent.putExtras(b);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        mContext.startActivity(intent);
                    }
                });
        }
    }

    public FavoriteStopIdsAdapter(Context context, List<String> favStopIds) {
        mContext = context;
        mFavStopIds = favStopIds;
    }

    @Override
    public FavoriteStopIdsAdapter.FavoriteStopIdsViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.fav_stop_id_item, parent, false);
        return new FavoriteStopIdsViewHolder(v);
    }

    @Override
    public void onBindViewHolder(FavoriteStopIdsViewHolder holder, int position) {
        holder.mStopIdTextView.setText(mFavStopIds.get(position));
    }

    @Override
    public int getItemCount() {
        return mFavStopIds.size();
    }

}
