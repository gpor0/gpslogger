package com.mendhak.gpslogger.loggers.geojson;

import android.location.Location;
import android.util.Pair;

import com.mendhak.gpslogger.common.RejectionHandler;

import java.io.File;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

/**
 * Created by clemens on 10.05.17.
 */

public class GeoJSONLogger implements Observer<Pair<Location, Integer>> {

    final static Object lock = new Object();
    private final static ThreadPoolExecutor EXECUTOR = new ThreadPoolExecutor(1, 1, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(10), new RejectionHandler());
    private final File file;
    protected final String name;
    private final boolean addNewTrackSegment;

    public GeoJSONLogger(File file, boolean addNewTrackSegment) {
        this.file = file;
        name = "GeoJSON";
        this.addNewTrackSegment = addNewTrackSegment;
    }

    @Override
    public void onSubscribe(Disposable d) {

    }

    @Override
    public void onNext(Pair<Location, Integer> locationIntegerPair) {
        annotate("", locationIntegerPair.first);
    }

    @Override
    public void onError(Throwable e) {

    }

    @Override
    public void onComplete() {

    }

    public void annotate(String description, Location loc) {
        Runnable gw = new GeoJSONWriterPoints(file, loc, description, addNewTrackSegment);
        EXECUTOR.execute(gw);
    }

    public static int getCount() {
        return EXECUTOR.getActiveCount();
    }
}

