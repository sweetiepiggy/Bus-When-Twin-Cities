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
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.support.v7.app.AlertDialog;
import android.util.JsonReader;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class DownloadNexTripsTask extends AsyncTask<Void, Integer, Void> {
    private static final String NEXTRIPS_URL = "svc.metrotransit.org/NexTrip/";
    private static boolean mUseHttps = true;
    private Context mContext;
    private OnDownloadedListener mDownloadedListener;
    private String mAlertMessage = null;
    private String mStopId = null;
    private List<NexTrip> mNexTrips = null;

    public interface OnDownloadedListener {
        public void onDownloaded(List<NexTrip> nexTrips);
    }

    public class UnauthorizedException extends IOException {
        public UnauthorizedException() {
            super();
        }
    }

    public DownloadNexTripsTask(Context context, OnDownloadedListener downloadedListener,
                                String stopId) {
        mContext = context;
        mDownloadedListener = downloadedListener;
        mStopId = stopId;
    }

    @Override
    protected Void doInBackground(Void... params) {
        boolean retry;
        String firstAlertMessage = null;

        do {
            retry = false;
            try {
                mNexTrips = downloadNexTrips(mStopId);
            } catch (UnknownHostException e) { // probably no internet connection
                mAlertMessage = mContext.getResources().getString(R.string.unknown_host);
            } catch (java.io.FileNotFoundException e) {
                mAlertMessage =
                    mContext.getResources().getString(R.string.file_not_found) + ":\n" + e.getMessage();
            } catch (java.net.SocketTimeoutException e) {
                mAlertMessage =
                    mContext.getResources().getString(R.string.timed_out) + ":\n" + e.getMessage();
            } catch (UnauthorizedException e) {
                mAlertMessage = mContext.getResources().getString(R.string.unauthorized);
            } catch (SocketException e) {
                mAlertMessage = e.getMessage();
            } catch (MalformedURLException e) {
                mAlertMessage = e.getMessage();
            } catch (UnsupportedEncodingException e) {
                mAlertMessage = e.getMessage();
            } catch (IOException e) {
                if (firstAlertMessage == null) {
                    firstAlertMessage = e.getMessage();
                }
                // old Android versions seem to have a problem with https and
                // throw IOException: CertPathValidatorException,
                // try again using http
                if (mUseHttps) {
                    mUseHttps = false;
                    retry = true;
                } else {
                    mAlertMessage = e.getMessage();
                }
            }
        } while (retry);

        if (mAlertMessage != null && firstAlertMessage != null) {
            mAlertMessage = firstAlertMessage;
        }

        return null;
    }

    @Override
    protected void onPostExecute(Void result)
    {
        if (mAlertMessage != null && mContext != null) {
            alert(mAlertMessage);
        }
        if (mNexTrips != null) {
            mDownloadedListener.onDownloaded(mNexTrips);
        }
    }

    private void alert(String msg)
    {
        AlertDialog.Builder alert = new AlertDialog.Builder(mContext);
        alert.setTitle(mContext.getResources().getString(android.R.string.dialog_alert_title));
        alert.setMessage(msg);
        alert.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        alert.show();
    }

    private List<NexTrip> downloadNexTrips(String stopId)
        throws MalformedURLException, UnsupportedEncodingException, IOException {
        List<NexTrip> nexTrips = null;

        final String nexTripsUrl = (mUseHttps ? "https://" : "http://")
            + NEXTRIPS_URL + stopId + "?format=json";

        URL url = new URL(nexTripsUrl);
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

        JsonReader reader = new JsonReader(new InputStreamReader(urlConnection.getInputStream(),
                                                               "utf-8"));

        try {
            nexTrips = parseNexTrips(reader);
        } finally {
            reader.close();
        }
        // nexTrips = new ArrayList<NexTrip>();
        // nexTrips.add(new NexTrip(true, 1175, "5 Min", "/Date(1547811780000-0600)/",
        //                        "Minn Drive / France Av / Southdale", "", "6",
        //                        "SOUTHBOUND", "F", 0, 44.980820, -93.270970));
        // nexTrips.add(new NexTrip(true, 2036, "10 Min", "/Date(1547812080000-0600)/",
        //                        "Hopkins/United Health/Bren Rd W", "", "12",
        //                        "WESTBOUND", "G", 0, 44.982220, -93.268790));
        // nexTrips.add(new NexTrip(false, 1078, "7:58", "/Date(1547812200000-0600)/",
        //                        "Bryant Av/82St-35W TC/Via Lyndale", "", "4",
        //                        "SOUTHBOUND", "L", 0, 0, 0));

        return nexTrips;
    }

    private List<NexTrip> parseNexTrips(JsonReader reader)
        throws IOException {
        List<NexTrip> nexTrips = new ArrayList<NexTrip>();

        reader.beginArray();
        while (reader.hasNext()) {
            reader.beginObject();
            boolean actual = false;
            int blockNumber = -1;
            String departureText = null;
            String departureTime = null;
            String description = null;
            String gate = null;
            String route = null;
            String routeDirection = null;
            String terminal = null;
            double vehicleHeading = 0;
            double vehicleLatitude = 0;
            double vehicleLongitude = 0;
            while (reader.hasNext()) {
                String name = reader.nextName();
                if (name.equals("Actual")) {
                    actual = reader.nextBoolean();
                } else if (name.equals("BlockNumber")) {
                    blockNumber = reader.nextInt();
                } else if (name.equals("DepartureText")) {
                    departureText = reader.nextString();
                } else if (name.equals("DepartureTime")) {
                    departureTime = reader.nextString();
                } else if (name.equals("Description")) {
                    description = reader.nextString();
                } else if (name.equals("Gate")) {
                    gate = reader.nextString();
                } else if (name.equals("Route")) {
                    route = reader.nextString();
                } else if (name.equals("RouteDirection")) {
                    routeDirection = reader.nextString();
                } else if (name.equals("Terminal")) {
                    terminal = reader.nextString();
                } else if (name.equals("VehicleHeading")) {
                    vehicleHeading = reader.nextDouble();
                } else if (name.equals("VehicleLatitude")) {
                    vehicleLatitude = reader.nextDouble();
                } else if (name.equals("VehicleLongitude")) {
                    vehicleLongitude = reader.nextDouble();
                } else {
                    reader.skipValue();
                }
            }
            nexTrips.add(new NexTrip(mContext, actual, blockNumber, departureText,
                                     departureTime, description, gate, route,
                                     routeDirection, terminal, vehicleHeading,
                                     vehicleLatitude, vehicleLongitude));
            reader.endObject();
        }
        reader.endArray();

        return nexTrips;
    }
}
