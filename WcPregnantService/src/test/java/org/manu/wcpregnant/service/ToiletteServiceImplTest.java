package org.manu.wcpregnant.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.logging.log4j.util.Strings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.manu.wcpregnant.bdd.dao.ToiletDAO;
import org.manu.wcpregnant.bdd.repository.ToiletteRepository;
import org.manu.wcpregnant.utils.TestsUtils;
import org.manu.wcpregnant.web.dto.Adress;
import org.manu.wcpregnant.web.dto.GeoPosition;
import org.manu.wcpregnant.web.dto.Graduation;
import org.manu.wcpregnant.web.dto.Toilet;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;

@SpringBootTest
class ToiletteServiceImplTest {
    private final Logger log = LoggerFactory.getLogger(ToiletteServiceImplTest.class);

    @Autowired
    ToiletteService service;

    @Mock
    ToiletteRepository repository;

    private GeoPosition currentPosition = GeoPosition.builder().latitude(48.607348).longitude(-1.8898742).build();

    @BeforeEach
    public void setup() {

        repository = mock(ToiletteRepository.class);
        ReflectionTestUtils.setField(service, "toiletteRepository", repository);

    }

    @ParameterizedTest
    @ValueSource(strings = { "", "  " })
    void isBlank_ShouldReturnTrueForNullOrBlankStrings(String input) {
        assertTrue(Strings.isBlank(input));
    }

    @ParameterizedTest
    @CsvSource(value = { "test:test", "tEst:test", "Java:java" }, delimiter = ':')
    void toLowerCase_ShouldGenerateTheExpectedLowercaseValue(String input, String expected) {
        String actualValue = input.toLowerCase();
        assertEquals(expected, actualValue);
    }

    @ParameterizedTest
    @ValueSource(strings = { "", "", "", "", "" })
    void addToilet() {
        Toilet givenData = TestsUtils.buildToilet();
        ToiletDAO repoResult = TestsUtils.buildToiletDAO();
        doReturn(repoResult).when(repository).save(repoResult);

        Toilet result = service.addToilet(givenData);

    }

    @Test
    void getNearsetToilettes() {

        List<ToiletDAO> repoResult = TestsUtils.buildBddContent(currentPosition);
        doReturn(repoResult).when(repository).findAll();

        List<Toilet> result = service.getNearsetToilettes(currentPosition);
        log.info("la liste resultante est :\n{}", result);

        assertTrue(result.size() <= 5);
        assertTrue(result.size() >= 2 ? result.getFirst().getDistance() < result.getLast().getDistance() : true);

        log.info("ToileteDTO : \n{}", result);
    }

    @Test
    void testGetNearsetToilettes() {
    }

    @Test
    void lireToilet() {
    }

    @Test
    void updateToilette() {
    }

    @Test
    void updateToilet() {
    }

    @Test
    void deleteToilette() {
    }


    private Toilet buildBddToilet(String name) {
        return Toilet.builder().libelle(name).adress(buildAddress()).grade(buildGrade()).position(buildPosition())
                .build();
    }

    private Adress buildAddress() {
        return Adress.builder().numero("19 bis").codePostal("35350").ville("La Gouesnioere").voie("rue de la baie")
                .build();
    }

    private Graduation buildGrade() {
        return Graduation.builder().accueil(2).espace(2).proprete(2).build();
    }

    private GeoPosition buildPosition() {
        double offset = ThreadLocalRandom.current().nextDouble() / 1000;
        return GeoPosition.builder().longitude(-1.98 + offset).latitude(48.1 + offset).build();
    }
}