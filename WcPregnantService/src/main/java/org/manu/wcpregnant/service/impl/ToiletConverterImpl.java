package org.manu.wcpregnant.service.impl;

import lombok.RequiredArgsConstructor;
import org.manu.wcpregnant.bdd.dao.ToiletDAO;
import org.manu.wcpregnant.bdd.mapper.ToiletteDAOMapper;
import org.manu.wcpregnant.service.ToiletConverter;
import org.manu.wcpregnant.web.dto.Toilet;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ToiletConverterImpl implements ToiletConverter {
    private final ToiletteDAOMapper mapper;

    @Override
    public Toilet daoToBo(ToiletDAO otherIn) {
        return mapper.daoToBo(otherIn);
    }

    @Override
    public ToiletDAO boToDao(Toilet otherIn) {
        return mapper.boToDao(otherIn);
    }

    @Override
    public void updateToiletteDAOFromBO(Toilet boData, ToiletDAO dtoData) {
        mapper.updateToiletteDAOFromBO(boData, dtoData);
    }
}
