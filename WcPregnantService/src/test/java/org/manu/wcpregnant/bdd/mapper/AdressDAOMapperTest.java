package org.manu.wcpregnant.bdd.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
    void daoToBo_shouldMapAllFields() {
        AdressDAO dao = new AdressDAO();
        dao.setNumero("19 bis");
        dao.setVoie("rue de la baie");
        dao.setCodePostal("35350");
        dao.setVille("La Gouesniere");
        dao.setPays("France");

        Adress bo = mapper.daoToBo(dao);

        assertNotNull(bo);
        assertEquals("19 BIS", bo.getNumero());
        assertEquals("RUE DE LA BAIE", bo.getVoie());
        assertEquals("35350", bo.getCodePostal()); // si tu veux garder les codes postaux inchangés, laisse tel quel
        assertEquals("LA GOUESNIERE", bo.getVille());
        assertEquals("FRANCE", bo.getPays());
    }

    @Test
    void boToDao_shouldMapAllFields() {
        Adress bo = Adress.builder()
                .numero("19 bis")
                .voie("rue de la baie")
                .codePostal("35350")
                .ville("La Gouesniere")
                .pays("France")
                .build();

        AdressDAO dao = mapper.boToDao(bo);

        assertNotNull(dao);
        assertEquals("19 BIS", dao.getNumero());
        assertEquals("RUE DE LA BAIE", dao.getVoie());
        assertEquals("35350", dao.getCodePostal());
        assertEquals("LA GOUESNIERE", dao.getVille());
        assertEquals("FRANCE", dao.getPays());
    }
}