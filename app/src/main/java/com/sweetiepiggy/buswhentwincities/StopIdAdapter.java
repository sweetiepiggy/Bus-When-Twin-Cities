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
 import android.net.Uri;
 import android.os.Bundle;
 import android.support.v7.widget.RecyclerView;
 import android.view.LayoutInflater;
 import android.view.View;
 import android.view.ViewGroup;
 import android.widget.ImageButton;
 import android.widget.TextView;

 import java.util.List;

public class StopIdAdapter extends RecyclerView.Adapter<StopIdAdapter.StopIdViewHolder> {
    private List<NexTrip> mNexTrips;
    private Context mCtxt;

    public class StopIdViewHolder extends RecyclerView.ViewHolder {
        public TextView mRouteTextView;
        public TextView mDirectionTextView;
        public TextView mDescriptionTextView;
        public TextView mDepartureTextTextView;
        public TextView mScheduledTextView;
        public ImageButton mMapButton;

        public StopIdViewHolder(View v) {
            super(v);
            mRouteTextView = (TextView) v.findViewById(R.id.route);
            mDirectionTextView = (TextView) v.findViewById(R.id.direction);
            mDescriptionTextView = (TextView) v.findViewById(R.id.description);
            mDepartureTextTextView = (TextView) v.findViewById(R.id.departure_text);
            mScheduledTextView = (TextView) v.findViewById(R.id.scheduled);
            mMapButton = (ImageButton) v.findViewById(R.id.map_button);
            mMapButton.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        NexTrip nexTrip = mNexTrips.get(getAdapterPosition());
                        String latitudeStr = Double.toString(nexTrip.getVehicleLatitude());
                        String longitudeStr = Double.toString(nexTrip.getVehicleLongitude());
                        Uri uri = Uri.parse("geo:" + latitudeStr + "," + longitudeStr + "?q="
                                            + Uri.encode(latitudeStr + "," + longitudeStr + "("
                                                         + mCtxt.getResources().getString(R.string.bus)
                                                         + " " + nexTrip.getRoute()
                                                         + nexTrip.getTerminal() + ")"));
                        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                        intent.setPackage("com.google.android.apps.maps");
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        mCtxt.startActivity(intent);
                    }
                });
        }
    }

    public StopIdAdapter(Context ctxt, List<NexTrip> nexTrips) {
        mCtxt = ctxt;
        mNexTrips = nexTrips;
    }

    @Override
    public StopIdAdapter.StopIdViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.stop_id_item, parent, false);
        StopIdViewHolder vh = new StopIdViewHolder(v);
        return vh;
    }

    @Override
    public void onBindViewHolder(StopIdViewHolder holder, int position) {
        NexTrip nexTrip = mNexTrips.get(position);
        holder.mRouteTextView.setText(nexTrip.getRoute() + nexTrip.getTerminal());
        holder.mDirectionTextView.setText(translateDirection(nexTrip.getRouteDirection()));
        holder.mDescriptionTextView.setText(nexTrip.getDescription());
        holder.mDepartureTextTextView.setText(translateDeparture(nexTrip.getDepartureText()));
        holder.mMapButton.setVisibility(nexTrip.isActual() ? View.VISIBLE : View.GONE);
        // holder.mMapButton.setVisibility(View.GONE);
        holder.mScheduledTextView.setText(mCtxt.getResources().getString(nexTrip.isActual()
                                                                         ? R.string.real_time
                                                                         : R.string.scheduled));
    }

    @Override
    public int getItemCount() {
        return mNexTrips.size();
    }

    String translateDeparture(String departure) {
        if (departure.endsWith(" Min")) {
            return departure.substring(0, departure.length() - 3)
                + mCtxt.getResources().getString(R.string.minutes);
        } else {
            return departure;
        }
    }

    String translateDirection(String dir) {
        if (dir.equals("SOUTHBOUND")) {
            return mCtxt.getResources().getString(R.string.south);
        } else if (dir.equals("EASTBOUND")) {
            return mCtxt.getResources().getString(R.string.east);
        } else if (dir.equals("WESTBOUND")) {
            return mCtxt.getResources().getString(R.string.west);
        } else if (dir.equals("NORTHBOUND")) {
            return mCtxt.getResources().getString(R.string.north);
        } else {
            return dir;
        }
    }
}
