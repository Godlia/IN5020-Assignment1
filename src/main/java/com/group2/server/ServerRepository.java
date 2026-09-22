package com.group2.server;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ServerRepository {

    private static final String DEFAULT_DATABASE_URL = "jdbc:sqlite:cities.db";

    private final String databaseUrl;

    public ServerRepository() throws SQLException {
        this(DEFAULT_DATABASE_URL);
        System.out.println("Using default database");
    }

    public ServerRepository(String databaseUrl) throws SQLException {
        this.databaseUrl = databaseUrl;
        createTableIfNeeded();
    }

    public void importCities(Path csvFile) throws Exception {
        String insertSql = """
                INSERT OR REPLACE INTO cities
                (geoname_id, name, country_code, country_name, population, timezone, latitude, longitude)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(insertSql)) {
            connection.setAutoCommit(false);
            try (var lines = Files.lines(csvFile)) {
                boolean header = true;
                long rowNumber = 1;
                for (String line : (Iterable<String>) lines::iterator) {
                    if (header) {
                        header = false;
                        continue;
                    }
                    if (line.isBlank()) {
                        continue;
                    }
                    bindCity(statement, parseCity(line, rowNumber));
                    statement.addBatch();
                    rowNumber++;
                }
                statement.executeBatch();
            }
            connection.commit();
        }
    }

    public List<City> getEveryCityInCountry(String countryName) throws SQLException {
        return getCities("country_name = ?", countryName);
    }

    public List<City> getEveryCityInCountryCode(String countryCode) throws SQLException {
        return getCities("country_code = ?", countryCode);
    }

    public City getCityById(long geonameId) throws SQLException {
        List<City> cities = getCities("geoname_id = ?", geonameId);
        return cities.isEmpty() ? null : cities.get(0);
    }

    public long getPopulationOfCountry(String countryName) throws SQLException {
        String sql = "SELECT COALESCE(SUM(population), 0) FROM cities WHERE country_name = ?";
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, countryName);
            try (ResultSet results = statement.executeQuery()) {
                return results.getLong(1);
            }
        }
    }

    public long getNumberOfCities(String countryName) throws SQLException {
        String sql = "SELECT COUNT(*) FROM cities WHERE country_name = ?";
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, countryName);
            try (ResultSet results = statement.executeQuery()) {
                return results.getLong(1);
            }
        }
    }

    private List<City> getCities(String condition, Object value) throws SQLException {
        String sql = "SELECT geoname_id, name, country_code, country_name, population, timezone, "
                + "latitude, longitude FROM cities WHERE " + condition + " ORDER BY name";
        List<City> cities = new ArrayList<>();
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(sql)) {
            if (value instanceof Long id) {
                statement.setLong(1, id);
            } else {
                statement.setString(1, (String) value);
            }
            try (ResultSet results = statement.executeQuery()) {
                while (results.next()) {
                    cities.add(toCity(results));
                }
            }
        }
        return cities;
    }


    public int getNumberOfCitiesFiltered(String countryCode, int threshold, String comp) throws SQLException {
        String comparison = populationComparison(comp);

        String sql = "SELECT COUNT(*) FROM cities "
                + "WHERE (country_code = ? OR country_name = ?) AND population " + comparison + " ?";
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, countryCode);
            statement.setString(2, countryCode);
            statement.setInt(3, threshold);
            try (ResultSet results = statement.executeQuery()) {
                return Math.toIntExact(results.getLong(1));
            }
        }
    }

    public int getNumberofCountries(int citycount, int threshold, String comp) throws SQLException {
        String comparison = populationComparison(comp);
        String sql = "SELECT COUNT(*) FROM ("
                + "SELECT country_code FROM cities WHERE population " + comparison + " ? "
                + "GROUP BY country_code HAVING COUNT(*) >= ?"
                + ")";
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, threshold);
            statement.setInt(2, citycount);
            try (ResultSet results = statement.executeQuery()) {
                return Math.toIntExact(results.getLong(1));
            }
        }
    }

    public int getNumberofCountriesMM(int citycount, int minpopulation, int maxpopulation)
            throws SQLException {
        String sql = "SELECT COUNT(*) FROM ("
                + "SELECT country_code FROM cities "
                + "WHERE population BETWEEN ? AND ? "
                + "GROUP BY country_code HAVING COUNT(*) >= ?"
                + ")";
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, minpopulation);
            statement.setInt(2, maxpopulation);
            statement.setInt(3, citycount);
            try (ResultSet results = statement.executeQuery()) {
                return Math.toIntExact(results.getLong(1));
            }
        }
    }

    private static String populationComparison(String comp) {
        return switch (comp.toLowerCase()) {
            case "min", ">=" -> ">=";
            case "max", "<=" -> "<=";
            default -> throw new IllegalArgumentException("Comparison must be min or max");
        };
    }

    private void createTableIfNeeded() throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS cities (
                    geoname_id INTEGER PRIMARY KEY,
                    name TEXT NOT NULL,
                    country_code TEXT NOT NULL,
                    country_name TEXT NOT NULL,
                    population INTEGER NOT NULL,
                    timezone TEXT NOT NULL,
                    latitude REAL NOT NULL,
                    longitude REAL NOT NULL
                )
                """;
        try (Connection connection = connect(); Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(databaseUrl);
    }

    private static City parseCity(String line, long rowNumber) {
        String[] fields = line.split(";", -1);
        if (fields.length != 7) {
            throw new IllegalArgumentException("Expected 7 fields at CSV row " + rowNumber);
        }
        String[] coordinates = fields[6].split(",", -1);
        if (coordinates.length != 2) {
            throw new IllegalArgumentException("Expected latitude and longitude at CSV row " + rowNumber);
        }
        try {
            return new City(
                    Long.parseLong(fields[0]), fields[1], fields[2], fields[3],
                    Long.parseLong(fields[4]), fields[5], Double.parseDouble(coordinates[0]),
                    Double.parseDouble(coordinates[1]));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid number at CSV row " + rowNumber, exception);
        }
    }

    private static void bindCity(PreparedStatement statement, City city) throws SQLException {
        statement.setLong(1, city.geonameId());
        statement.setString(2, city.name());
        statement.setString(3, city.countryCode());
        statement.setString(4, city.countryName());
        statement.setLong(5, city.population());
        statement.setString(6, city.timezone());
        statement.setDouble(7, city.latitude());
        statement.setDouble(8, city.longitude());
    }

    private static City toCity(ResultSet results) throws SQLException {
        return new City(
                results.getLong("geoname_id"), results.getString("name"),
                results.getString("country_code"), results.getString("country_name"),
                results.getLong("population"), results.getString("timezone"),
                results.getDouble("latitude"), results.getDouble("longitude"));
    }
}
