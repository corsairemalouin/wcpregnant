package org.manu.wcpregnant.agent.tool;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Objet JSON Schema (sous-ensemble suffisant pour le Function Calling OpenAI)
 * décrivant les arguments d'un outil agentique.
 *
 * <p>La map des propriétés est copiée dans un {@link LinkedHashMap} afin de
 * garantir un ordre stable dans le JSON envoyé au modèle, ce qui améliore la
 * reproductibilité des appels au LLM.</p>
 *
 * @see PropertySchema
 * @see ToolDefinition
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record JsonSchema(
        String type,
        Map<String, PropertySchema> properties,
        List<String> required
) {

    /**
     * Constructeur validant : le type est obligatoire et les collections sont
     * copiées défensivement (le schéma est donc immuable).
     *
     * @param type       type JSON du schéma, généralement {@code object}
     * @param properties propriétés déclarées, {@code null} acceptée
     * @param required   noms des propriétés obligatoires, {@code null} acceptée
     * @throws IllegalArgumentException si {@code type} est vide
     */
    public JsonSchema {
        Objects.requireNonNull(type, "type must not be null");
        if (type.isBlank()) {
            throw new IllegalArgumentException("type must not be blank");
        }
        properties = properties == null
                ? null
                : new LinkedHashMap<>(properties);
        required = required == null ? null : List.copyOf(required);
    }

    /**
     * Fabrique un schéma {@code object} à partir des propriétés données.
     *
     * @param properties propriétés de l'objet, ne doit pas être {@code null}
     * @param required   noms des propriétés obligatoires, peut être {@code null}
     * @return un {@link JsonSchema} immuable de type {@code object}
     */
    public static JsonSchema object(Map<String, PropertySchema> properties, List<String> required) {
        Objects.requireNonNull(properties, "properties must not be null");
        return new JsonSchema("object", properties, required);
    }

    /**
     * Rend le schéma sous forme de map ordonnée, prête à être insérée dans
     * l'enveloppe OpenAI {@code {"type":"function","function":{...}}}.
     *
     * @return une map ordonnée {@code type} / {@code properties} / {@code required}
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("type", type);
        if (properties != null && !properties.isEmpty()) {
            Map<String, Object> props = new LinkedHashMap<>();
            properties.forEach((name, schema) -> props.put(name, schema));
            map.put("properties", props);
        }
        if (required != null && !required.isEmpty()) {
            map.put("required", required);
        }
        return map;
    }
}
