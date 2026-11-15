package org.manu.wcpregnant.agent.tool;

import java.util.Arrays;
import java.util.List;

/**
 * Actions acceptées par l'outil {@code gerer_equipement_crud}.
 *
 * <p>Ces valeurs sont exposées telles quelles dans le schéma JSON de l'outil,
 * ce qui garantit que le LLM ne propose jamais une action non supportée.</p>
 */
public enum CrudAction {

    /** Création d'un équipement. */
    CREATE,

    /** Mise à jour d'un équipement existant. */
    UPDATE,

    /** Suppression d'un équipement. */
    DELETE;

    /**
     * Liste des valeurs autorisées, utilisée pour construire l'énumération du
     * schéma JSON envoyé au modèle.
     *
     * @return la liste ordonnée des noms des actions possibles
     */
    public static List<String> allowedValues() {
        return Arrays.stream(values()).map(Enum::name).toList();
    }

    /**
     * Convertit une chaîne provenant du LLM en {@link CrudAction}.
     *
     * @param value valeur fournie par le modèle, insensible à la casse.
     *              Ne doit pas être {@code null}.
     * @return l'action correspondante
     * @throws IllegalArgumentException si la valeur ne correspond à aucune action
     */
    public static CrudAction from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("action must not be null or blank");
        }
        return Arrays.stream(values())
                .filter(candidate -> candidate.name().equalsIgnoreCase(value.trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown action '" + value + "'. Allowed values: " + allowedValues()));
    }
}
