package org.manu.wcpregnant.bdd.mapper;

import org.manu.wcpregnant.bdd.dao.AdressDAO;
import org.manu.wcpregnant.web.dto.Adress;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AdressDAOMapper {
    @Mapping(target = "voie", expression = "java(toUpperCase(otherIn.getVoie()))")
    @Mapping(target = "ville", expression = "java(toUpperCase(otherIn.getVille()))")
    Adress daoToBo(AdressDAO otherIn);

    @Mapping(target = "voie", expression = "java(toUpperCase(otherIn.getVoie()))")
    @Mapping(target = "ville", expression = "java(toUpperCase(otherIn.getVille()))")
    AdressDAO boToDao(Adress otherIn);

    default String toUpperCase(String field) {
        return (field == null) ? null : field.toUpperCase();
    }

}
