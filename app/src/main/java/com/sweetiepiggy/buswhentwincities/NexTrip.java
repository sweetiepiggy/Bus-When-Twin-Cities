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

public class NexTrip {
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

    public NexTrip(boolean actual, int blockNumber, String departureText,
                   String departureTime, String description, String gate,
                   String route, String routeDirection, String terminal,
                   double vehicleHeading, double vehicleLatitude,
                   double vehicleLongitude) {
        mActual = actual;
        mBlockNumber = blockNumber;
        mDepartureText = departureText;
        mDepartureTime = departureTime;
        mDescription = description;
        mGate = gate;
        mRoute = route;
        mRouteDirection = routeDirection;
        mTerminal = terminal;
        mVehicleHeading = vehicleHeading;
        mVehicleLatitude = vehicleLatitude;
        mVehicleLongitude = vehicleLongitude;
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
}
