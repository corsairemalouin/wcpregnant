package org.manu.wcpregnant.agent.tool;

import lombok.extern.slf4j.Slf4j;
import org.manu.wcpregnant.config.AgentToolConfig;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Registre des outils exposés au LLM local (Strata AI / Qwen-3.8-Flash-Next)
 * au format <em>OpenAI Function Calling</em>.
 *
 * <p>Chaque outil est un simple <strong>adaptateur</strong> d'une capacité
 * métier déjà implémentée dans la couche {@code service} :</p>
 * <ul>
 *   <li>{@code search_toilettes_proches} → {@link org.manu.wcpregnant.service.ToiletteProximityService}</li>
 *   <li>{@code get_details_toilette} → {@link org.manu.wcpregnant.service.ToiletteCrudService}</li>
 *   <li>{@code gerer_equipement_crud} → {@link org.manu.wcpregnant.service.ToiletteCrudService}</li>
 * </ul>
 *
 * <p>Respecte <strong>SRP</strong> (déclaration du contrat) et <strong>OCP</strong> :
 * ajouter un outil consiste uniquement à ajouter une {@link ToolDefinition} dans
 * la liste construite par {@link #buildTools(AgentToolConfig)}.</p>
 *
 * @see ToolRegistry
 * @see ToiletToolExecutor
 */
@Slf4j
@Component
public class ToiletToolRegistry implements ToolRegistry {

    /** Nom de l'outil de recherche géographique par proximité (Haversine). */
    public static final String SEARCH_NEAR_TOILETS = "search_toilettes_proches";

    /** Nom de l'outil de consultation d'un équipement. */
    public static final String GET_TOILET_DETAILS = "get_details_toilette";

    /** Nom de l'outil de gestion (création / mise à jour / suppression) d'un équipement. */
    public static final String MANAGE_EQUIPMENT_CRUD = "gerer_equipement_crud";

    private final List<ToolDefinition> tools;
    private final ObjectMapper objectMapper;

    /**
     * Construit le registre à partir de la configuration agentique.
     *
     * @param objectMapper mapper Jackson utilisé pour sérialiser le registre en JSON
     * @param config       configuration des limites applicables aux outils. Ne doit pas être {@code null}.
     */
    public ToiletToolRegistry(ObjectMapper objectMapper, AgentToolConfig config) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        Objects.requireNonNull(config, "config must not be null");
        this.tools = buildTools(config);
        log.debug("Agent tool registry initialised with {} tools: {}",
                tools.size(), tools.stream().map(ToolDefinition::name).toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ToolDefinition> getTools() {
        return tools;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<ToolDefinition> findTool(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        return tools.stream()
                .filter(tool -> tool.name().equals(name))
                .findFirst();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Map<String, Object>> openAiTools() {
        return tools.stream()
                .map(ToolDefinition::toOpenAiEnvelope)
                .toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toJson() {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(openAiTools());
        } catch (Exception e) {
            log.error("Unable to serialise the agent tool registry", e);
            throw new IllegalStateException("Unable to serialise the agent tool registry", e);
        }
    }
    // ========================================================================
    // Déclarations des outils
    // ========================================================================

    /**
     * Déclare les outils du domaine "toilettes".
     *
     * @param config configuration agentique fournissant les bornes de rayon
     *                et le nombre maximal de résultats
     * @return liste immuable des définitions d'outils
     */
    private List<ToolDefinition> buildTools(AgentToolConfig config) {
        return List.of(
                proximityTool(config),
                detailsTool(),
                crudTool()
        );
    }

    /**
     * Outil de recherche géographique : {@code search_toilettes_proches}.
     *
     * @param config configuration agentique
     * @return la définition de l'outil de proximité
     */
    private ToolDefinition proximityTool(AgentToolConfig config) {
        Map<String, PropertySchema> properties = new LinkedHashMap<>();
        properties.put("latitude", PropertySchema.number(
                "Latitude du point de recherche, en degrés décimaux (-90 à 90)", -90.0, 90.0));
        properties.put("longitude", PropertySchema.number(
                "Longitude du point de recherche, en degrés décimaux (-180 à 180)", -180.0, 180.0));
        properties.put("rayon_metres", PropertySchema.number(
                "Rayon maximal de recherche en mètres. Défaut : " + config.getDefaultRadius()
                        + " m, maximum : " + config.getMaxRadius() + " m",
                1.0, config.getMaxRadius()));
        properties.put("table_a_langer", PropertySchema.booleanType(
                "Filtre 'table à langer' (true = uniquement les équipements équipés). "
                        + "Attribut encore absent du modèle de données : il est ignoré et signalé dans la réponse"));

        return ToolDefinition.function(
                SEARCH_NEAR_TOILETS,
                "Recherche les toilettes publiques les plus proches d'une position GPS en utilisant la "
                        + "distance de Haversine. Les résultats sont triés du plus proche au plus éloigné.",
                JsonSchema.object(properties, List.of("latitude", "longitude"))
        );
    }

    /**
     * Outil de consultation : {@code get_details_toilette}.
     *
     * @return la définition de l'outil de détail d'un équipement
     */
    private ToolDefinition detailsTool() {
        Map<String, PropertySchema> properties = new LinkedHashMap<>();
        properties.put("id", PropertySchema.integer(
                "Identifiant technique de la toilette à consulter", 1, null));

        return ToolDefinition.function(
                GET_TOILET_DETAILS,
                "Récupère la fiche complète d'une toilette publique : libellé, adresse, position GPS, "
                        + "note moyenne et historique des visites.",
                JsonSchema.object(properties, List.of("id"))
        );
    }

    /**
     * Outil de gestion : {@code gerer_equipement_crud}.
     *
     * @return la définition de l'outil CRUD d'un équipement
     */
    private ToolDefinition crudTool() {
        Map<String, PropertySchema> properties = new LinkedHashMap<>();
        properties.put("action", PropertySchema.enumerated(
                "Action à effectuer sur l'équipement", CrudAction.allowedValues()));
        properties.put("id", PropertySchema.integer(
                "Identifiant de la toilette concernée. Obligatoire pour UPDATE et DELETE", 1, null));
        properties.put("payload", PropertySchema.object(
                "Objet 'Toilet' complet à créer ou à mettre à jour : "
                        + "{ \"libelle\": string, \"adress\": { \"numero\", \"voie\", \"codePostal\", \"ville\", \"pays\" }, "
                        + "\"position\": { \"latitude\": number, \"longitude\": number }, "
                        + "\"grade\": { \"proprete\": 0-10, \"accueil\": 0-10, \"espace\": 0-10 } }. "
                        + "Obligatoire pour CREATE et UPDATE, ignoré pour DELETE"));

        return ToolDefinition.function(
                MANAGE_EQUIPMENT_CRUD,
                "Crée, met à jour ou supprime un équipement sanitaire. "
                        + "Réservé aux utilisateurs disposant du rôle ADMIN.",
                JsonSchema.object(properties, List.of("action"))
        );
    }

}
