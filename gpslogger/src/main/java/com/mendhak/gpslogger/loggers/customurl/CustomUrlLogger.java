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

package com.mendhak.gpslogger.loggers.customurl;

import android.location.Location;
import android.util.Pair;

import com.mendhak.gpslogger.common.AppSettings;
import com.mendhak.gpslogger.common.PreferenceHelper;
import com.mendhak.gpslogger.common.SerializableLocation;
import com.mendhak.gpslogger.common.Session;
import com.mendhak.gpslogger.common.Strings;
import com.mendhak.gpslogger.common.Systems;
import com.mendhak.gpslogger.common.network.Networks;
import com.mendhak.gpslogger.common.slf4j.Logs;

import org.slf4j.Logger;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.KeyStore;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CustomUrlLogger implements Observer<Pair<Location, Integer>> {

    protected static PreferenceHelper preferenceHelper;

    private static final Logger LOG = Logs.of(CustomUrlLogger.class);

    private X509TrustManager trustManager;

    public CustomUrlLogger() {

        if (preferenceHelper == null) {
            preferenceHelper = PreferenceHelper.getInstance();
        }

        try {
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init((KeyStore) null);
            TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
            if (trustManagers.length != 1 || !(trustManagers[0] instanceof X509TrustManager)) {
                throw new IllegalStateException("Unexpected default trust managers:" + Arrays.toString(trustManagers));
            }
            trustManager = (X509TrustManager) trustManagers[0];
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }


    public void annotate(Location loc, Integer batteryLevel) {

        final String customLoggingUrl = preferenceHelper.getCustomLoggingUrl();
        final String httpMethod = preferenceHelper.getCustomLoggingHTTPMethod();
        final String httpBody = preferenceHelper.getCustomLoggingHTTPBody();
        final String httpHeaders = preferenceHelper.getCustomLoggingHTTPHeaders();
        final String basicAuthUsername = preferenceHelper.getCustomLoggingBasicAuthUsername();
        final String basicAuthPassword = preferenceHelper.getCustomLoggingBasicAuthPassword();

        LOG.info("HTTP " + httpMethod + " - " + customLoggingUrl);

        String description = "";

        String finalUrl = getFormattedTextblock(customLoggingUrl, loc, description, Systems.getAndroidId(), batteryLevel, Strings.getBuildSerial(),
                Session.getInstance().getStartTimeStamp(), Session.getInstance().getCurrentFormattedFileName(), PreferenceHelper.getInstance().getCurrentProfileName(), Session.getInstance().getTotalTravelled());
        String finalBody = getFormattedTextblock(httpBody, loc, description, Systems.getAndroidId(), batteryLevel, Strings.getBuildSerial(),
                Session.getInstance().getStartTimeStamp(), Session.getInstance().getCurrentFormattedFileName(), PreferenceHelper.getInstance().getCurrentProfileName(), Session.getInstance().getTotalTravelled());
        String finalHeaders = getFormattedTextblock(httpHeaders, loc, description, Systems.getAndroidId(), batteryLevel, Strings.getBuildSerial(),
                Session.getInstance().getStartTimeStamp(), Session.getInstance().getCurrentFormattedFileName(), PreferenceHelper.getInstance().getCurrentProfileName(), Session.getInstance().getTotalTravelled());

        CustomUrlRequest urlRequest = new CustomUrlRequest(finalUrl, httpMethod, finalBody, finalHeaders, basicAuthUsername, basicAuthPassword);

        OkHttpClient.Builder okBuilder = new OkHttpClient.Builder();
        okBuilder.sslSocketFactory(Networks.getSocketFactory(AppSettings.getInstance()), trustManager);
        Request.Builder requestBuilder = new Request.Builder().url(urlRequest.getLogURL());

        for (Map.Entry<String, String> header : urlRequest.getHttpHeaders().entrySet()) {
            requestBuilder.addHeader(header.getKey(), header.getValue());
        }

        if (!urlRequest.getHttpMethod().equalsIgnoreCase("GET")) {
            RequestBody body = RequestBody.create(null, urlRequest.getHttpBody());
            requestBuilder = requestBuilder.method(urlRequest.getHttpMethod(), body);
        }

        Response response = null;
        try {
            Request request = requestBuilder.build();
            response = okBuilder.build().newCall(request).execute();

            if (response.isSuccessful()) {
                LOG.debug("HTTP request complete with successful response code " + response);
            } else {
                throw new RuntimeException("HTTP request failed with unexpected response code " + response);
            }

        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            if (response != null) {
                response.body().close();
            }
        }

    }


    public static String getFormattedTextblock(String customLoggingUrl, Location loc, String description, String androidId,
                                               float batteryLevel, String buildSerial, long sessionStartTimeStamp, String fileName, String profileName, double distance) {

        String logUrl = customLoggingUrl;
        SerializableLocation sLoc = new SerializableLocation(loc);
        try {
            logUrl = logUrl.replaceAll("(?i)%lat", String.valueOf(sLoc.getLatitude()));
            logUrl = logUrl.replaceAll("(?i)%lon", String.valueOf(sLoc.getLongitude()));
            logUrl = logUrl.replaceAll("(?i)%sat", String.valueOf(sLoc.getSatelliteCount()));
            logUrl = logUrl.replaceAll("(?i)%desc", Strings.htmlDecode(description));
            logUrl = logUrl.replaceAll("(?i)%alt", String.valueOf(sLoc.getAltitude()));
            logUrl = logUrl.replaceAll("(?i)%acc", String.valueOf(sLoc.getAccuracy()));
            logUrl = logUrl.replaceAll("(?i)%dir", String.valueOf(sLoc.getBearing()));
            logUrl = logUrl.replaceAll("(?i)%prov", sLoc.getProvider());
            logUrl = logUrl.replaceAll("(?i)%spd", String.valueOf(sLoc.getSpeed()));
            logUrl = logUrl.replaceAll("(?i)%timestamp", String.valueOf(sLoc.getTime() / 1000));
            logUrl = logUrl.replaceAll("(?i)%time", Strings.getIsoDateTime(new Date(sLoc.getTime())));
            logUrl = logUrl.replaceAll("(?i)%date", Strings.getIsoCalendarDate(new Date(sLoc.getTime())));
            logUrl = logUrl.replaceAll("(?i)%starttimestamp", String.valueOf(sessionStartTimeStamp / 1000));
            logUrl = logUrl.replaceAll("(?i)%batt", String.valueOf(batteryLevel));
            logUrl = logUrl.replaceAll("(?i)%aid", androidId);
            logUrl = logUrl.replaceAll("(?i)%ser", buildSerial);
            logUrl = logUrl.replaceAll("(?i)%act", sLoc.getDetectedActivity());
            logUrl = logUrl.replaceAll("(?i)%filename", fileName);
            logUrl = logUrl.replaceAll("(?i)%profile", URLEncoder.encode(profileName, "UTF-8"));
            logUrl = logUrl.replaceAll("(?i)%hdop", sLoc.getHDOP());
            logUrl = logUrl.replaceAll("(?i)%vdop", sLoc.getVDOP());
            logUrl = logUrl.replaceAll("(?i)%pdop", sLoc.getPDOP());
            logUrl = logUrl.replaceAll("(?i)%dist", String.valueOf((int) distance));
        } catch (UnsupportedEncodingException e) {
            LOG.warn(e.getMessage(), e);
        }
        return logUrl;
    }

    @Override
    public void onSubscribe(Disposable d) {
        LOG.info("CustomUrlLogger.onSubscribe");
    }

    @Override
    public void onNext(Pair<Location, Integer> locationIntegerPair) {
        try {
            annotate(locationIntegerPair.first, locationIntegerPair.second);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }

    @Override
    public void onError(Throwable e) {
        LOG.error("CustomUrlLogger.onError: " + e.getMessage(), e);
    }

    @Override
    public void onComplete() {
        LOG.info("CustomUrlLogger.onComplete");
    }
}


