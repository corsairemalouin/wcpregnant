package org.manu.wcpregnant.bdd.mapper;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.manu.wcpregnant.bdd.dao.AdressDAO;
import org.manu.wcpregnant.web.dto.Adress;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
 class AdressDAOMapperTest {
    @Autowired
    AdressDAOMapper mapper;

    @Test
     void daoToBo() {
        AdressDAO daoObj = new AdressDAO();
        daoObj.setNumero("19 bis");
        daoObj.setVoie("rue de la baie");
        daoObj.setCodePostal("35350");
        daoObj.setVille("La Gouesniere");
        daoObj.setPays("France");

        Adress boObj = mapper.daoToBo(daoObj);

        Assertions.assertNotNull(boObj);
        Assertions.assertEquals("35350",boObj.getCodePostal());
    }

    @Test
     void boToDao() {
        Adress boObj = Adress.builder().numero("19 bis").voie("rue de la baie").codePostal("35350")
                .ville("La Gouesniere").build();

        AdressDAO daoObj = mapper.boToDao(boObj);

        Assertions.assertNotNull(boObj);
        Assertions.assertEquals("35350",boObj.getCodePostal());
    }
}