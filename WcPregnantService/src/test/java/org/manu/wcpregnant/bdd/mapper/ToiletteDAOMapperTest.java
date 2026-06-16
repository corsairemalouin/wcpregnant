package org.manu.wcpregnant.bdd.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.manu.wcpregnant.bdd.dao.ToiletDAO;
import org.manu.wcpregnant.utils.TestsUtils;
import org.manu.wcpregnant.web.dto.Toilet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ToiletteDAOMapperTest {

    @Autowired
    ToiletteDAOMapper mapper;

    @Test
    @Disabled
    void daoToBo_shouldMapFields() {
        ToiletDAO dao = TestsUtils.buildToiletDAO();
        Toilet bo = mapper.daoToBo(dao);
        assertNotNull(bo);
        assertEquals(dao.getLibelle(), bo.getLibelle());
        assertNotNull(bo.getPosition());
    }

    @Test
    @Disabled
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
}

