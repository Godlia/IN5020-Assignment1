package com.group2.server;
//sqlite class
public record City(
        long geonameId,
        String name,
        String countryCode,
        String countryName,
        long population,
        String timezone,
        double latitude,
        double longitude) {
}