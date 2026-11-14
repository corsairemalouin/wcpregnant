package org.manu.wcpregnant.bdd.mapper;

import org.manu.wcpregnant.bdd.dao.XxxDAO;
import org.manu.wcpregnant.web.dto.Xxx;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * Mapper MapStruct entre l'entité {@link XxxDAO} et l'objet métier {@link Xxx}.
 *
 * <p>Les champs dont la transformation n'est pas triviale (ex. {@code libelle})
 * doivent être mappés explicitement avec {@code expression = "java(...)"} pour
 * éviter qu'un mapper listé dans {@code uses} applique une transformation implicite.</p>
 */
@Mapper(componentModel = "spring")
public interface XxxDAOMapper {

    Xxx daoToBo(XxxDAO dao);

    XxxDAO boToDao(Xxx dto);

    void updateDaoFromBo(Xxx dto, @MappingTarget XxxDAO dao);
}
