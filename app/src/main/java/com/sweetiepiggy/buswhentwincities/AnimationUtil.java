/* Copyright 2013 Google Inc.
   Licensed under Apache 2.0: http://www.apache.org/licenses/LICENSE-2.0.html
   Source - https://gist.github.com/broady/6314689
   Video - https://www.youtube.com/watch?v=WKfZsCKSXVQ&feature=youtu.be
   */
/*
    Copyright (C) 2019 Sweetie Piggy Apps <sweetiepiggyapps@gmail.com>

    Modified by Sweetie Piggy Apps to work with OsmDroid

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

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.Marker;

import android.animation.ObjectAnimator;
import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Property;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;

/**
 * Animation utilities for moving markers from one location to another with Maps API.
 *
 */
public class AnimationUtil {

    /**
     * Animates a marker from it's current position to the provided finalPosition
     *
     * @param marker        marker to animate
     * @param finalPosition the final position of the marker after the animation
     */
    public static void animateMarkerTo(final Marker marker, final GeoPoint finalPosition) {
        // Use the appropriate implementation per API Level
       // if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            // animateMarkerToICS(marker, finalPosition);
        /* } else */ if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
           animateMarkerToHC(marker, finalPosition);
       } else {
           animateMarkerToGB(marker, finalPosition);
       }
    }

    private static void animateMarkerToGB(final Marker marker, final GeoPoint finalPosition) {
        final LatLngInterpolator latLngInterpolator = new LatLngInterpolator.Linear();
        final GeoPoint startPosition = marker.getPosition();
        final Handler handler = new Handler();
        final long start = SystemClock.uptimeMillis();
        final Interpolator interpolator = new AccelerateDecelerateInterpolator();
        final float durationInMs = 3000;

        handler.post(new Runnable() {
            long elapsed;

            float t;

            float v;

            @Override
            public void run() {
                // Calculate progress using interpolator
                elapsed = SystemClock.uptimeMillis() - start;
                t = elapsed / durationInMs;
                v = interpolator.getInterpolation(t);

                marker.setPosition(latLngInterpolator.interpolate(v, startPosition, finalPosition));

                // Repeat till progress is complete.
                if (t < 1) {
                    // Post again 16ms later.
                    handler.postDelayed(this, 16);
                }
            }
        });
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    private static void animateMarkerToHC(final Marker marker, final GeoPoint finalPosition) {
        final LatLngInterpolator latLngInterpolator = new LatLngInterpolator.Linear();
        final GeoPoint startPosition = marker.getPosition();

        ValueAnimator valueAnimator = new ValueAnimator();
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float v = animation.getAnimatedFraction();
                GeoPoint newPosition = latLngInterpolator
                        .interpolate(v, startPosition, finalPosition);
                marker.setPosition(newPosition);
            }
        });
        valueAnimator.setFloatValues(0, 1); // Ignored.
        valueAnimator.setDuration(3000);
        valueAnimator.start();
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private static void animateMarkerToICS(Marker marker, GeoPoint finalPosition) {
        final LatLngInterpolator latLngInterpolator = new LatLngInterpolator.Linear();
        TypeEvaluator<GeoPoint> typeEvaluator = new TypeEvaluator<GeoPoint>() {
            @Override
            public GeoPoint evaluate(float fraction, GeoPoint startValue, GeoPoint endValue) {
                return latLngInterpolator.interpolate(fraction, startValue, endValue);
            }
        };
        Property<Marker, GeoPoint> property = Property.of(Marker.class, GeoPoint.class, "position");
        ObjectAnimator animator = ObjectAnimator
                .ofObject(marker, property, typeEvaluator, finalPosition);
        animator.setDuration(3000);
        animator.start();
    }

    /**
     * For other LatLngInterpolator interpolators, see https://gist.github.com/broady/6314689
     */
    interface LatLngInterpolator {

        GeoPoint interpolate(float fraction, GeoPoint a, GeoPoint b);

        class Linear implements LatLngInterpolator {

            @Override
            public GeoPoint interpolate(float fraction, GeoPoint a, GeoPoint b) {
                double lat = (b.getLatitude() - a.getLatitude()) * fraction + a.getLatitude();
                double lngDelta = b.getLongitude() - a.getLongitude();

                // Take the shortest path across the 180th meridian.
                if (Math.abs(lngDelta) > 180) {
                    lngDelta -= Math.signum(lngDelta) * 360;
                }
                double lng = lngDelta * fraction + a.getLongitude();
                return new GeoPoint(lat, lng);
            }
        }
    }
}
