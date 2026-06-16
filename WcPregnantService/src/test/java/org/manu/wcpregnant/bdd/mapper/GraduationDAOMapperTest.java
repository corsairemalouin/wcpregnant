package org.manu.wcpregnant.bdd.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.manu.wcpregnant.bdd.dao.GraduationDAO;
import org.manu.wcpregnant.web.dto.Graduation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class GraduationDAOMapperTest {

    @Autowired
    GraduationDAOMapper mapper;

    @Test
    void daoToBo_and_boToDao_shouldCopyFields() {
        GraduationDAO dao = new GraduationDAO();
        dao.setAccueil(2);
        dao.setProprete(3);
        dao.setEspace(4);

        Graduation bo = mapper.daoToBo(dao);

        assertEquals(2, bo.getAccueil());
        assertEquals(3, bo.getProprete());
        assertEquals(4, bo.getEspace());

        GraduationDAO dao2 = mapper.boToDao(bo);
        assertEquals(2, dao2.getAccueil());
    }
}

