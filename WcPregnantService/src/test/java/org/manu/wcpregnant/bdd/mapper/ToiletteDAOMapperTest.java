package org.manu.wcpregnant.bdd.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.manu.wcpregnant.bdd.dao.GraduationDAO;
import org.manu.wcpregnant.bdd.dao.ToiletDAO;
import org.manu.wcpregnant.utils.TestsUtils;
import org.manu.wcpregnant.web.dto.Graduation;
import org.manu.wcpregnant.web.dto.Toilet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ToiletteDAOMapperTest {

    @Autowired
    ToiletteDAOMapper mapper;

    @Test
    void daoToBo_shouldMapFields() {
        ToiletDAO dao = TestsUtils.buildToiletDAO();
        Toilet bo = mapper.daoToBo(dao);
        assertNotNull(bo);
        assertEquals(dao.getLibelle(), bo.getLibelle());
        assertNotNull(bo.getPosition());
    }

    @Test
    void boToDao_and_updateShouldWork() {
        Toilet bo = TestsUtils.buildToilet();
        ToiletDAO dao = mapper.boToDao(bo);
        assertNotNull(dao);
        assertEquals(bo.getLibelle(), dao.getLibelle());

        // update
        bo.setLibelle("updated name");
        mapper.updateToiletteDAOFromBO(bo, dao);
        assertEquals("updated name", dao.getLibelle());
    }

    @Test
    void gradeToilet_shouldStoreEntryAndSetGrade() {
        ToiletDAO dao = TestsUtils.buildToiletDAO();
        dao.setGraduationHistory(new ArrayList<>());

        Graduation graduation = Graduation.builder().accueil(8).espace(6).proprete(4).build();

        ToiletDAO graded = mapper.gradeToilet(dao, graduation);

        assertEquals(1, graded.getGraduationHistory().size());
        GraduationDAO stored = graded.getGraduationHistory().get(0);
        assertEquals(8, stored.getAccueil());
        assertEquals(6, stored.getEspace());
        assertEquals(4, stored.getProprete());
        assertNotNull(stored.getGradedAt());
        assertEquals(8, graded.getGrade().getAccueil());
        assertEquals(6, graded.getGrade().getEspace());
        assertEquals(4, graded.getGrade().getProprete());
    }

    @Test
    void gradeToilet_shouldAverageWithExistingHistory() {
        ToiletDAO dao = TestsUtils.buildToiletDAO();
        dao.setGraduationHistory(new ArrayList<>(List.of(graduationDAO(10, 10, 10))));

        mapper.gradeToilet(dao, Graduation.builder().accueil(0).espace(0).proprete(0).build());

        assertEquals(2, dao.getGraduationHistory().size());
        assertEquals(5, dao.getGrade().getAccueil());
        assertEquals(5, dao.getGrade().getEspace());
        assertEquals(5, dao.getGrade().getProprete());
    }

    @Test
    void gradeToilet_shouldClampOutOfRangeValues() {
        ToiletDAO dao = TestsUtils.buildToiletDAO();
        dao.setGraduationHistory(null);

        mapper.gradeToilet(dao, Graduation.builder().accueil(15).espace(-3).proprete(10).build());

        GraduationDAO stored = dao.getGraduationHistory().get(0);
        assertEquals(10, stored.getAccueil());
        assertEquals(0, stored.getEspace());
        assertEquals(10, stored.getProprete());
        assertEquals(1, dao.getGraduationHistory().size());
    }

    /**
     * Construit une entrée d'historique de notation sans horodatage.
     *
     * @param accueil note du critère accueil
     * @param espace note du critère espace
     * @param proprete note du critère propreté
     * @return entrée d'historique prête à être injectée dans une toilette
     */
    private GraduationDAO graduationDAO(int accueil, int espace, int proprete) {
        GraduationDAO entry = new GraduationDAO();
        entry.setAccueil(accueil);
        entry.setEspace(espace);
        entry.setProprete(proprete);
        return entry;
    }
}
