package org.manu.wcpregnant.agent.tool;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.manu.wcpregnant.config.AgentToolConfig;
import org.manu.wcpregnant.config.ToiletteServiceConfig;
import org.manu.wcpregnant.service.ToiletteCrudService;
import org.manu.wcpregnant.service.ToiletteProximityService;
import org.manu.wcpregnant.service.utils.ResourceNotFoundException;
import org.manu.wcpregnant.web.dto.GeoPosition;
import org.manu.wcpregnant.web.dto.Toilet;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Exécuteur des outils agentiques du domaine "toilettes".
 *
 * <p>Cet adaptateur ne contient <strong>aucune logique métier</strong> : il
 * valide les arguments issus du LLM puis délègue aux interfaces de service
 * existantes ({@link ToiletteProximityService}, {@link ToiletteCrudService}),
 * conformément au principe <strong>DIP</strong>.</p>
 *
 * <p>Le contrôle d'accès est exercé ici (défense en profondeur) : les opérations
 * d'écriture exigent le rôle {@code ADMIN}, exactement comme les endpoints REST
 * protégés par {@code @PreAuthorize}.</p>
 *
 * @see ToolExecutor
 * @see ToiletToolRegistry
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ToiletToolExecutor implements ToolExecutor {

    /** Rôle requis pour les opérations d'écriture exposées au LLM. */
    private static final String ADMIN_ROLE = "ADMIN";

    private final ToolRegistry registry;
    private final ToiletteProximityService proximityService;
    private final ToiletteCrudService crudService;
    private final AgentToolConfig agentConfig;
    private final ToiletteServiceConfig toiletteConfig;
    private final ObjectMapper objectMapper;

    /**
     * {@inheritDoc}
     *
     * <p>Le nom de l'outil est d'abord confronté au registre : un outil inconnu
     * est rejeté, ce qui empêche le modèle d'inventer une capacité.</p>
     */
    @Override
    public ToolResult execute(ToolCall call) {
        Objects.requireNonNull(call, "call must not be null");

        ToolDefinition tool = registry.findTool(call.name())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown tool '" + call.name() + "'. Available tools: " + availableToolNames()));

        log.debug("Executing agent tool '{}' with arguments {}", tool.name(), call.arguments());

        return switch (tool.name()) {
            case ToiletToolRegistry.SEARCH_NEAR_TOILETS -> searchNearToilets(call.arguments());
            case ToiletToolRegistry.GET_TOILET_DETAILS -> getToiletDetails(call.arguments());
            case ToiletToolRegistry.MANAGE_EQUIPMENT_CRUD -> manageEquipment(call.arguments());
            default -> throw new IllegalArgumentException("No executor registered for tool '" + tool.name() + "'");
        };
    }

    /**
     * Adaptateur de l'outil {@code search_toilettes_proches}.
     *
     * <p>La recherche Haversine est déléguée à {@link ToiletteProximityService};
     * le rayon demandé par le LLM est appliqué comme filtre supplémentaire.</p>
     *
     * @param arguments arguments de l'appel ({@code latitude}, {@code longitude},
     *                  {@code rayon_metres}, {@code table_a_langer})
     * @return un {@link ToolResult} contenant les équipements triés par distance croissante
     * @throws IllegalArgumentException si les coordonnées sont absentes ou hors intervalle
     */
    private ToolResult searchNearToilets(Map<String, Object> arguments) {
        double latitude = requireDouble(arguments, "latitude");
        double longitude = requireDouble(arguments, "longitude");

        if (latitude < -90.0 || latitude > 90.0) {
            throw new IllegalArgumentException("latitude must be between -90 and 90");
        }
        if (longitude < -180.0 || longitude > 180.0) {
            throw new IllegalArgumentException("longitude must be between -180 and 180");
        }

        double radius = optionalDouble(arguments, "rayon_metres", agentConfig.getDefaultRadius());
        if (radius <= 0.0) {
            throw new IllegalArgumentException("rayon_metres must be greater than 0");
        }
        if (radius > agentConfig.getMaxRadius()) {
            throw new IllegalArgumentException("rayon_metres must not exceed " + agentConfig.getMaxRadius());
        }

        GeoPosition position = GeoPosition.builder()
                .latitude(latitude)
                .longitude(longitude)
                .build();

        List<Toilet> nearest = proximityService.getNearestToilets(position);
        List<Toilet> withinRadius = filterWithinRadius(nearest, radius);

        log.debug("Tool '{}' found {} toilets within {} m of ({}, {})",
                ToiletToolRegistry.SEARCH_NEAR_TOILETS, withinRadius.size(), radius, latitude, longitude);

        return ToolResult.ofList(
                ToiletToolRegistry.SEARCH_NEAR_TOILETS,
                "OK",
                withinRadius.stream().limit(agentConfig.getMaxResults()).toList(),
                buildProximityWarning(radius, arguments)
        );
    }

    /**
     * Adaptateur de l'outil {@code get_details_toilette}.
     *
     * @param arguments arguments de l'appel ({@code id})
     * @return un {@link ToolResult} contenant la fiche complète de l'équipement
     * @throws IllegalArgumentException si l'identifiant est absent ou invalide
     * @throws ResourceNotFoundException si aucune toilette ne correspond
     */
    private ToolResult getToiletDetails(Map<String, Object> arguments) {
        long id = requireLong(arguments, "id");
        Toilet toilet = crudService.getToiletById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Toilette not found with id: " + id));

        log.debug("Tool '{}' returned toilet {}", ToiletToolRegistry.GET_TOILET_DETAILS, id);
        return ToolResult.ofOne(ToiletToolRegistry.GET_TOILET_DETAILS, "OK", toilet, null);
    }

    /**
     * Adaptateur de l'outil {@code gerer_equipement_crud}.
     *
     * <p>Le rôle {@code ADMIN} est exigé pour toute écriture : l'agent ne peut
     * jamais contourner le RBAC appliqué aux endpoints REST.</p>
     *
     * @param arguments arguments de l'appel ({@code action}, {@code id}, {@code payload})
     * @return un {@link ToolResult} décrivant l'opération effectuée
     * @throws IllegalArgumentException si l'action est inconnue ou le payload invalide
     * @throws AccessDeniedException si l'utilisateur n'a pas le rôle ADMIN
     * @throws ResourceNotFoundException si la toilette à modifier n'existe pas
     */
    private ToolResult manageEquipment(Map<String, Object> arguments) {
        CrudAction action = CrudAction.from(requireString(arguments, "action"));
        requireAdminRole(action);

        return switch (action) {
            case CREATE -> {
                Toilet payload = parsePayload(arguments);
                validateToilet(payload);
                Toilet created = crudService.addToilet(payload);
                log.info("Agent tool created toilet {}", created.getId());
                yield ToolResult.ofOne(ToiletToolRegistry.MANAGE_EQUIPMENT_CRUD, "CREATED", created, null);
            }
            case UPDATE -> {
                long id = requireLong(arguments, "id");
                Toilet payload = parsePayload(arguments);
                validateToilet(payload);
                crudService.updateToilet(id, payload);
                log.info("Agent tool updated toilet {}", id);
                yield ToolResult.ofOne(
                        ToiletToolRegistry.MANAGE_EQUIPMENT_CRUD,
                        "UPDATED",
                        crudService.getToiletById(id)
                                .orElseThrow(() -> new ResourceNotFoundException("Toilette not found with id: " + id)),
                        null
                );
            }
            case DELETE -> {
                long id = requireLong(arguments, "id");
                crudService.deleteToilet(id);
                log.info("Agent tool deleted toilet {}", id);
                yield new ToolResult(ToiletToolRegistry.MANAGE_EQUIPMENT_CRUD, "DELETED", 0, null, null);
            }
        };
    }

    /**
     * Conserve uniquement les équipements dont la distance calculée est
     * inférieure ou égale au rayon demandé.
     *
     * @param toilets liste des équipements enrichis de leur distance
     * @param radius  rayon en mètres
     * @return la liste filtrée
     */
    private List<Toilet> filterWithinRadius(List<Toilet> toilets, double radius) {
        return toilets.stream()
                .filter(toilet -> toilet.getDistance() != null && toilet.getDistance() <= radius)
                .toList();
    }

    /**
     * Construit l'avertissement destiné au LLM : filtre non supporté ou rayon
     * plafonné par la configuration du service de proximité.
     *
     * @param radius    rayon demandé en mètres
     * @param arguments arguments bruts de l'appel
     * @return l'avertissement, ou {@code null} s'il n'y a rien à signaler
     */
    private String buildProximityWarning(double radius, Map<String, Object> arguments) {
        List<String> warnings = new ArrayList<>();

        if (arguments.get("table_a_langer") != null) {
            warnings.add("Le filtre 'table_a_langer' n'est pas encore supporté par le modèle de données "
                    + "(aucun attribut 'table à langer' dans ToiletDAO) : il a été ignoré.");
        }
        if (radius > toiletteConfig.getMaxDistance()) {
            warnings.add("Le rayon demandé (" + radius + " m) dépasse la distance maximale configurée "
                    + "(app.distance.max = " + toiletteConfig.getMaxDistance() + " m) : "
                    + "seuls les équipements à moins de cette distance sont retournés.");
        }

        return warnings.isEmpty() ? null : String.join(" | ", warnings);
    }


    /**
     * Vérifie que l'utilisateur courant du contexte de sécurité possède le rôle
     * {@code ADMIN}.
     *
     * @param action action CRUD demandée, utilisée dans le message de log
     * @throws AccessDeniedException si le rôle ADMIN est absent
     */
    private void requireAdminRole(CrudAction action) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = authentication != null
                && authentication.getAuthorities().stream()
                .anyMatch(this::hasAdminRole);

        if (!isAdmin) {
            log.warn("Agent tool '{}' rejected for action {}: ADMIN role required",
                    ToiletToolRegistry.MANAGE_EQUIPMENT_CRUD, action);
            throw new AccessDeniedException(
                    "The tool " + ToiletToolRegistry.MANAGE_EQUIPMENT_CRUD + " requires the ADMIN role");
        }
    }

    /**
     * Détermine si une autorité accordée correspond au rôle {@code ADMIN}.
     *
     * <p>Spring Security préfixe les rôles déclarés par {@code ROLE_}
     * ( {@code .roles("ADMIN")} produit {@code ROLE_ADMIN}), alors que les
     * autorités stockées dans la table {@code authorities} sont enregistrées
     * telles quelles. Les deux formes sont donc acceptées.</p>
     *
     * @param authority autorité accordée à l'authentification courante
     * @return {@code true} si l'autorité correspond au rôle ADMIN
     */
    private boolean hasAdminRole(GrantedAuthority authority) {
        String granted = authority.getAuthority();
        return ADMIN_ROLE.equals(granted) || ("ROLE_" + ADMIN_ROLE).equals(granted);
    }

    /**
     * Convertit le {@code payload} fourni par le LLM en objet métier {@link Toilet}.
     *
     * @param arguments arguments de l'appel
     * @return l'objet {@link Toilet} correspondant au payload
     * @throws IllegalArgumentException si le payload est absent ou mal formé
     */
    private Toilet parsePayload(Map<String, Object> arguments) {
        Object payload = arguments.get("payload");
        if (payload == null) {
            throw new IllegalArgumentException("payload is required for CREATE and UPDATE actions");
        }
        try {
            return objectMapper.convertValue(payload, Toilet.class);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("payload is not a valid Toilet object: " + e.getMessage());
        }
    }

    /**
     * Valide les champs indispensables d'un équipement, en cohérence avec la
     * validation appliquée par {@link org.manu.wcpregnant.web.ToilettesApiImpl}.
     *
     * @param toilet l'équipement à valider. Ne doit pas être {@code null}.
     * @throws IllegalArgumentException si un champ obligatoire est manquant
     */
    private void validateToilet(Toilet toilet) {
        Assert.notNull(toilet, "Toilet object cannot be null");
        Assert.hasText(toilet.getLibelle(), "libelle is mandatory");
        Assert.notNull(toilet.getPosition(), "position is mandatory");
        Assert.notNull(toilet.getAdress(), "adress is mandatory");
        Assert.hasText(toilet.getAdress().getCodePostal(), "codePostal is mandatory");
        Assert.notNull(toilet.getGrade(), "grade is mandatory (proprete, accueil, espace)");
        Assert.notNull(toilet.getGrade().getProprete(), "grade.proprete is mandatory");
        Assert.notNull(toilet.getGrade().getAccueil(), "grade.accueil is mandatory");
        Assert.notNull(toilet.getGrade().getEspace(), "grade.espace is mandatory");
    }

    /**
     * Liste des noms d'outils disponibles, utile pour les messages d'erreur.
     *
     * @return la liste des noms déclarés dans le registre
     */
    private List<String> availableToolNames() {
        return registry.getTools().stream()
                .map(ToolDefinition::name)
                .toList();
    }

    /**
     * Extrait un argument numérique obligatoire.
     *
     * @param arguments arguments de l'appel
     * @param key       nom de l'argument
     * @return la valeur en {@code double}
     * @throws IllegalArgumentException si l'argument est absent ou non numérique
     */
    private double requireDouble(Map<String, Object> arguments, String key) {
        Object value = arguments.get(key);
        if (value == null) {
            throw new IllegalArgumentException(key + " is required");
        }
        if (!(value instanceof Number number)) {
            throw new IllegalArgumentException(key + " must be a number");
        }
        return number.doubleValue();
    }

    /**
     * Extrait un argument numérique optionnel avec valeur par défaut.
     *
     * @param arguments arguments de l'appel
     * @param key       nom de l'argument
     * @param fallback  valeur utilisée si l'argument est absent
     * @return la valeur fournie, ou {@code fallback}
     * @throws IllegalArgumentException si la valeur est présente mais non numérique
     */
    private double optionalDouble(Map<String, Object> arguments, String key, double fallback) {
        Object value = arguments.get(key);
        if (value == null) {
            return fallback;
        }
        if (!(value instanceof Number number)) {
            throw new IllegalArgumentException(key + " must be a number");
        }
        return number.doubleValue();
    }

    /**
     * Extrait un argument identifiant obligatoire.
     *
     * @param arguments arguments de l'appel
     * @param key       nom de l'argument
     * @return la valeur en {@code long}
     * @throws IllegalArgumentException si l'argument est absent, non numérique ou négatif
     */
    private long requireLong(Map<String, Object> arguments, String key) {
        Object value = arguments.get(key);
        if (value == null) {
            throw new IllegalArgumentException(key + " is required");
        }
        if (!(value instanceof Number number)) {
            throw new IllegalArgumentException(key + " must be a number");
        }
        if (number.longValue() <= 0) {
            throw new IllegalArgumentException(key + " must be greater than 0");
        }
        return number.longValue();
    }

    /**
     * Extrait un argument texte obligatoire.
     *
     * @param arguments arguments de l'appel
     * @param key       nom de l'argument
     * @return la valeur texte
     * @throws IllegalArgumentException si l'argument est absent
     */
    private String requireString(Map<String, Object> arguments, String key) {
        Object value = arguments.get(key);
        if (value == null) {
            throw new IllegalArgumentException(key + " is required");
        }
        return String.valueOf(value);
    }
}

