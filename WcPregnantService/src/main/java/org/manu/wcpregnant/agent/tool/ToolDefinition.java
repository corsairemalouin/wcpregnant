package org.manu.wcpregnant.agent.tool;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Définition d'un outil (tool) au format <em>OpenAI Function Calling</em>,
 * directement consommable par une API compatible OpenAI telle que Strata AI
 * servant le modèle local Qwen-3.8-Flash-Next.
 *
 * <p>Le modèle de données reste la source de vérité : le JSON envoyé au LLM est
 * produit depuis cette définition Java via {@link #toOpenAiEnvelope()}, ce qui
 * évite la duplication entre code et fichier de configuration.</p>
 *
 * @see JsonSchema
 * @see ToolRegistry
 */
public record ToolDefinition(
        String name,
        String description,
        JsonSchema parameters
) {

    /**
     * Constructeur validant : le nom, la description et le schéma d'arguments
     * sont obligatoires.
     *
     * @param name        nom technique de l'outil, unique dans le registre
     * @param description description fonctionnelle lue par le LLM
     * @param parameters  schéma JSON des arguments de l'outil
     * @throws IllegalArgumentException si l'un des arguments est {@code null} ou vide
     */
    public ToolDefinition {
        Objects.requireNonNull(name, "name must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        Objects.requireNonNull(description, "description must not be null");
        if (description.isBlank()) {
            throw new IllegalArgumentException("description must not be blank");
        }
        Objects.requireNonNull(parameters, "parameters must not be null");
    }

    /**
     * Fabrique une définition d'outil fonctionnel.
     *
     * @param name        nom de la fonction exposée au modèle
     * @param description description fonctionnelle de l'outil
     * @param parameters  schéma JSON des arguments
     * @return une {@link ToolDefinition} valide
     */
    public static ToolDefinition function(String name, String description, JsonSchema parameters) {
        return new ToolDefinition(name, description, parameters);
    }

    /**
     * Produit l'enveloppe exacte attendue par le champ {@code tools} d'une
     * requête {@code /chat/completions} :
     * <pre>
     * { "type": "function", "function": { "name": ..., "description": ..., "parameters": ... } }
     * </pre>
     *
     * @return une map ordonnée représentant l'outil au format OpenAI
     */
    public Map<String, Object> toOpenAiEnvelope() {
        Map<String, Object> function = new LinkedHashMap<>();
        function.put("name", name);
        function.put("description", description);
        function.put("parameters", parameters.toMap());

        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("type", "function");
        envelope.put("function", function);
        return envelope;
    }
}
