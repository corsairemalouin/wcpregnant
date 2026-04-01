package org.manu.wcpregnant.bdd.mapper;

import org.manu.wcpregnant.bdd.dao.ToiletDAO;
import org.manu.wcpregnant.web.dto.Toilet;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = { GraduationDAOMapper.class, AdressDAOMapper.class })
public interface ToiletteDAOMapper {
    Toilet daoToBo(ToiletDAO otherIn);

    ToiletDAO boToDao(Toilet otherIn);

    void updateToiletteDAOFromBO(Toilet boData, @MappingTarget ToiletDAO dtoData);

}