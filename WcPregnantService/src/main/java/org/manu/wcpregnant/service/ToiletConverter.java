package org.manu.wcpregnant.service;

import org.manu.wcpregnant.bdd.dao.ToiletDAO;
import org.manu.wcpregnant.web.dto.Graduation;
import org.manu.wcpregnant.web.dto.Toilet;

public interface ToiletConverter {
    Toilet daoToBo(ToiletDAO otherIn);
    ToiletDAO boToDao(Toilet otherIn);
    /**
     * Dépose une note issue d'une visite sur une toilette déjà chargée.
     *
     * <p>La note est ajoutée à l'historique des visites de la toilette et la
     * note affichée ({@code grade}) est recalculée comme la moyenne de toutes
     * les notes déposées.</p>
     *
     * @param toilet     la toilette persistée à enrichir. Ne doit pas être {@code null}.
     * @param graduation la note déposée par le visiteur. Ne doit pas être {@code null}.
     * @return la note moyenne de la toilette après l'ajout de la visite
     */
    ToiletDAO gradeToilet(ToiletDAO toilet, Graduation graduation);

    void updateToiletteDAOFromBO(Toilet boData, ToiletDAO dtoData);
}
