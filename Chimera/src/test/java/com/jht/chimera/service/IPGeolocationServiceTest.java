package com.jht.chimera.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import com.jht.chimera.service.IPGeolocationService.LocationInfo;

import org.junit.Test;

public class IPGeolocationServiceTest {
    static LocationInfo li(String cc, String ip) {
        LocationInfo loc = new LocationInfo();
        loc.countryCode = cc;
        loc.ipAddress = ip;
        return loc;
    }
    
    @Test
    public void testNonIdenticalLocationInfos() {
        LocationInfo a = li("US", "147.219.13.199");
        LocationInfo b = li("US", "147.219.13.196"); // Subtly different!
        assertNotEquals(a, b);
        // Not guaranteed for all non-equal LocationInfos, but likely enough for this case:
        assertNotEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void testParseLocationFromRealData() {
        String responseText = "{\"as\":\"AS20115 Charter Communications\",\"city\":\"Madison\",\"country\":\"United States\",\"countryCode\":\"US\",\"isp\":\"Charter Communications\",\"lat\":43.0334,\"lon\":-89.4512,\"org\":\"Spectrum\",\"query\":\"147.219.13.199\",\"region\":\"WI\",\"regionName\":\"Wisconsin\",\"status\":\"success\",\"timezone\":\"America/Chicago\",\"zip\":\"53711\"}";
        assertEqualsAndHashCodeMatches(
                li("US", "147.219.13.199"),
                IPGeolocationService.parseLocationInfo(responseText)
        );
    }

    static void assertEqualsAndHashCodeMatches(Object o1, Object o2) {
        assertEquals(o1, o2);
        assertEquals(o1.hashCode(), o2.hashCode());
    }

    @Test
    public void testParseSimplifiedLocationString() {
        assertEqualsAndHashCodeMatches(
                li("US", "147.219.13.199"),
                IPGeolocationService.parseLocationInfo("{\"countryCode\":\"US\",\"query\":\"147.219.13.199\"}")
        );
    }

    @Test
    public void testParseIpv6LocationString() {
        assertEqualsAndHashCodeMatches(
                li("US", "2001:470:1f11:11e8:2e4d:54ff:feea:71c4"),
                IPGeolocationService.parseLocationInfo("{\"countryCode\":\"US\",\"query\":\"2001:470:1f11:11e8:2e4d:54ff:feea:71c4\"}")
        );
    }

    @Test
    public void testParseNullCountryCodeLocationString() {
        assertEqualsAndHashCodeMatches(
                li(null, "147.219.13.199"),
                IPGeolocationService.parseLocationInfo("{\"countryCode\":null,\"query\":\"147.219.13.199\"}")
        );
    }

    @Test
    public void testParseNullIpAddressLocationString() {
        assertEqualsAndHashCodeMatches(
                li("US", null),
                IPGeolocationService.parseLocationInfo("{\"countryCode\":\"US\",\"query\":null}")
        );
    }

    @Test
    public void testParseEmptyLocationString() {
        assertEqualsAndHashCodeMatches(
                li(null, null),
                IPGeolocationService.parseLocationInfo("{}")
        );
    }
}
