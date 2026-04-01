package org.manu.wcpregnant.bdd.mapper;

import org.manu.wcpregnant.bdd.dao.GraduationDAO;
import org.manu.wcpregnant.web.dto.Graduation;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
interface GraduationDAOMapper {
    Graduation daoToBo(GraduationDAO otherIn);

    GraduationDAO boToDao(Graduation otherIn);
}
