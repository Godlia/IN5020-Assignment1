package com.group2.server;

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