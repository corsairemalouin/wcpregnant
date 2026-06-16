package org.manu.wcpregnant.service;

import org.manu.wcpregnant.service.impl.ToiletteServiceImpl;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.manu.wcpregnant.bdd.dao.GeoPositionDAO;
import org.manu.wcpregnant.bdd.dao.ToiletDAO;
import org.manu.wcpregnant.bdd.mapper.ToiletteDAOMapper;
import org.manu.wcpregnant.bdd.repository.ToiletteRepository;
import org.manu.wcpregnant.config.ToiletteServiceConfig;
import org.manu.wcpregnant.utils.TestsUtils;
import org.manu.wcpregnant.web.dto.GeoPosition;
import org.manu.wcpregnant.web.dto.Toilet;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class ToiletteServiceImplUnitTest {

    private ToiletteServiceImpl service;

    @Mock
    private ToiletteRepository repository;

    @Mock
    private ToiletteDAOMapper mapper;

    @Mock
    private ToiletteServiceConfig config;

    private GeoPosition currentPosition = GeoPosition.builder().latitude(48.607348).longitude(-1.8898742).build();

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
        // create service with mocked deps
        service = new ToiletteServiceImpl(config, repository, mapper);
    }

    @Test
    void getNearestToilets_filters_sorts_and_limits() {
        List<ToiletDAO> repoResult = TestsUtils.buildBddContent(currentPosition);
        doReturn(repoResult).when(repository).findAll();

        // mapper: convert DAO -> DTO by copying position and name
        doAnswer(invocation -> {
            ToiletDAO dao = invocation.getArgument(0);
            GeoPositionDAO p = dao.getPosition();
            GeoPosition pos = GeoPosition.builder().latitude(p.getLatitude()).longitude(p.getLongitude()).build();
            Toilet t = Toilet.builder().libelle(dao.getLibelle()).position(pos).build();
            return t;
        }).when(mapper).daoToBo(any(ToiletDAO.class));

        doReturn(10000.0).when(config).getMaxDistance();
        doReturn(5).when(config).getMaxResult();

        List<Toilet> result = service.getNearsetToilettes(currentPosition);

        assertTrue(result.size() <= 5);
        if (result.size() >= 2) {
            assertTrue(result.get(0).getDistance() <= result.get(result.size() - 1).getDistance());
        }
    }
}

