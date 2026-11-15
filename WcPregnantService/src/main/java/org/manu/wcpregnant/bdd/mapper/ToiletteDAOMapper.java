package org.manu.wcpregnant.bdd.mapper;

import org.manu.wcpregnant.bdd.dao.GraduationDAO;
import org.manu.wcpregnant.bdd.dao.ToiletDAO;
import org.manu.wcpregnant.web.dto.Graduation;
import org.manu.wcpregnant.web.dto.Toilet;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/**
 * Mapper MapStruct entre l'entité {@link ToiletDAO} et l'objet métier {@link Toilet}.
 *
 * <p>Le libellé est volontairement copié "tel quel" : sans mapping explicite, MapStruct
 * réutilise la méthode {@code toUpperCase} héritée des mappers listés dans {@code uses}
 * ({@code AdressDAOMapper}), ce qui transformait implicitement le libellé en majuscules.</p>
 *
 * <p>La {@code distance} est calculée par le service de proximité et n'est jamais persistée.
 * L'historique des notes ({@code graduationHistory}) est exposé en lecture seule : il n'est
 * jamais écrit à partir du DTO, seule la méthode {@link #gradeToilet(ToiletDAO, Graduation)}
 * l'enrichit.</p>
 */
@Mapper(componentModel = "spring", uses = { GraduationDAOMapper.class, AdressDAOMapper.class })
public interface ToiletteDAOMapper {

    @Mapping(target = "libelle", expression = "java(otherIn.getLibelle())")
    @Mapping(target = "distance", ignore = true)
    Toilet daoToBo(ToiletDAO otherIn);

    @Mapping(target = "libelle", expression = "java(otherIn.getLibelle())")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "graduationHistory", ignore = true)
    ToiletDAO boToDao(Toilet otherIn);

    @Mapping(target = "libelle", expression = "java(boData.getLibelle())")
    @Mapping(target = "graduationHistory", ignore = true)
    void updateToiletteDAOFromBO(Toilet boData, @MappingTarget ToiletDAO dtoData);

    /**
     * Enregistre une note déposée par un visiteur sur la toilette.
     *
     * <p>La note est d'abord bornée à l'intervalle {@code [0, 10]}, ajoutée à l'historique
     * des visites de la toilette, puis la note affichée ({@code grade}) est recalculée comme
     * la moyenne de toutes les notes de cet historique.</p>
     *
     * @param toiletDao toilette à noter, non {@code null}
     * @param graduation note déposée par le visiteur, non {@code null}
     * @return la même entité {@code toiletDao}, enrichie de la note et de son historique
     */
    default ToiletDAO gradeToilet(ToiletDAO toiletDao, Graduation graduation) {
        GraduationDAO entry = new GraduationDAO();
        entry.setAccueil(clamp(graduation.getAccueil()));
        entry.setEspace(clamp(graduation.getEspace()));
        entry.setProprete(clamp(graduation.getProprete()));
        entry.setGradedAt(Instant.now());

        List<GraduationDAO> history = toiletDao.getGraduationHistory();
        if (history == null) {
            history = new ArrayList<>();
            toiletDao.setGraduationHistory(history);
        }
        history.add(entry);

        toiletDao.setGrade(average(history));
        return toiletDao;
    }

    /**
     * Convertit un {@code Instant} persisté en {@code OffsetDateTime} UTC pour l'exposition API.
     *
     * @param instant horodatage de la visite, peut être {@code null}
     * @return horodatage au format ISO-8601 en UTC, ou {@code null}
     */
    default OffsetDateTime toOffsetDateTime(Instant instant) {
        return instant == null ? null : OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    /**
     * Bornent une valeur de critère de notation dans l'intervalle autorisé {@code [0, 10]}.
     *
     * @param value valeur brute du critère
     * @return {@code 0} si la valeur est négative, {@code 10} si elle dépasse 10
     */
    private int clamp(int value) {
        return Math.min(10, Math.max(0, value));
    }

    /**
     * Moyenne arithmétique des trois critères de chaque entrée de l'historique.
     *
     * @param history historique des notes, non {@code null} mais pouvant être vide
     * @return note agrégée arrondie ; {@code (0, 0, 0)} si l'historique est vide
     */
    private GraduationDAO average(List<GraduationDAO> history) {
        GraduationDAO average = new GraduationDAO();
        if (history.isEmpty()) {
            return average;
        }

        double accueil = 0D;
        double espace = 0D;
        double proprete = 0D;
        for (GraduationDAO entry : history) {
            accueil += entry.getAccueil();
            espace += entry.getEspace();
            proprete += entry.getProprete();
        }

        int size = history.size();
        average.setAccueil(round(accueil / size));
        average.setEspace(round(espace / size));
        average.setProprete(round(proprete / size));
        return average;
    }

    /**
     * Arrondit une moyenne au plus proche entier.
     *
     * @param value moyenne calculée
     * @return valeur arrondie
     */
    private int round(double value) {
        return (int) Math.round(value);
    }
}
