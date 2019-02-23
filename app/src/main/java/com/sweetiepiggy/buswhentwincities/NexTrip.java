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
import android.text.format.DateFormat;

import java.util.Calendar;
import java.util.Date;

public class NexTrip {
    private Context mCtxt;
    private boolean mActual;
    private int mBlockNumber;
    private String mDepartureText;
    private String mDepartureTime;
    private String mDescription;
    private String mGate;
    private String mRoute;
    private String mRouteDirection;
    private String mTerminal;
    private double mVehicleHeading;
    private double mVehicleLatitude;
    private double mVehicleLongitude;

    public NexTrip(Context ctxt, boolean actual, int blockNumber, String departureText,
                   String departureTime, String description, String gate,
                   String route, String routeDirection, String terminal,
                   double vehicleHeading, double vehicleLatitude,
                   double vehicleLongitude) {
        mCtxt = ctxt;
        mActual = actual;
        mBlockNumber = blockNumber;
        mDescription = description;
        mGate = gate;
        mRoute = route;
        mRouteDirection = translateDirection(routeDirection);
        mTerminal = terminal;
        mVehicleHeading = vehicleHeading;
        mVehicleLatitude = vehicleLatitude;
        mVehicleLongitude = vehicleLongitude;

        long departureTimeInMillis = parseDepartureTime(departureTime);
        long millisUntilDeparture = departureTimeInMillis - Calendar.getInstance().getTimeInMillis();
        long minutesUntilDeparture = millisUntilDeparture / 1000 / 60;
        if (departureTimeInMillis < 0 || millisUntilDeparture < 0) {
            mDepartureText = translateDepartureText(departureText);
            mDepartureTime = "";
        } else if (minutesUntilDeparture < 60) {
            mDepartureText = (minutesUntilDeparture < 1
                              ? mCtxt.getResources().getString(R.string.due)
                              : Long.toString(minutesUntilDeparture)
                                + " " + mCtxt.getResources().getString(R.string.minutes));
            mDepartureTime = DateFormat.getTimeFormat(mCtxt).format(new Date(departureTimeInMillis));
        } else {
            mDepartureText = DateFormat.getTimeFormat(mCtxt).format(new Date(departureTimeInMillis));
            mDepartureTime = "";
        }
    }

    boolean isActual() {
        return mActual;
    }

    int getBlockNumber() {
        return mBlockNumber;
    }
    String getDepartureText() {
        return mDepartureText;
    }

    String getDepartureTime() {
        return mDepartureTime;
    }

    String getDescription() {
        return mDescription;
    }

    String getGate() {
        return mGate;
    }

    String getRoute() {
        return mRoute;
    }

    String getRouteDirection() {
        return mRouteDirection;
    }

    String getTerminal() {
        return mTerminal;
    }

    double getVehicleHeading() {
        return mVehicleHeading;
    }

    double getVehicleLatitude() {
        return mVehicleLatitude;
    }

    double getVehicleLongitude() {
        return mVehicleLongitude;
    }

    long parseDepartureTime(String departureTime) {
        if (departureTime.startsWith("/Date(")) {
            int timezoneIdx = departureTime.indexOf('-', 6);
            if (timezoneIdx < 0) {
                timezoneIdx = departureTime.indexOf('+', 6);
                if (timezoneIdx < 0) {
                    return -1;
                }
            }
            return Long.parseLong(departureTime.substring(6, timezoneIdx));
        }
        return -1;
    }

    String translateDepartureText(String departureText) {
        if (departureText.endsWith(" Min")) {
            return departureText.substring(0, departureText.length() - 3)
                + mCtxt.getResources().getString(R.string.minutes);
        } else {
            return departureText;
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
