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

package com.mendhak.gpslogger.loggers.csv;

import android.location.Location;
import android.util.Pair;

import com.mendhak.gpslogger.common.BundleConstants;
import com.mendhak.gpslogger.common.Maths;
import com.mendhak.gpslogger.common.Session;
import com.mendhak.gpslogger.common.Strings;
import com.mendhak.gpslogger.loggers.Files;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Locale;

import io.reactivex.observers.DisposableObserver;


public class CSVFileLogger extends DisposableObserver<Pair<Location, Integer>> {

    private File file;
    protected final String name = "TXT";

    public CSVFileLogger(File file) {
        this.file = file;
    }

    @Override
    public void onNext(Pair<Location, Integer> locationIntegerPair) {
        if (!Session.getInstance().hasDescription()) {
            annotate("", locationIntegerPair.second, locationIntegerPair.first);
        }
    }

    @Override
    public void onError(Throwable e) {

    }

    @Override
    public void onComplete() {

    }

    String getCsvLine(Location loc, Integer batteryLevel, String dateTimeString) {
        return getCsvLine("", batteryLevel, loc, dateTimeString);
    }

    String getCsvLine(String description, Integer batteryLevel, Location loc, String dateTimeString) {

        if (description.length() > 0) {
            description = "\"" + description.replaceAll("\"", "\"\"") + "\"";
        }

        String outputString = String.format(Locale.US, "%s,%s,%s,%s,%s,%s,%s,%d,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s\n", dateTimeString,
                loc.getLatitude(),
                loc.getLongitude(),
                loc.hasAltitude() ? loc.getAltitude() : "",
                loc.hasAccuracy() ? loc.getAccuracy() : "",
                loc.hasBearing() ? loc.getBearing() : "",
                loc.hasSpeed() ? loc.getSpeed() : "",
                Maths.getBundledSatelliteCount(loc),
                loc.getProvider(),
                (loc.getExtras() != null && !Strings.isNullOrEmpty(loc.getExtras().getString(BundleConstants.HDOP))) ? loc.getExtras().getString(BundleConstants.HDOP) : "",
                (loc.getExtras() != null && !Strings.isNullOrEmpty(loc.getExtras().getString(BundleConstants.VDOP))) ? loc.getExtras().getString(BundleConstants.VDOP) : "",
                (loc.getExtras() != null && !Strings.isNullOrEmpty(loc.getExtras().getString(BundleConstants.PDOP))) ? loc.getExtras().getString(BundleConstants.PDOP) : "",
                (loc.getExtras() != null && !Strings.isNullOrEmpty(loc.getExtras().getString(BundleConstants.GEOIDHEIGHT))) ? loc.getExtras().getString(BundleConstants.GEOIDHEIGHT) : "",
                (loc.getExtras() != null && !Strings.isNullOrEmpty(loc.getExtras().getString(BundleConstants.AGEOFDGPSDATA))) ? loc.getExtras().getString(BundleConstants.AGEOFDGPSDATA) : "",
                (loc.getExtras() != null && !Strings.isNullOrEmpty(loc.getExtras().getString(BundleConstants.DGPSID))) ? loc.getExtras().getString(BundleConstants.DGPSID) : "",
                (loc.getExtras() != null && !Strings.isNullOrEmpty(loc.getExtras().getString(BundleConstants.DETECTED_ACTIVITY))) ? loc.getExtras().getString(BundleConstants.DETECTED_ACTIVITY) : "",
                (batteryLevel != null) ? batteryLevel : "",
                description
        );
        return outputString;
    }

    public void annotate(String description, Integer batteryLevel, Location loc) {

        try {
            if (!Files.reallyExists(file)) {
                file.createNewFile();

                FileOutputStream writer = new FileOutputStream(file, true);
                BufferedOutputStream output = new BufferedOutputStream(writer);
                String header = "time,lat,lon,elevation,accuracy,bearing,speed,satellites,provider,hdop,vdop,pdop,geoidheight,ageofdgpsdata,dgpsid,activity,battery,annotation\n";
                output.write(header.getBytes());
                output.flush();
                output.close();

            }

            FileOutputStream writer = new FileOutputStream(file, true);
            BufferedOutputStream output = new BufferedOutputStream(writer);

            String dateTimeString = Strings.getIsoDateTime(new Date(loc.getTime()));
            String csvLine = getCsvLine(description, batteryLevel, loc, dateTimeString);


            output.write(csvLine.getBytes());
            output.flush();
            output.close();
            Files.addToMediaDatabase(file, "text/csv");
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

}
