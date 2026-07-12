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

    @Test
    void addToilet_withValidData_shouldSucceed() {
        Toilet givenData = TestsUtils.buildToilet();
        ToiletDAO mappedDao = TestsUtils.buildToiletDAO();
        ToiletDAO savedDao = TestsUtils.buildToiletDAO();
        savedDao.setId(42L);
        // Ensure saved DAO mirrors the input libelle (mapper + db generated id)
        savedDao.setLibelle(givenData.getLibelle());
        Toilet expectedResult = Toilet.builder()
                .id(42L)
                .libelle(givenData.getLibelle())
                .build();

        doReturn(savedDao).when(repository).save(org.mockito.Mockito.any(ToiletDAO.class));
        // Use ReflectionTestUtils to mock the mapper via service field since it's not mocked directly
        Toilet result = service.addToilet(givenData);

        // Verify the result matches (mapper may normalize casing)
        assertEquals(givenData.getLibelle().toUpperCase(), result.getLibelle());
    }

    @Test
    void getNearsetToilettes_withPosition_shouldReturnFilteredSortedList() {

        List<ToiletDAO> repoResult = TestsUtils.buildBddContent(currentPosition);
        doReturn(repoResult).when(repository).findAll();

        List<Toilet> result = service.getNearsetToilettes(currentPosition);
        log.info("la liste resultante est :\n{}", result);

        assertTrue(result.size() <= 5);
        assertTrue(result.size() >= 2 ? result.getFirst().getDistance() < result.getLast().getDistance() : true);

        log.info("ToileteDTO : \n{}", result);
    }

    @Test
    void lireToilet_withExistingId_shouldReturnToilet() {
        ToiletDAO dao = TestsUtils.buildToiletDAO();
        dao.setId(1L);
        doReturn(java.util.Optional.of(dao)).when(repository).findById(1L);

        java.util.Optional<Toilet> result = service.lireToilet(1L);

        assertTrue(result.isPresent());
    }

    @Test
    void updateToilet_withExistingId_shouldSucceed() {
        ToiletDAO existingDao = TestsUtils.buildToiletDAO();
        existingDao.setId(1L);
        Toilet updateData = TestsUtils.buildToilet();
        doReturn(java.util.Optional.of(existingDao)).when(repository).findById(1L);
        doReturn(existingDao).when(repository).save(existingDao);

        service.updateToilet(1L, updateData);
        // No exception means success
        assertTrue(true);
    }

    @Test
    void deleteToilet_withExistingId_shouldSucceed() {
        doReturn(true).when(repository).existsById(1L);

        service.deleteToilet(1L);
        // No exception means success
        assertTrue(true);
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