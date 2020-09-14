/*
 * Copyright (C) 2016 mendhak
 *
 * This file is part of GPSLogger for Android.
 *
 * GPSLogger for Android is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * GPSLogger for Android is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with GPSLogger for Android.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.mendhak.gpslogger.loggers.opengts;

import android.location.Location;
import android.util.Pair;

import com.mendhak.gpslogger.common.PreferenceHelper;
import com.mendhak.gpslogger.common.SerializableLocation;
import com.mendhak.gpslogger.senders.opengts.OpenGTSManager;

import io.reactivex.observers.DisposableObserver;

/**
 * Send locations directly to an OpenGTS server <br/>
 *
 * @author Francisco Reynoso
 */
public class OpenGTSLogger extends DisposableObserver<Pair<Location, Integer>> {

    protected final String name = "OpenGTS";
    private static PreferenceHelper preferenceHelper = PreferenceHelper.getInstance();

    public OpenGTSLogger() {
    }

    public void write(Integer batteryLevel, Location loc) {
        OpenGTSManager manager = new OpenGTSManager(preferenceHelper, batteryLevel);
        manager.sendLocations(new SerializableLocation[]{new SerializableLocation(loc)});
    }

    @Override
    public void onNext(Pair<Location, Integer> locationIntegerPair) {
        write(locationIntegerPair.second, locationIntegerPair.first);
    }

    @Override
    public void onError(Throwable e) {

    }

    @Override
    public void onComplete() {

    }
}

