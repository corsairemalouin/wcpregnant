package org.manu.wcpregnant.agent.tool;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.manu.wcpregnant.config.AgentToolConfig;
import org.manu.wcpregnant.config.ToiletteServiceConfig;
import org.manu.wcpregnant.service.ToiletteCrudService;
import org.manu.wcpregnant.service.ToiletteProximityService;
import org.manu.wcpregnant.service.utils.ResourceNotFoundException;
import org.manu.wcpregnant.web.dto.GeoPosition;
import org.manu.wcpregnant.web.dto.Toilet;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests de l'exécuteur d'outils agentiques : validation des arguments,
 * délégation aux services existants et respect du RBAC.
 */
class ToiletToolExecutorTest {

    private ToiletToolExecutor executor;
    private ToiletToolRegistry registry;

    private final ToiletteProximityService proximityService = mock(ToiletteProximityService.class);
    private final ToiletteCrudService crudService = mock(ToiletteCrudService.class);
    private final AgentToolConfig agentConfig = mock(AgentToolConfig.class);
    private final ToiletteServiceConfig toiletteConfig = mock(ToiletteServiceConfig.class);

    @BeforeEach
    void init() {
        when(agentConfig.getDefaultRadius()).thenReturn(200.0);
        when(agentConfig.getMaxRadius()).thenReturn(1000.0);
        when(agentConfig.getMaxResults()).thenReturn(10);
        when(toiletteConfig.getMaxDistance()).thenReturn(50.0);

        ObjectMapper objectMapper = new ObjectMapper();
        registry = new ToiletToolRegistry(objectMapper, agentConfig);
        executor = new ToiletToolExecutor(registry, proximityService, crudService,
                agentConfig, toiletteConfig, objectMapper);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    /**
     * Contexte de sécurité simulant un utilisateur disposant des rôles donnés.
     *
     * @param roles rôles à attribuer à l'utilisateur simulé
     */
    private void authenticate(String... roles) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("agent", null,
                        List.of(roles).stream().map(SimpleGrantedAuthority::new).toList()));
    }

    /**
     * Fabrique une toilette enrichie de sa distance.
     *
     * @param id       identifiant de la toilette
     * @param distance distance en mètres par rapport au point de recherche
     * @return un objet {@link Toilet} prêt pour les tests
     */
    private Toilet toiletWithDistance(long id, double distance) {
        Toilet toilet = new Toilet();
        toilet.setId(id);
        toilet.setLibelle("Toilette " + id);
        toilet.setDistance(distance);
        return toilet;
    }

    // ========================================================================
    // search_toilettes_proches
    // ========================================================================

    @Test
    void execute_shouldDelegateToProximityService_whenSearchToolIsCalled() {
        // Arrange
        when(proximityService.getNearestToilets(any())).thenReturn(List.of(toiletWithDistance(1L, 30.0)));

        // Act
        ToolResult result = executor.execute(new ToolCall(
                ToiletToolRegistry.SEARCH_NEAR_TOILETS,
                Map.of("latitude", 48.6073, "longitude", -1.8898, "rayon_metres", 30.0)));

        // Assert
        assertThat(result.tool()).isEqualTo(ToiletToolRegistry.SEARCH_NEAR_TOILETS);
        assertThat(result.status()).isEqualTo("OK");
        assertThat(result.count()).isEqualTo(1);
        assertThat(result.warning()).isNull();
        verify(proximityService).getNearestToilets(any(GeoPosition.class));
    }

    @Test
    void execute_shouldFilterResults_whenRadiusIsSmallerThanDistance() {
        // Arrange
        when(proximityService.getNearestToilets(any())).thenReturn(List.of(
                toiletWithDistance(1L, 20.0),
                toiletWithDistance(2L, 45.0)));

        // Act
        ToolResult result = executor.execute(new ToolCall(
                ToiletToolRegistry.SEARCH_NEAR_TOILETS,
                Map.of("latitude", 48.6, "longitude", -1.9, "rayon_metres", 30.0)));

        // Assert
        assertThat(result.count()).isEqualTo(1);
    }

    @Test
    void execute_shouldApplyDefaultRadius_whenRayonIsAbsent() {
        // Arrange
        when(proximityService.getNearestToilets(any())).thenReturn(List.of(toiletWithDistance(1L, 40.0)));

        // Act
        ToolResult result = executor.execute(new ToolCall(
                ToiletToolRegistry.SEARCH_NEAR_TOILETS,
                Map.of("latitude", 48.6, "longitude", -1.9)));

        // Assert
        assertThat(result.count()).isEqualTo(1);
    }
    @Test
    void execute_shouldWarn_whenTableALangerFilterIsRequested() {
        // Arrange
        when(proximityService.getNearestToilets(any())).thenReturn(List.of(toiletWithDistance(1L, 10.0)));

        // Act
        ToolResult result = executor.execute(new ToolCall(
                ToiletToolRegistry.SEARCH_NEAR_TOILETS,
                Map.of("latitude", 48.6, "longitude", -1.9, "table_a_langer", true)));

        // Assert
        assertThat(result.warning()).contains("table_a_langer");
    }

    @Test
    void execute_shouldWarn_whenRadiusExceedsConfiguredMaxDistance() {
        // Arrange
        when(proximityService.getNearestToilets(any())).thenReturn(List.of());

        // Act
        ToolResult result = executor.execute(new ToolCall(
                ToiletToolRegistry.SEARCH_NEAR_TOILETS,
                Map.of("latitude", 48.6, "longitude", -1.9, "rayon_metres", 800.0)));

        // Assert
        assertThat(result.warning()).contains("app.distance.max");
    }

    @Test
    void execute_shouldThrow_whenCoordinatesAreMissingOrInvalid() {
        // Arrange / Act / Assert
        assertThrows(IllegalArgumentException.class,
                () -> executor.execute(new ToolCall(ToiletToolRegistry.SEARCH_NEAR_TOILETS, Map.of("latitude", 48.6))));
        assertThrows(IllegalArgumentException.class,
                () -> executor.execute(new ToolCall(ToiletToolRegistry.SEARCH_NEAR_TOILETS,
                        Map.of("latitude", 200.0, "longitude", -1.9))));
        assertThrows(IllegalArgumentException.class,
                () -> executor.execute(new ToolCall(ToiletToolRegistry.SEARCH_NEAR_TOILETS,
                        Map.of("latitude", "48.6", "longitude", -1.9))));
        assertThrows(IllegalArgumentException.class,
                () -> executor.execute(new ToolCall(ToiletToolRegistry.SEARCH_NEAR_TOILETS,
                        Map.of("latitude", 48.6, "longitude", -1.9, "rayon_metres", -5.0))));

        verify(proximityService, never()).getNearestToilets(any());
    }

    @Test
    void execute_shouldThrow_whenRadiusExceedsRegistryMaximum() {
        // Arrange / Act / Assert
        assertThrows(IllegalArgumentException.class,
                () -> executor.execute(new ToolCall(ToiletToolRegistry.SEARCH_NEAR_TOILETS,
                        Map.of("latitude", 48.6, "longitude", -1.9, "rayon_metres", 99999.0))));
    }

    @Test
    void execute_shouldThrow_whenToolIsUnknown() {
        // Arrange / Act / Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> executor.execute(new ToolCall("hack_the_database", Map.of())));

        assertThat(exception.getMessage()).contains("Unknown tool");
    }

    @Test
    void execute_shouldThrow_whenCallIsNull() {
        // Arrange / Act / Assert
        assertThrows(NullPointerException.class, () -> executor.execute(null));
    }

    // ========================================================================
    // get_details_toilette
    // ========================================================================

    @Test
    void execute_shouldReturnTheToilet_whenDetailsToolIsCalled() {
        // Arrange
        Toilet toilet = toiletWithDistance(7L, 0.0);
        when(crudService.getToiletById(7L)).thenReturn(Optional.of(toilet));

        // Act
        ToolResult result = executor.execute(new ToolCall(
                ToiletToolRegistry.GET_TOILET_DETAILS, Map.of("id", 7)));

        // Assert
        assertThat(result.status()).isEqualTo("OK");
        assertThat(result.count()).isEqualTo(1);
        assertThat(result.data()).isEqualTo(toilet);
    }

    @Test
    void execute_shouldThrowNotFound_whenToiletDoesNotExist() {
        // Arrange
        when(crudService.getToiletById(99L)).thenReturn(Optional.empty());

        // Arrange / Act / Assert
        assertThrows(ResourceNotFoundException.class,
                () -> executor.execute(new ToolCall(ToiletToolRegistry.GET_TOILET_DETAILS, Map.of("id", 99))));
    }

    @Test
    void execute_shouldThrow_whenIdIsMissingOrNotNumeric() {
        // Arrange / Act / Assert
        assertThrows(IllegalArgumentException.class,
                () -> executor.execute(new ToolCall(ToiletToolRegistry.GET_TOILET_DETAILS, Map.of())));
        assertThrows(IllegalArgumentException.class,
                () -> executor.execute(new ToolCall(ToiletToolRegistry.GET_TOILET_DETAILS, Map.of("id", -1))));

        verify(crudService, never()).getToiletById(any());
    }

    // ========================================================================
    // gerer_equipement_crud
    // ========================================================================

    /**
     * Payload valide pour la création / mise à jour d'un équipement.
     *
     * @return un map représentant un objet {@link Toilet} conforme au contrat OpenAPI
     */
    private Map<String, Object> validPayload() {
        return Map.of(
                "libelle", "Toilette du marché",
                "adress", Map.of("voie", "Rue du Marché", "codePostal", "72100", "ville", "Le Mans"),
                "position", Map.of("latitude", 47.9945, "longitude", 0.1946),
                "grade", Map.of("proprete", 8, "accueil", 7, "espace", 6)
        );
    }

    @Test
    void execute_shouldCreateToilet_whenAdminCallsCreate() {
        // Arrange
        authenticate("USER", "ADMIN");
        Toilet created = toiletWithDistance(42L, 0.0);
        when(crudService.addToilet(any(Toilet.class))).thenReturn(created);

        // Act
        ToolResult result = executor.execute(new ToolCall(
                ToiletToolRegistry.MANAGE_EQUIPMENT_CRUD,
                Map.of("action", "CREATE", "payload", validPayload())));

        // Assert
        assertThat(result.status()).isEqualTo("CREATED");
        assertThat(result.count()).isEqualTo(1);
        verify(crudService).addToilet(any(Toilet.class));
    }

    @Test
    void execute_shouldThrowAccessDenied_whenUserHasNoAdminRole() {
        // Arrange
        authenticate("USER");

        // Act / Assert
        assertThrows(AccessDeniedException.class,
                () -> executor.execute(new ToolCall(
                        ToiletToolRegistry.MANAGE_EQUIPMENT_CRUD,
                        Map.of("action", "CREATE", "payload", validPayload()))));

        verify(crudService, never()).addToilet(any());
    }

    @Test
    void execute_shouldThrowAccessDenied_whenNobodyIsAuthenticated() {
        // Arrange / Act / Assert
        assertThrows(AccessDeniedException.class,
                () -> executor.execute(new ToolCall(
                        ToiletToolRegistry.MANAGE_EQUIPMENT_CRUD,
                        Map.of("action", "DELETE", "id", 1))));
    }

    @Test
    void execute_shouldAcceptAdminRole_whenAuthorityUsesSpringRolePrefix() {
        // Arrange : Spring Security préfixe les rôles déclarés par "ROLE_"
        authenticate("USER", "ROLE_ADMIN");
        Toilet created = toiletWithDistance(42L, 0.0);
        when(crudService.addToilet(any(Toilet.class))).thenReturn(created);

        // Act
        ToolResult result = executor.execute(new ToolCall(
                ToiletToolRegistry.MANAGE_EQUIPMENT_CRUD,
                Map.of("action", "CREATE", "payload", validPayload())));

        // Assert
        assertThat(result.status()).isEqualTo("CREATED");
        assertThat(result.count()).isEqualTo(1);
        assertThat(result.data()).isEqualTo(created);
        verify(crudService).addToilet(any(Toilet.class));
    }

    @Test
    void execute_shouldUpdateToilet_whenAdminCallsUpdate() {
        // Arrange
        authenticate("ADMIN");
        when(crudService.getToiletById(3L)).thenReturn(Optional.of(toiletWithDistance(3L, 12.0)));

        // Act
        ToolResult result = executor.execute(new ToolCall(
                ToiletToolRegistry.MANAGE_EQUIPMENT_CRUD,
                Map.of("action", "UPDATE", "id", 3, "payload", validPayload())));

        // Assert
        assertThat(result.status()).isEqualTo("UPDATED");
        verify(crudService).updateToilet(eq(3L), any(Toilet.class));
    }

    @Test
    void execute_shouldDeleteToilet_whenAdminCallsDelete() {
        // Arrange
        authenticate("ADMIN");

        // Act
        ToolResult result = executor.execute(new ToolCall(
                ToiletToolRegistry.MANAGE_EQUIPMENT_CRUD,
                Map.of("action", "DELETE", "id", 5)));

        // Assert
        assertThat(result.status()).isEqualTo("DELETED");
        assertThat(result.count()).isZero();
        verify(crudService).deleteToilet(5L);
    }

    @Test
    void execute_shouldThrow_whenPayloadOrIdIsMissingForWriteActions() {
        // Arrange
        authenticate("ADMIN");

        // Act / Assert
        assertThrows(IllegalArgumentException.class,
                () -> executor.execute(new ToolCall(
                        ToiletToolRegistry.MANAGE_EQUIPMENT_CRUD, Map.of("action", "CREATE"))));
        assertThrows(IllegalArgumentException.class,
                () -> executor.execute(new ToolCall(
                        ToiletToolRegistry.MANAGE_EQUIPMENT_CRUD, Map.of("action", "DELETE"))));
    }

    @Test
    void execute_shouldThrow_whenGradeIsMissingOrIncomplete() {
        // Arrange
        authenticate("ADMIN");
        Map<String, Object> payloadWithoutGrade = Map.of(
                "libelle", "Toilette sans note",
                "adress", Map.of("voie", "Rue Neuve", "codePostal", "72000", "ville", "Le Mans"),
                "position", Map.of("latitude", 48.0, "longitude", 0.2)
        );
        Map<String, Object> payloadWithPartialGrade = Map.of(
                "libelle", "Toilette mal notée",
                "adress", Map.of("voie", "Rue Neuve", "codePostal", "72000", "ville", "Le Mans"),
                "position", Map.of("latitude", 48.0, "longitude", 0.2),
                "grade", Map.of("proprete", 5)
        );

        // Act / Assert
        assertThrows(IllegalArgumentException.class,
                () -> executor.execute(new ToolCall(
                        ToiletToolRegistry.MANAGE_EQUIPMENT_CRUD,
                        Map.of("action", "CREATE", "payload", payloadWithoutGrade))));
        assertThrows(IllegalArgumentException.class,
                () -> executor.execute(new ToolCall(
                        ToiletToolRegistry.MANAGE_EQUIPMENT_CRUD,
                        Map.of("action", "CREATE", "payload", payloadWithPartialGrade))));

        verify(crudService, never()).addToilet(any());
    }

    @Test
    void execute_shouldThrow_whenActionIsUnknown() {
        // Arrange
        authenticate("ADMIN");

        // Act / Assert
        assertThrows(IllegalArgumentException.class,
                () -> executor.execute(new ToolCall(
                        ToiletToolRegistry.MANAGE_EQUIPMENT_CRUD, Map.of("action", "RENAME"))));

        verify(crudService, never()).addToilet(any());
    }

    @Test
    void execute_shouldThrow_whenPayloadIsNotAToilet() {
        // Arrange
        authenticate("ADMIN");

        // Act / Assert
        assertThrows(IllegalArgumentException.class,
                () -> executor.execute(new ToolCall(
                        ToiletToolRegistry.MANAGE_EQUIPMENT_CRUD,
                        Map.of("action", "CREATE", "payload", Map.of("libelle", "Sans adresse")))));
    }
}

