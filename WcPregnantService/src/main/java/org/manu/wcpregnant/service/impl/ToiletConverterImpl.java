package org.manu.wcpregnant.service.impl;

import lombok.RequiredArgsConstructor;
import org.manu.wcpregnant.bdd.dao.ToiletDAO;
import org.manu.wcpregnant.bdd.mapper.ToiletteDAOMapper;
import org.manu.wcpregnant.service.ToiletConverter;
import org.manu.wcpregnant.web.dto.Graduation;
import org.manu.wcpregnant.web.dto.Toilet;
import org.springframework.stereotype.Component;

import java.util.Objects;

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

    /**
     * {@inheritDoc}
     *
     * <p>Délègue la notation à {@link ToiletteDAOMapper#gradeToilet(ToiletDAO, Graduation)}
     * afin de garder la logique de mapping dans la couche {@code bdd}.</p>
     */
    @Override
    public ToiletDAO gradeToilet(ToiletDAO toilet, Graduation graduation) {
        Objects.requireNonNull(toilet, "toilet must not be null");
        Objects.requireNonNull(graduation, "graduation must not be null");
        return mapper.gradeToilet(toilet, graduation);
    }

}
