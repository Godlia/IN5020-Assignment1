package com.group2.server;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ServerRepositoryTest {

    @Test
    void importsAndQueriesCities(@TempDir Path temporaryDirectory) throws Exception {
        Path database = temporaryDirectory.resolve("cities.db");
        Path csv = temporaryDirectory.resolve("cities.csv");
        Files.writeString(csv, """
                Geoname ID;Name;Country Code;Country name EN;Population;Timezone;Coordinates
                1;Alpha;AA;Testland;100;Test/Zone;10.5,20.5
                2;Beta;AA;Testland;250;Test/Zone;11.5,21.5
                3;Gamma;BB;Otherland;300;Test/Zone;12.5,22.5
                4;Delta;BB;Otherland;50;Test/Zone;13.5,23.5
                5;Epsilon;CC;Thirdland;600;Test/Zone;14.5,24.5
                """);

        ServerRepository repository = new ServerRepository("jdbc:sqlite:" + database);
        repository.importCities(csv);

        List<City> cities = repository.getEveryCityInCountry("Testland");
        City alpha = repository.getCityById(1);

        assertEquals(2, cities.size());
        assertEquals("Alpha", cities.get(0).name());
        assertEquals(350, repository.getPopulationOfCountry("Testland"));
        assertEquals(2, repository.getNumberOfCities("Testland"));
        assertEquals(List.of("Alpha", "Beta"), repository.getEveryCityInCountryCode("AA")
                .stream().map(City::name).toList());
        assertEquals(1, repository.getNumberOfCitiesFiltered("AA", 200, "min"));
        assertEquals(1, repository.getNumberOfCitiesFiltered("Testland", 200, "max"));
        assertEquals(3, repository.getNumberofCountries(1, 200, "min"));
        assertEquals(1, repository.getNumberofCountries(2, 100, "min"));
        assertEquals(2, repository.getNumberofCountriesMM(1, 100, 300));
        assertEquals(2, repository.getNumberofCountriesMM(2, 0, 300));
        assertNotNull(alpha);
        assertEquals(10.5, alpha.latitude());
        assertEquals(20.5, alpha.longitude());
    }
}