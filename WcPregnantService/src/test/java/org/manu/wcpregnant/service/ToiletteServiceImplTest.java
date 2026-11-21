package org.manu.wcpregnant.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.manu.wcpregnant.bdd.dao.ToiletDAO;
import org.manu.wcpregnant.bdd.repository.ToiletteRepository;
import org.manu.wcpregnant.service.utils.ResourceNotFoundException;
import org.manu.wcpregnant.utils.TestsUtils;
import org.manu.wcpregnant.web.dto.GeoPosition;
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

    /**
     * Repository JPA réellement injecté dans le service.
     *
     * <p>Le bean {@code ToiletteServiceImpl} est partagé par le contexte Spring mis en
     * cache entre les classes de test {@code @SpringBootTest}. Comme chaque test remplace
     * le repository par un mock, le repository authentique doit être restauré après
     * chaque test, sinon les classes exécutées ensuite interrogent un mock
     * ({@code findAll(Pageable)} renvoie alors {@code null}).</p>
     */
    @Autowired
    ToiletteRepository realRepository;

    @Mock
    ToiletteRepository repository;

    private GeoPosition currentPosition = GeoPosition.builder()
            .latitude(48.607348)
            .longitude(-1.8898742)
            .build();

    @BeforeEach
    public void setup() {
        repository = mock(ToiletteRepository.class);
        ReflectionTestUtils.setField(service, "toiletteRepository", repository);
    }

    /**
     * Restaure le repository JPA authentique sur le service partagé.
     *
     * <p>Sans cette étape, la pollution du bean {@code ToiletteServiceImpl} par le mock
     * se propagerait aux autres classes d'intégration partageant le même contexte Spring
     * (par exemple {@code DefaultProfileSecurityTest}), ce qui rendrait les tests
     * dépendants de l'ordre d'exécution.</p>
     */
    @AfterEach
    public void restoreRealRepository() {
        ReflectionTestUtils.setField(service, "toiletteRepository", realRepository);
    }

    @Test
    void addToilet_withValidData_shouldSucceed() {
        Toilet givenData = TestsUtils.buildToilet();
        ToiletDAO savedDao = TestsUtils.buildToiletDAO();
        savedDao.setId(42L);
        savedDao.setLibelle(givenData.getLibelle());

        doReturn(savedDao).when(repository).save(org.mockito.Mockito.any(ToiletDAO.class));

        Toilet result = service.addToilet(givenData);

        assertTrue(result.getId() > 0);
    }

    @Test
    void getNearestToilets_withPosition_shouldReturnFilteredSortedList() {
        List<ToiletDAO> repoResult = TestsUtils.buildBddContent(currentPosition);
        doReturn(repoResult).when(repository).findAll();

        List<Toilet> result = service.getNearestToilets(currentPosition);
        log.info("la liste resultante est :\n{}", result);

        assertTrue(result.size() <= 5);
        assertTrue(result.size() >= 2
                ? result.getFirst().getDistance() < result.getLast().getDistance()
                : true);
    }

    @Test
    void getToiletById_withExistingId_shouldReturnToilet() {
        ToiletDAO dao = TestsUtils.buildToiletDAO();
        dao.setId(1L);
        doReturn(java.util.Optional.of(dao)).when(repository).findById(1L);

        java.util.Optional<Toilet> result = service.getToiletById(1L);

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
    void updateToilet_withNonExistingId_shouldThrowResourceNotFoundException() {
        doReturn(java.util.Optional.empty()).when(repository).findById(999L);

        assertThrows(ResourceNotFoundException.class, () ->
                service.updateToilet(999L, TestsUtils.buildToilet()));
    }

    @Test
    void deleteToilet_withExistingId_shouldSucceed() {
        doReturn(true).when(repository).existsById(1L);

        service.deleteToilet(1L);
        // No exception means success
        assertTrue(true);
    }

    @Test
    void deleteToilet_withNonExistingId_shouldThrowResourceNotFoundException() {
        doReturn(false).when(repository).existsById(999L);

        assertThrows(ResourceNotFoundException.class, () -> service.deleteToilet(999L));
    }
}