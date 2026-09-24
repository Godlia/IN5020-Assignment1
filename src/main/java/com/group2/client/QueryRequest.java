package com.group2.client;

import java.util.Arrays;

record QueryRequest(String originalQuery, String methodName, Object[] arguments, int zone) {
    static QueryRequest parse(String line) {
        String[] tokens = line.split("\\s+");
        int zoneIndex = -1;
        for (int index = 0; index < tokens.length; index++) {
            if (tokens[index].startsWith("Zone:")) {
                zoneIndex = index;
                break;
            }
        }

        if (zoneIndex < 0) {
            throw new IllegalArgumentException("Missing Zone value in query: " + line);
        }

        String methodName = tokens[0];
        String[] argumentTokens = Arrays.copyOfRange(tokens, 1, zoneIndex);
        Object[] arguments = buildArguments(methodName, argumentTokens);
        int zone = Integer.parseInt(tokens[zoneIndex].substring("Zone:".length()));
        return new QueryRequest(line, methodName, arguments, zone);
    }

    private static Object[] buildArguments(String methodName, String[] tokens) {
        return switch (methodName) {
            case "getPopulationofCountry" -> new Object[] { String.join(" ", tokens) };
            case "getNumberofCities" -> new Object[] {
                    String.join(" ", Arrays.copyOfRange(tokens, 0, tokens.length - 2)),
                    Integer.parseInt(tokens[tokens.length - 2]),
                    tokens[tokens.length - 1]
            };
            case "getNumberofCountries" -> new Object[] {
                    Integer.parseInt(tokens[0]),
                    Integer.parseInt(tokens[1]),
                    tokens[2]
            };
            case "getNumberofCountriesMM" -> new Object[] {
                    Integer.parseInt(tokens[0]),
                    Integer.parseInt(tokens[1]),
                    Integer.parseInt(tokens[2])
            };
            default -> throw new IllegalArgumentException("Unknown method: " + methodName);
        };
    }
}
