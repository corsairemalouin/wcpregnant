package org.manu.wcpregnant.service;

import org.manu.wcpregnant.service.impl.ToiletteServiceImpl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.manu.wcpregnant.bdd.dao.GeoPositionDAO;
import org.manu.wcpregnant.bdd.dao.GraduationDAO;
import org.manu.wcpregnant.bdd.dao.ToiletDAO;
import org.manu.wcpregnant.bdd.repository.ToiletteRepository;
import org.manu.wcpregnant.config.ToiletteServiceConfig;
import org.manu.wcpregnant.service.strategy.DistanceCalculator;
import org.manu.wcpregnant.service.utils.ResourceNotFoundException;
import org.manu.wcpregnant.utils.TestsUtils;
import org.manu.wcpregnant.web.dto.GeoPosition;
import org.manu.wcpregnant.web.dto.Graduation;
import org.manu.wcpregnant.web.dto.Toilet;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

class ToiletteServiceImplUnitTest {

    private ToiletteServiceImpl service;

    @Mock
    private ToiletteRepository repository;

    @Mock
    private ToiletConverter mapper;

    @Mock
    private ToiletteServiceConfig config;

    @Mock
    private DistanceCalculator distanceCalculator;

    private GeoPosition currentPosition = GeoPosition.builder()
            .latitude(48.607348)
            .longitude(-1.8898742)
            .build();

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
        service = new ToiletteServiceImpl(config, repository, mapper, distanceCalculator);
    }

    // ========================================================================
    // getAllToilets - Récupère toutes les toilettes avec pagination
    // Cas : page/size valides, page vide, exception technique, paramètres null
    // ========================================================================

    @Test
    void getAllToilets_withValidPagination_shouldReturnContent() {
        ToiletDAO dao1 = new ToiletDAO();
        dao1.setId(1L);
        dao1.setLibelle("Toilet A");
        ToiletDAO dao2 = new ToiletDAO();
        dao2.setId(2L);
        dao2.setLibelle("Toilet B");
        Page<ToiletDAO> page = new PageImpl<>(List.of(dao1, dao2));

        Toilet toilet1 = Toilet.builder().id(1L).libelle("Toilet A").build();
        Toilet toilet2 = Toilet.builder().id(2L).libelle("Toilet B").build();

        doReturn(page).when(repository).findAll(PageRequest.of(0, 10));
        doReturn(toilet1).when(mapper).daoToBo(dao1);
        doReturn(toilet2).when(mapper).daoToBo(dao2);

        List<Toilet> result = service.getAllToilets(0, 10);

        assertEquals(2, result.size());
        assertEquals("Toilet A", result.get(0).getLibelle());
        assertEquals("Toilet B", result.get(1).getLibelle());
        verify(repository).findAll(PageRequest.of(0, 10));
        verify(mapper).daoToBo(dao1);
        verify(mapper).daoToBo(dao2);
    }

    @Test
    void getAllToilets_withEmptyPage_shouldReturnEmptyList() {
        doReturn(Page.empty()).when(repository).findAll(any(PageRequest.class));

        List<Toilet> result = service.getAllToilets(0, 10);

        assertTrue(result.isEmpty());
    }

    @Test
    void getAllToilets_whenRepositoryThrows_shouldPropagateException() {
        doThrow(new RuntimeException("DB error")).when(repository).findAll(any(PageRequest.class));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.getAllToilets(0, 10));
        assertEquals("DB error", ex.getMessage());
    }

    @Test
    void getAllToilets_withNullPage_shouldThrowNullPointerException() {
        assertThrows(NullPointerException.class, () -> service.getAllToilets(null, 10));
    }

    @Test
    void getAllToilets_withNullSize_shouldThrowNullPointerException() {
        assertThrows(NullPointerException.class, () -> service.getAllToilets(0, null));
    }

    // ========================================================================
    // addToilet - Ajoute une nouvelle toilette en base
    // Cas : nominal, exception repository, null input, mapper exception
    // ========================================================================

    @Test
    void addToilet_withValidData_shouldMapSaveAndReturn() {
        Toilet inputToilet = TestsUtils.buildToilet();
        ToiletDAO mappedDao = new ToiletDAO();
        mappedDao.setLibelle(inputToilet.getLibelle());
        ToiletDAO savedDao = new ToiletDAO();
        savedDao.setId(42L);
        savedDao.setLibelle(inputToilet.getLibelle());
        Toilet expectedResult = Toilet.builder()
                .id(42L)
                .libelle(inputToilet.getLibelle())
                .build();

        doReturn(mappedDao).when(mapper).boToDao(inputToilet);
        doReturn(savedDao).when(repository).save(mappedDao);
        doReturn(expectedResult).when(mapper).daoToBo(savedDao);

        Toilet result = service.addToilet(inputToilet);

        assertEquals(expectedResult, result);
        assertEquals(Long.valueOf(42L), result.getId());
        verify(mapper).boToDao(inputToilet);
        verify(repository).save(mappedDao);
        verify(mapper).daoToBo(savedDao);
    }

    @Test
    void addToilet_whenRepositoryThrows_shouldPropagate() {
        Toilet inputToilet = TestsUtils.buildToilet();
        ToiletDAO mappedDao = new ToiletDAO();
        mappedDao.setLibelle(inputToilet.getLibelle());

        doReturn(mappedDao).when(mapper).boToDao(inputToilet);
        doThrow(new RuntimeException("DB error")).when(repository).save(mappedDao);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.addToilet(inputToilet));
        assertEquals("DB error", ex.getMessage());
    }

    @Test
    void addToilet_withNullInput_shouldThrowNullPointerException() {
        assertThrows(NullPointerException.class, () -> service.addToilet(null));
    }

    @Test
    void addToilet_whenMapperThrows_shouldPropagate() {
        Toilet inputToilet = TestsUtils.buildToilet();
        doThrow(new RuntimeException("Mapping error")).when(mapper).boToDao(inputToilet);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.addToilet(inputToilet));
        assertEquals("Mapping error", ex.getMessage());
    }

    // ========================================================================
    // getNearestToilets - Recherche les toilettes les plus proches
    // Cas : résultat normal, aucun résultat, distance max = 0, position null
    // ========================================================================

    @Test
    void getNearestToilets_withResults_shouldFilterSortAndLimit() {
        List<ToiletDAO> repoResult = TestsUtils.buildBddContent(currentPosition);
        doReturn(repoResult).when(repository).findAll();

        doAnswer(invocation -> {
            ToiletDAO dao = invocation.getArgument(0);
            GeoPositionDAO p = dao.getPosition();
            GeoPosition pos = GeoPosition.builder()
                    .latitude(p.getLatitude())
                    .longitude(p.getLongitude())
                    .build();
            return Toilet.builder().libelle(dao.getLibelle()).position(pos).build();
        }).when(mapper).daoToBo(any(ToiletDAO.class));

        doReturn(10000.0).when(config).getMaxDistance();
        doReturn(5).when(config).getMaxResult();

        // Simule des distances croissantes pour vérifier le tri
        doAnswer(invocation -> {
            GeoPosition toiletPos = invocation.getArgument(0);
            GeoPosition refPos = invocation.getArgument(1);
            // Distance proportionnelle à la différence de latitude
            return Math.abs(toiletPos.getLatitude() - refPos.getLatitude()) * 1000;
        }).when(distanceCalculator).calculate(any(GeoPosition.class), any(GeoPosition.class));

        List<Toilet> result = service.getNearestToilets(currentPosition);

        assertTrue(result.size() <= 5);
        if (result.size() >= 2) {
            assertTrue(result.get(0).getDistance() <= result.get(result.size() - 1).getDistance());
        }
        // Vérifie que le distanceCalculator a bien été appelé pour chaque toilette (6 DAOs)
        verify(distanceCalculator, times(6)).calculate(any(GeoPosition.class), any(GeoPosition.class));
    }

    @Test
    void getNearestToilets_whenNoToilets_shouldReturnEmpty() {
        doReturn(List.of()).when(repository).findAll();
        doReturn(10000.0).when(config).getMaxDistance();
        doReturn(5).when(config).getMaxResult();

        List<Toilet> result = service.getNearestToilets(currentPosition);

        assertTrue(result.isEmpty());
    }

    @Test
    void getNearestToilets_whenMaxDistanceIsZero_shouldReturnEmpty() {
        List<ToiletDAO> repoResult = TestsUtils.buildBddContent(currentPosition);
        doReturn(repoResult).when(repository).findAll();

        doAnswer(invocation -> {
            ToiletDAO dao = invocation.getArgument(0);
            GeoPositionDAO p = dao.getPosition();
            GeoPosition pos = GeoPosition.builder()
                    .latitude(p.getLatitude())
                    .longitude(p.getLongitude())
                    .build();
            return Toilet.builder().libelle(dao.getLibelle()).position(pos).build();
        }).when(mapper).daoToBo(any(ToiletDAO.class));

        doReturn(0.0).when(config).getMaxDistance();
        doReturn(5).when(config).getMaxResult();

        List<Toilet> result = service.getNearestToilets(currentPosition);

        assertTrue(result.isEmpty());
    }

    @Test
    void getNearestToilets_withNullPosition_shouldThrowNullPointerException() {
        assertThrows(NullPointerException.class, () -> service.getNearestToilets(null));
    }

    @Test
    void getNearestToilets_whenRepositoryThrows_shouldPropagate() {
        doThrow(new RuntimeException("DB error")).when(repository).findAll();

        assertThrows(RuntimeException.class, () -> service.getNearestToilets(currentPosition));
    }

    // ========================================================================
    // getToiletById - Récupère une toilette par son ID
    // Cas : trouvé, non trouvé, id null
    // ========================================================================

    @Test
    void getToiletById_withExistingId_shouldReturnToilet() {
        ToiletDAO dao = new ToiletDAO();
        dao.setId(1L);
        dao.setLibelle("Found Toilet");
        Toilet expected = Toilet.builder().id(1L).libelle("Found Toilet").build();

        doReturn(Optional.of(dao)).when(repository).findById(1L);
        doReturn(expected).when(mapper).daoToBo(dao);

        Optional<Toilet> result = service.getToiletById(1L);

        assertTrue(result.isPresent());
        assertEquals("Found Toilet", result.get().getLibelle());
        verify(repository).findById(1L);
        verify(mapper).daoToBo(dao);
    }

    @Test
    void getToiletById_withNonExistingId_shouldReturnEmpty() {
        doReturn(Optional.empty()).when(repository).findById(999L);

        Optional<Toilet> result = service.getToiletById(999L);

        assertTrue(result.isEmpty());
    }

    @Test
    void getToiletById_withNullId_shouldThrowNullPointerException() {
        assertThrows(NullPointerException.class, () -> service.getToiletById(null));
    }

    @Test
    void getToiletById_whenRepositoryThrows_shouldPropagate() {
        doThrow(new RuntimeException("DB error")).when(repository).findById(1L);

        assertThrows(RuntimeException.class, () -> service.getToiletById(1L));
    }

    // ========================================================================
    // updateToilet - Met à jour une toilette existante
    // Cas : nominal, not found, id null, data null, repository exception
    // ========================================================================

    @Test
    void updateToilet_withExistingId_shouldUpdateAndSave() {
        ToiletDAO existingDao = new ToiletDAO();
        existingDao.setId(1L);
        existingDao.setLibelle("Old Name");
        Toilet updateData = Toilet.builder().libelle("New Name").build();

        doReturn(Optional.of(existingDao)).when(repository).findById(1L);
        doReturn(existingDao).when(repository).save(existingDao);

        service.updateToilet(1L, updateData);

        verify(repository).findById(1L);
        verify(mapper).updateToiletteDAOFromBO(updateData, existingDao);
        verify(repository).save(existingDao);
    }

    @Test
    void updateToilet_withNonExistingId_shouldThrowResourceNotFoundException() {
        doReturn(Optional.empty()).when(repository).findById(999L);

        assertThrows(ResourceNotFoundException.class, () -> service.updateToilet(999L, Toilet.builder().build()));
        verify(repository, never()).save(any());
    }

    @Test
    void updateToilet_withNullId_shouldThrowNullPointerException() {
        assertThrows(NullPointerException.class, () -> service.updateToilet(null, Toilet.builder().build()));
    }

    @Test
    void updateToilet_withNullData_shouldThrowNullPointerException() {
        doReturn(Optional.of(new ToiletDAO())).when(repository).findById(1L);

        assertThrows(NullPointerException.class, () -> service.updateToilet(1L, null));
    }

    @Test
    void updateToilet_whenRepositoryThrowsOnFind_shouldPropagate() {
        doThrow(new RuntimeException("DB error")).when(repository).findById(1L);

        assertThrows(RuntimeException.class, () -> service.updateToilet(1L, Toilet.builder().build()));
    }

    @Test
    void updateToilet_whenRepositoryThrowsOnSave_shouldPropagate() {
        ToiletDAO existingDao = new ToiletDAO();
        existingDao.setId(1L);

        doReturn(Optional.of(existingDao)).when(repository).findById(1L);
        doThrow(new RuntimeException("Save error")).when(repository).save(existingDao);

        assertThrows(RuntimeException.class, () -> service.updateToilet(1L, Toilet.builder().build()));
    }

    // ========================================================================
    // deleteToilet - Supprime une toilette par ID
    // Cas : nominal, not found, id null, repository exception
    // ========================================================================

    @Test
    void deleteToilet_withExistingId_shouldDelete() {
        doReturn(true).when(repository).existsById(1L);

        service.deleteToilet(1L);

        verify(repository).existsById(1L);
        verify(repository).deleteById(1L);
    }

    @Test
    void deleteToilet_withNonExistingId_shouldThrowResourceNotFoundException() {
        doReturn(false).when(repository).existsById(999L);

        assertThrows(ResourceNotFoundException.class, () -> service.deleteToilet(999L));
        verify(repository, never()).deleteById(any());
    }

    @Test
    void deleteToilet_withNullId_shouldThrowNullPointerException() {
        assertThrows(NullPointerException.class, () -> service.deleteToilet(null));
    }

    @Test
    void deleteToilet_whenRepositoryThrowsOnExists_shouldPropagate() {
        doThrow(new RuntimeException("DB error")).when(repository).existsById(1L);

        assertThrows(RuntimeException.class, () -> service.deleteToilet(1L));
    }

    @Test
    void deleteToilet_whenRepositoryThrowsOnDelete_shouldPropagate() {
        doReturn(true).when(repository).existsById(1L);
        doThrow(new RuntimeException("Delete error")).when(repository).deleteById(1L);

        assertThrows(RuntimeException.class, () -> service.deleteToilet(1L));
    }
    @Test
    void gradeToilet_shouldDelegateToMapperAndPersistHistory() {
        ToiletDAO dao = TestsUtils.buildToiletDAO();
        Graduation graduation = Graduation.builder().accueil(9).espace(7).proprete(5).build();

        doReturn(Optional.of(dao)).when(repository).findById(1L);
        // Le mapper est simulé : il ajoute la note à l'historique et renvoie l'entité
        dao.setGraduationHistory(new ArrayList<>());
        doAnswer(invocation -> {
            ToiletDAO target = invocation.getArgument(0);
            Graduation grade = invocation.getArgument(1);
            GraduationDAO entry = new GraduationDAO();
            entry.setAccueil(grade.getAccueil());
            entry.setEspace(grade.getEspace());
            entry.setProprete(grade.getProprete());
            target.getGraduationHistory().add(entry);
            return target;
        }).when(mapper).gradeToilet(dao, graduation);
        doReturn(TestsUtils.buildToilet()).when(mapper).daoToBo(dao);
        doReturn(dao).when(repository).save(dao);

        Toilet result = service.gradeToilet(1L, graduation);

        verify(mapper).gradeToilet(dao, graduation);
        verify(repository).save(dao);
        assertNotNull(result, "the converted toilet must be returned");
        assertNotNull(result.getGrade(), "the returned grade must not be null");
        assertEquals(1, dao.getGraduationHistory().size(), "the history must contain the new rating");
    }

    @Test
    void gradeToilet_whenToiletNotFound_shouldThrowResourceNotFoundException() {
        doReturn(TestsUtils.buildEmptyToilette()).when(repository).findById(999L);

        assertThrows(ResourceNotFoundException.class,
                () -> service.gradeToilet(999L, Graduation.builder().accueil(5).espace(5).proprete(5).build()));

        verify(repository, never()).save(any(ToiletDAO.class));
    }

    @Test
    void gradeToilet_withNullGraduation_shouldThrowNullPointerException() {
        assertThrows(NullPointerException.class, () -> service.gradeToilet(1L, null));

        verify(repository, never()).findById(any());
        verify(mapper, never()).gradeToilet(any(ToiletDAO.class), any(Graduation.class));
    }
}