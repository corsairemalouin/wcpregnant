package org.manu.wcpregnant.service;

import org.manu.wcpregnant.bdd.dao.ToiletDAO;
import org.manu.wcpregnant.web.dto.Toilet;

public interface ToiletConverter {
    Toilet daoToBo(ToiletDAO otherIn);
    ToiletDAO boToDao(Toilet otherIn);
    void updateToiletteDAOFromBO(Toilet boData, ToiletDAO dtoData);
}
