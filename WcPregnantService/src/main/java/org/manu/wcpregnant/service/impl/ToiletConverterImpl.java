package org.manu.wcpregnant.service.impl;

import lombok.RequiredArgsConstructor;
import org.manu.wcpregnant.bdd.dao.ToiletDAO;
import org.manu.wcpregnant.bdd.mapper.ToiletteDAOMapper;
import org.manu.wcpregnant.service.ToiletConverter;
import org.manu.wcpregnant.web.dto.Graduation;
import org.manu.wcpregnant.web.dto.Toilet;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Adaptateur du domaine vers la couche de persistance.
 *
 * <p>Ce composant expose le mapping objet métier / entité JPA aux services
 * sans que ceux-ci aient à connaître MapStruct : toute la logique de
 * conversion reste dans {@link ToiletteDAOMapper} (principe <strong>DIP</strong>).</p>
 *
 * @see ToiletteDAOMapper
 */
@Component
@RequiredArgsConstructor
public class ToiletConverterImpl implements ToiletConverter {
    private final ToiletteDAOMapper mapper;

    /**
     * {@inheritDoc}
     *
     * <p>Délègue la conversion à MapStruct : l'entité {@link ToiletDAO} et ses
     * relations (adresse, position, notes) sont transformées en DTO {@link Toilet}
     * prêt à être sérialisé par l'API.</p>
     */
    @Override
    public Toilet daoToBo(ToiletDAO otherIn) {
        return mapper.daoToBo(otherIn);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Construit une entité {@link ToiletDAO} vierge à partir du DTO reçu par
     * l'API, sans identifiant : l'identifiant est attribué par la base lors de
     * la sauvegarde.</p>
     */
    @Override
    public ToiletDAO boToDao(Toilet otherIn) {
        return mapper.boToDao(otherIn);
    }

    /**
     * {@inheritDoc}
     *
     * <p>La mise à jour est faite <em>en place</em> sur l'entité déjà chargée,
     * ce qui préserve l'identifiant, l'historique des notes et les relations
     * existantes.</p>
     */
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
