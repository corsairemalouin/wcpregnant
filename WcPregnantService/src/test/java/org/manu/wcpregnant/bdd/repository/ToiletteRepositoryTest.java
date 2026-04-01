package org.manu.wcpregnant.bdd.repository;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.manu.wcpregnant.bdd.dao.ToiletDAO;
import org.manu.wcpregnant.utils.TestsUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ToiletteRepositoryTest {

    @Autowired
    ToiletteRepository toiletteRepository;

    @Test
    void addToilet() {
        ToiletDAO newData = TestsUtils.buildToiletDAO();
        toiletteRepository.save(newData);

        List<ToiletDAO> loadData = toiletteRepository.findAll();

        Assertions.assertEquals(1, loadData.size());
    }

}