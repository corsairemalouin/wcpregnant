package org.manu.wcpregnant.bdd.mapper;

import org.manu.wcpregnant.bdd.dao.ToiletDAO;
import org.manu.wcpregnant.web.dto.Toilet;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * Mapper MapStruct entre l'entité {@link ToiletDAO} et l'objet métier {@link Toilet}.
 *
 * <p>Le libellé est volontairement copié "tel quel" : sans mapping explicite, MapStruct
 * réutilise la méthode {@code toUpperCase} héritée des mappers listés dans {@code uses}
 * ({@code AdressDAOMapper}), ce qui transformait implicitement le libellé en majuscules.</p>
 */
@Mapper(componentModel = "spring", uses = { GraduationDAOMapper.class, AdressDAOMapper.class })
public interface ToiletteDAOMapper {

    @Mapping(target = "libelle", expression = "java(otherIn.getLibelle())")
    Toilet daoToBo(ToiletDAO otherIn);

    @Mapping(target = "libelle", expression = "java(otherIn.getLibelle())")
    ToiletDAO boToDao(Toilet otherIn);

    @Mapping(target = "libelle", expression = "java(boData.getLibelle())")
    void updateToiletteDAOFromBO(Toilet boData, @MappingTarget ToiletDAO dtoData);

}