package com.mendhak.gpslogger.loggers.customurl;

import android.location.Location;

import androidx.test.filters.SmallTest;

import com.mendhak.gpslogger.common.BundleConstants;
import com.mendhak.gpslogger.common.PreferenceHelper;
import com.mendhak.gpslogger.loggers.MockLocations;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.UnsupportedEncodingException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;


@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class CustomUrlLoggerTest {

    @Test
    public void getFormattedUrl_WhenPlaceholders_ValuesSubstituted() {
        CustomUrlLogger.preferenceHelper = mock(PreferenceHelper.class);
        Location loc = MockLocations.builder("MOCK", 12.193, 19.111)
                .putExtra("satellites", 9)
                .withAltitude(45)
                .withAccuracy(8)
                .withBearing(359)
                .withSpeed(9001)
                .withTime(1457205869949l)
                .putExtra(BundleConstants.DETECTED_ACTIVITY, "TILTED")
                .build();

        String expected = "http://192.168.1.65:8000/test?lat=12.193&lon=19.111&sat=9&desc=blah&alt=45.0&acc=8.0&dir=359.0&prov=MOCK&spd=9001.0&time=2016-03-05T19:24:29.949Z&battery=91.0&androidId=22&serial=SRS11&activity=TILTED";
        String urlTemplate = "http://192.168.1.65:8000/test?lat=%LAT&lon=%LON&sat=%SAT&desc=%DESC&alt=%ALT&acc=%ACC&dir=%DIR&prov=%PROV&spd=%SPD&time=%TIME&battery=%BATT&androidId=%AID&serial=%SER&activity=%act";
        assertThat("Placeholders are substituted", CustomUrlLogger.getFormattedTextblock(urlTemplate, loc, "blah", "22", 91, "SRS11", 0, "", "", 27), is(expected));
    }


    @Test
    public void getFormattedUrl_WhenDistanceAvailable_FormattedWithoutDecimal() {

        Location loc = MockLocations.builder("MOCK", 12.193, 19.111)
                .build();

        String expected = "http://192.168.1.65:8000/test?lat=12.193&lon=19.111&dist=27";
        String urlTemplate = "http://192.168.1.65:8000/test?lat=%LAT&lon=%LON&dist=%DIST";
        assertThat("Distance formatted without decimal", CustomUrlLogger.getFormattedTextblock(urlTemplate, loc, "blah", "22", 91, "SRS11", 0, "", "", 27), is(expected));

    }


    @Test
    public void getFormattedUrl_WhenValuesMissing_UrlReturnsWhatsAvailable() {


        Location loc = MockLocations.builder("MOCK", 12.193, 19.111)
                .withTime(1457205869949l)
                .build();

        String expected = "http://192.168.1.65:8000/test?lat=12.193&lon=19.111&sat=0&desc=&alt=0.0&acc=0.0&dir=0.0&prov=MOCK&spd=0.0&time=2016-03-05T19:24:29.949Z&battery=0.0&androidId=&serial=&activity=";
        String urlTemplate = "http://192.168.1.65:8000/test?lat=%LAT&lon=%LON&sat=%SAT&desc=%DESC&alt=%ALT&acc=%ACC&dir=%DIR&prov=%PROV&spd=%SPD&time=%TIME&battery=%BATT&androidId=%AID&serial=%SER&activity=%ACT";
        assertThat("Placeholders are substituted", CustomUrlLogger.getFormattedTextblock(urlTemplate, loc, "", "", 0, "", 0, "", "", 0), is(expected));
    }

    @Test
    public void getFormattedUrl_WhenTimeStamp_UseUnixEpoch() {
        Location loc = MockLocations.builder("MOCK", 12.193, 19.456).withTime(1457205869949l).build();
        String expected = "http://192.168.1.65:8000/test?lat=12.193&lon=19.456&sat=0&desc=&alt=0.0&acc=0.0&dir=0.0&prov=MOCK&spd=0.0&time=2016-03-05T19:24:29.949Z&battery=0.0&androidId=&serial=&activity=&epoch=1457205869";
        String urlTemplate = "http://192.168.1.65:8000/test?lat=%LAT&lon=%LON&sat=%SAT&desc=%DESC&alt=%ALT&acc=%ACC&dir=%DIR&prov=%PROV&spd=%SPD&time=%TIME&battery=%BATT&androidId=%AID&serial=%SER&activity=%ACT&epoch=%TIMESTAMP";
        assertThat("Unix timestamp is in seconds", CustomUrlLogger.getFormattedTextblock(urlTemplate, loc, "", "", 0, "", 0, "", "", 0), is(expected));

        expected = "http://192.168.1.65:8000/test?lat=12.193&lon=19.456&sat=0&desc=&alt=0.0&acc=0.0&dir=0.0&prov=MOCK&spd=0.0&time=2016-03-05T19:24:29.949Z&battery=0.0&androidId=&serial=&activity=&epoch=1457205869000";
        urlTemplate = "http://192.168.1.65:8000/test?lat=%LAT&lon=%LON&sat=%SAT&desc=%DESC&alt=%ALT&acc=%ACC&dir=%DIR&prov=%PROV&spd=%SPD&time=%TIME&battery=%BATT&androidId=%AID&serial=%SER&activity=%ACT&epoch=%TIMESTAMP000";

        assertThat("Unix timestamp with 000 to fake milliseconds", CustomUrlLogger.getFormattedTextblock(urlTemplate, loc, "", "", 0, "", 0, "", "", 0), is(expected));

    }


    @Test
    public void getFormattedUrl_WhenDateParameter_UseISODateFormat() {
        Location loc = MockLocations.builder("MOCK", 12.193, 19.456).withTime(1457205869949l).build();
        String expected = "http://192.168.1.65:8000/test?lat=12.193&lon=19.456&date=2016-03-05";
        String urlTemplate = "http://192.168.1.65:8000/test?lat=%LAT&lon=%LON&date=%DATE";

        assertThat("DATE parameter is substituted with ISO Date Format", CustomUrlLogger.getFormattedTextblock(urlTemplate, loc, "", "", 0, "", 0, "", "", 0), is(expected));


    }

    @Test
    public void getFormattedUrl_WhenStartTime_AddSessionStartTime() {
        Location loc = MockLocations.builder("MOCK", 12.193, 19.456).withTime(1457205869949l).build();
        String expected = "http://192.168.1.65:8000/test?lat=12.193&lon=19.456&stst=1495884681";
        String urlTemplate = "http://192.168.1.65:8000/test?lat=%LAT&lon=%LON&stst=%STARTTIMESTAMP";
        assertThat("Start timestamp is in seconds", CustomUrlLogger.getFormattedTextblock(urlTemplate, loc, "", "", 0, "", 1495884681283l, "", "", 0), is(expected));

        expected = "http://192.168.1.65:8000/test?lat=12.193&lon=19.456&stst=0";
        urlTemplate = "http://192.168.1.65:8000/test?lat=%LAT&lon=%LON&stst=%STARTTIMESTAMP";
        assertThat("Absence of start timestamp recorded as 0", CustomUrlLogger.getFormattedTextblock(urlTemplate, loc, "", "", 0, "", 0l, "", "", 0), is(expected));
    }

    @Test
    public void getFormattedUrl_WhenFilename_AddFilename() {
        Location loc = MockLocations.builder("MOCK", 12.193, 19.456).withTime(1457205869949l).build();
        String expected = "http://192.168.1.65:8000/test?lat=12.193&lon=19.456&fn=20170527abc";
        String urlTemplate = "http://192.168.1.65:8000/test?lat=%LAT&lon=%LON&fn=%FILENAME";
        assertThat("Start timestamp is in seconds", CustomUrlLogger.getFormattedTextblock(urlTemplate, loc, "", "", 0, "", 1495884681283l, "20170527abc", "", 0), is(expected));

    }

    @Test
    public void getFormattedUrl_WhenProfilename_AddProfileName() throws UnsupportedEncodingException {
        Location loc = MockLocations.builder("MOCK", 12.193, 19.456).withTime(1457205869949l).build();
        String expected = "http://192.168.1.65:8000/test?lat=12.193&lon=19.456&fn=20170527abc&profile=Default+Profile";
        String urlTemplate = "http://192.168.1.65:8000/test?lat=%LAT&lon=%LON&fn=%FILENAME&profile=%PROFILE";

        String url = CustomUrlLogger.getFormattedTextblock(urlTemplate, loc, "", "", 0, "", 1495884681283l, "20170527abc", "Default Profile", 0);
        assertThat("Profile name is provided", url, is(expected));

    }

    @Test
    public void getFormattedUrl_WhenHDOPAvailable_AddDopValues() {
        Location loc = MockLocations.builder("MOCK", 12.193, 19.456).putExtra(BundleConstants.HDOP, "4").withTime(1457205869949l).build();
        String expected = "http://192.168.1.65:8000/test?lat=12.193&lon=19.456&horizontal=4";
        String urlTemplate = "http://192.168.1.65:8000/test?lat=%LAT&lon=%LON&horizontal=%HDOP";
        assertThat("HDOP value is provided", CustomUrlLogger.getFormattedTextblock(urlTemplate, loc, "", "", 0, "", 1495884681283l, "20170527abc", "Default Profile", 0), is(expected));

    }

    @Test
    public void getFormattedUrl_WhenVDOPAvailable_AddDopValues() {
        Location loc = MockLocations.builder("MOCK", 12.193, 19.456).putExtra(BundleConstants.VDOP, "19").withTime(1457205869949l).build();
        String expected = "http://192.168.1.65:8000/test?lat=12.193&lon=19.456&horizontal=&vertical=19";
        String urlTemplate = "http://192.168.1.65:8000/test?lat=%LAT&lon=%LON&horizontal=%HDOP&vertical=%VDop";
        assertThat("VDOP value is provided", CustomUrlLogger.getFormattedTextblock(urlTemplate, loc, "", "", 0, "", 1495884681283l, "20170527abc", "Default Profile", 0), is(expected));
    }

    @Test
    public void getFormattedUrl_WhenPDOPAvailable_AddDopValues() {
        Location loc = MockLocations.builder("MOCK", 12.193, 19.456).putExtra(BundleConstants.PDOP, "2").withTime(1457205869949l).build();
        String expected = "http://192.168.1.65:8000/test?lat=12.193&lon=19.456&horizontal=&positional=2";
        String urlTemplate = "http://192.168.1.65:8000/test?lat=%LAT&lon=%LON&horizontal=%HDOP&positional=%pdop";
        assertThat("PDOP value is provided", CustomUrlLogger.getFormattedTextblock(urlTemplate, loc, "", "", 0, "", 1495884681283l, "20170527abc", "Default Profile", 0), is(expected));
    }


    @Test
    public void getFormattedUrl_WhenPostBody_ValuesSubstituted() {

        Location loc = MockLocations.builder("MOCK", 12.193, 19.111)
                .putExtra("satellites", 9)
                .withAltitude(45)
                .withAccuracy(8)
                .withBearing(359)
                .withSpeed(9001)
                .withTime(1457205869949l)
                .putExtra(BundleConstants.DETECTED_ACTIVITY, "TILTED")
                .build();


        String expected = "This my post body\nlat=12.193&lon=19.111&sat=9&desc=blah&alt=45.0&acc=8.0&dir=359.0&prov=MOCK&spd=9001.0&time=2016-03-05T19:24:29.949Z&battery=91.0&androidId=22&serial=SRS11&activity=TILTED&dist=27";
        String urlTemplate = "This my post body\nlat=%LAT&lon=%LON&sat=%SAT&desc=%DESC&alt=%ALT&acc=%ACC&dir=%DIR&prov=%PROV&spd=%SPD&time=%TIME&battery=%BATT&androidId=%AID&serial=%SER&activity=%act&dist=%DIST";
        assertThat("Post body parameters are substituted", CustomUrlLogger.getFormattedTextblock(urlTemplate, loc, "blah", "22", 91, "SRS11", 0, "", "", 27.5), is(expected));
    }


}
