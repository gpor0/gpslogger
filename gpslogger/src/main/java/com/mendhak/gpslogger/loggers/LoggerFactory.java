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

package com.mendhak.gpslogger.loggers;

import android.content.Context;
import android.location.Location;
import android.util.Pair;

import com.mendhak.gpslogger.common.PreferenceHelper;
import com.mendhak.gpslogger.common.Session;
import com.mendhak.gpslogger.common.Strings;
import com.mendhak.gpslogger.common.Systems;
import com.mendhak.gpslogger.common.slf4j.Logs;
import com.mendhak.gpslogger.loggers.csv.CSVFileLogger;
import com.mendhak.gpslogger.loggers.customurl.CustomUrlLogger;
import com.mendhak.gpslogger.loggers.geojson.GeoJSONLogger;
import com.mendhak.gpslogger.loggers.gpx.Gpx10FileLogger;
import com.mendhak.gpslogger.loggers.gpx.Gpx11FileLogger;
import com.mendhak.gpslogger.loggers.kml.Kml22FileLogger;
import com.mendhak.gpslogger.loggers.opengts.OpenGTSLogger;

import org.slf4j.Logger;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;

public class LoggerFactory {
    private static final Logger LOG = Logs.of(LoggerFactory.class);

    private static PreferenceHelper preferenceHelper = PreferenceHelper.getInstance();
    private static Session session = Session.getInstance();

    private static PublishSubject<Pair<Location, Integer>> source;
    private static Set<String> subscribers = new HashSet<>();


    public static void reinitLoggers() {

        if (source == null) {
            if (Strings.isNullOrEmpty(preferenceHelper.getGpsLoggerFolder())) {
                return;
            }

            File gpxFolder = new File(preferenceHelper.getGpsLoggerFolder());
            if (!gpxFolder.exists()) {
                gpxFolder.mkdirs();
            }

            source = PublishSubject.create();

            if (preferenceHelper.shouldLogToCustomUrl()) {

                CustomUrlLogger customUrlLogger = new CustomUrlLogger();
                //we would take last from backpressure in order to not spam upstream
                source.observeOn(Schedulers.io()).sample(10, TimeUnit.SECONDS).subscribe(customUrlLogger);
                subscribers.add(customUrlLogger.getClass().getSimpleName());
            }

            if (preferenceHelper.shouldLogToGpx()) {
                File gpxFile = new File(gpxFolder.getPath(), Strings.getFormattedFileName() + ".gpx");
                if (preferenceHelper.shouldLogAsGpx11()) {
                    Gpx11FileLogger gpx11FileLogger = new Gpx11FileLogger(gpxFile, session.shouldAddNewTrackSegment());
                    source.observeOn(Schedulers.io()).subscribe(gpx11FileLogger);
                    subscribers.add(gpx11FileLogger.getClass().getSimpleName());
                } else {
                    Gpx10FileLogger gpx10FileLogger = new Gpx10FileLogger(gpxFile, session.shouldAddNewTrackSegment());
                    source.observeOn(Schedulers.io()).subscribe(gpx10FileLogger);
                    subscribers.add(gpx10FileLogger.getClass().getSimpleName());
                }
            }

            if (preferenceHelper.shouldLogToKml()) {
                File kmlFile = new File(gpxFolder.getPath(), Strings.getFormattedFileName() + ".kml");
                Kml22FileLogger kml22FileLogger = new Kml22FileLogger(kmlFile, session.shouldAddNewTrackSegment());
                source.observeOn(Schedulers.io()).subscribe(kml22FileLogger);
                subscribers.add(kml22FileLogger.getClass().getSimpleName());
            }

            if (preferenceHelper.shouldLogToCSV()) {
                File file = new File(gpxFolder.getPath(), Strings.getFormattedFileName() + ".csv");
                CSVFileLogger csvFileLogger = new CSVFileLogger(file);
                source.observeOn(Schedulers.io()).subscribe(csvFileLogger);
                subscribers.add(csvFileLogger.getClass().getSimpleName());
            }

            if (preferenceHelper.shouldLogToOpenGTS()) {
                OpenGTSLogger openGTSLogger = new OpenGTSLogger();
                source.observeOn(Schedulers.io()).subscribe(openGTSLogger);
                subscribers.add(openGTSLogger.getClass().getSimpleName());
            }

            if (preferenceHelper.shouldLogToGeoJSON()) {
                File file = new File(gpxFolder.getPath(), Strings.getFormattedFileName() + ".geojson");
                GeoJSONLogger geoJSONLogger = new GeoJSONLogger(file, session.shouldAddNewTrackSegment());
                source.observeOn(Schedulers.io()).subscribe(geoJSONLogger);
                subscribers.add(geoJSONLogger.getClass().getSimpleName());
            }

        }
    }

    public static Set<String> getLoggers() {
        return subscribers;
    }

    public static void write(Context context, Location loc) {
        emit(context, loc);
    }

    public static void emit(Context context, Location loc) {
        LOG.info("emit new location");
        Integer batteryLevel = Systems.getBatteryLevel(context);
        if(source == null) {
            reinitLoggers();
        }
        source.onNext(new Pair<>(loc, batteryLevel));
    }
}
