package org.manu.wcpregnant.agent.tool;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Objects;

/**
 * Description d'une propriété d'un schéma JSON au sens de la spécification
 * JSON Schema, telle qu'attendue par le <em>Function Calling</em> compatible
 * OpenAI (Qwen-3.8-Flash-Next via Strata AI local).
 *
 * <p>Les champs {@code null} ne sont pas sérialisés : un outil ne déclare que
 * les contraintes réellement utiles, ce qui garde le prompt compact pour le
 * modèle.</p>
 *
 * @see JsonSchema
 * @see ToolDefinition
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PropertySchema(
        String type,
        String description,
        List<String> enumValues,
        Double minimum,
        Double maximum
) {

    /**
     * Constructeur validant : le type est obligatoire et l'intervalle
     * {@code minimum}/{@code maximum} doit être cohérent.
     *
     * @param type        type JSON de la propriété ({@code string}, {@code number},
     *                    {@code integer}, {@code boolean}, {@code object}).
     * @param description description destinée au LLM, expliquant l'usage du paramètre.
     * @param enumValues  valeurs autorisées, {@code null} si la propriété est libre.
     * @param minimum     borne basse (inclusive) pour un type numérique, {@code null} si non contrainte.
     * @param maximum     borne haute (inclusive) pour un type numérique, {@code null} si non contrainte.
     * @throws IllegalArgumentException si le type est vide ou si {@code minimum > maximum}
     */
    public PropertySchema {
        Objects.requireNonNull(type, "type must not be null");
        if (type.isBlank()) {
            throw new IllegalArgumentException("type must not be blank");
        }
        if (minimum != null && maximum != null && minimum > maximum) {
            throw new IllegalArgumentException("minimum must not be greater than maximum");
        }
        if (enumValues != null && enumValues.isEmpty()) {
            throw new IllegalArgumentException("enumValues must not be empty");
        }
    }

    /**
     * Propriété de type {@code string}.
     *
     * @param description description du paramètre pour le LLM
     * @return un {@link PropertySchema} de type {@code string}
     */
    public static PropertySchema string(String description) {
        return new PropertySchema("string", description, null, null, null);
    }

    /**
     * Propriété de type {@code number} (double), bornée.
     *
     * @param description description du paramètre
     * @param minimum     borne basse, peut être {@code null}
     * @param maximum     borne haute, peut être {@code null}
     * @return un {@link PropertySchema} de type {@code number}
     */
    public static PropertySchema number(String description, Double minimum, Double maximum) {
        return new PropertySchema("number", description, null, minimum, maximum);
    }

    /**
     * Propriété de type {@code integer} (entier), bornée.
     *
     * @param description description du paramètre
     * @param minimum     borne basse, peut être {@code null}
     * @param maximum     borne haute, peut être {@code null}
     * @return un {@link PropertySchema} de type {@code integer}
     */
    public static PropertySchema integer(String description, Integer minimum, Integer maximum) {
        return new PropertySchema("integer", description, null,
                minimum == null ? null : minimum.doubleValue(),
                maximum == null ? null : maximum.doubleValue());
    }

    /**
     * Propriété de type {@code boolean}.
     *
     * @param description description du paramètre
     * @return un {@link PropertySchema} de type {@code boolean}
     */
    public static PropertySchema booleanType(String description) {
        return new PropertySchema("boolean", description, null, null, null);
    }

    /**
     * Propriété de type {@code string} contrainte par une liste de valeurs autorisées.
     *
     * @param description description du paramètre
     * @param values      valeurs autorisées, non vide
     * @return un {@link PropertySchema} de type {@code string} avec une énumération
     */
    public static PropertySchema enumerated(String description, List<String> values) {
        return new PropertySchema("string", description, List.copyOf(values), null, null);
    }

    /**
     * Propriété de type {@code object} (structure libre, détaillée ailleurs).
     *
     * @param description description du paramètre
     * @return un {@link PropertySchema} de type {@code object}
     */
    public static PropertySchema object(String description) {
        return new PropertySchema("object", description, null, null, null);
    }
}
