package org.manu.wcpregnant.agent.tool;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.manu.wcpregnant.config.AgentToolConfig;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * Tests du registre d'outils agentiques et des modèles de schéma JSON.
 */
class ToiletToolRegistryTest {

    private ToiletToolRegistry registry;

    @Mock
    private AgentToolConfig config;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
        when(config.getDefaultRadius()).thenReturn(200.0);
        when(config.getMaxRadius()).thenReturn(1000.0);
        registry = new ToiletToolRegistry(new ObjectMapper(), config);
    }

    // ========================================================================
    // getTools
    // ========================================================================

    @Test
    void getTools_shouldReturnThreeRegisteredTools() {
        assertThat(registry.getTools()).hasSize(3);
        assertThat(registry.getTools().stream().map(ToolDefinition::name).toList())
                .containsExactly(
                        ToiletToolRegistry.SEARCH_NEAR_TOILETS,
                        ToiletToolRegistry.GET_TOILET_DETAILS,
                        ToiletToolRegistry.MANAGE_EQUIPMENT_CRUD);
    }

    @Test
    void getTools_shouldBeImmutable() {
        assertThrows(UnsupportedOperationException.class, () -> registry.getTools().add(null));
    }

    @Test
    void constructor_shouldThrow_whenConfigIsNull() {
        assertThrows(NullPointerException.class,
                () -> new ToiletToolRegistry(new ObjectMapper(), null));
    }

    @Test
    void constructor_shouldThrow_whenObjectMapperIsNull() {
        assertThrows(NullPointerException.class,
                () -> new ToiletToolRegistry(null, config));
    }

    // ========================================================================
    // findTool
    // ========================================================================

    @Test
    void findTool_shouldReturnDefinition_whenNameExists() {
        ToolDefinition tool = registry.findTool(ToiletToolRegistry.GET_TOILET_DETAILS).orElseThrow();

        assertThat(tool.description()).contains("fiche complète");
    }

    @Test
    void findTool_shouldReturnEmpty_whenNameIsUnknown() {
        assertThat(registry.findTool("delete_everything")).isEmpty();
    }

    @Test
    void findTool_shouldReturnEmpty_whenNameIsNullOrBlank() {
        assertThat(registry.findTool(null)).isEmpty();
        assertThat(registry.findTool("   ")).isEmpty();
    }

    // ========================================================================
    // openAiTools / toJson
    // ========================================================================

    @Test
    void openAiTools_shouldWrapEachToolInOpenAiEnvelope() {
        List<Map<String, Object>> envelopes = registry.openAiTools();

        assertThat(envelopes).hasSize(3);
        envelopes.forEach(envelope -> {
            assertThat(envelope.get("type")).isEqualTo("function");
            Map<?, ?> function = (Map<?, ?>) envelope.get("function");
            assertThat(function.get("name")).isNotNull();
            assertThat(function.get("description")).isNotNull();
            Map<?, ?> parameters = (Map<?, ?>) function.get("parameters");
            assertThat(parameters.get("type")).isEqualTo("object");
        });
    }
    @Test
    void proximityTool_shouldDeclareRequiredCoordinatesAndOptionalRadius() {
        Map<?, ?> function = (Map<?, ?>) registry.findTool(ToiletToolRegistry.SEARCH_NEAR_TOILETS)
                .orElseThrow()
                .toOpenAiEnvelope()
                .get("function");
        Map<?, ?> parameters = (Map<?, ?>) function.get("parameters");

        assertThat(parameters.get("required")).isEqualTo(List.of("latitude", "longitude"));

        Map<String, ?> properties = (Map<String, ?>) parameters.get("properties");
        assertThat(properties).containsKeys("latitude", "longitude", "rayon_metres", "table_a_langer");
        assertThat(((PropertySchema) properties.get("rayon_metres")).minimum()).isEqualTo(1.0);
        assertThat(((PropertySchema) properties.get("rayon_metres")).maximum()).isEqualTo(1000.0);
    }

    @Test
    void crudTool_shouldDeclareTheAllowedActionsAsEnum() {
        Map<?, ?> function = (Map<?, ?>) registry.findTool(ToiletToolRegistry.MANAGE_EQUIPMENT_CRUD)
                .orElseThrow()
                .toOpenAiEnvelope()
                .get("function");
        Map<?, ?> properties = (Map<?, ?>) ((Map<?, ?>) function.get("parameters")).get("properties");

        assertThat(((PropertySchema) properties.get("action")).enumValues())
                .containsExactly("CREATE", "UPDATE", "DELETE");
    }

    @Test
    void toJson_shouldProduceValidJsonWithEveryToolName() throws Exception {
        String json = registry.toJson();

        List<?> parsed = new ObjectMapper().readValue(json, List.class);
        assertThat(parsed).hasSize(3);
        assertThat(json)
                .contains(ToiletToolRegistry.SEARCH_NEAR_TOILETS)
                .contains(ToiletToolRegistry.GET_TOILET_DETAILS)
                .contains(ToiletToolRegistry.MANAGE_EQUIPMENT_CRUD);
    }

    // ========================================================================
    // Modèles du schéma JSON
    // ========================================================================

    @Test
    void toolDefinition_shouldRejectNullOrBlankName() {
        assertThrows(NullPointerException.class,
                () -> new ToolDefinition(null, "desc", JsonSchema.object(Map.of(), null)));
        assertThrows(IllegalArgumentException.class,
                () -> new ToolDefinition("  ", "desc", JsonSchema.object(Map.of(), null)));
    }

    @Test
    void toolDefinition_shouldRejectBlankDescriptionAndNullParameters() {
        assertThrows(IllegalArgumentException.class,
                () -> new ToolDefinition("tool", "", JsonSchema.object(Map.of(), null)));
        assertThrows(NullPointerException.class,
                () -> new ToolDefinition("tool", "desc", null));
    }

    @Test
    void jsonSchema_shouldCopyPropertiesDefensivelyAndRejectBlankType() {
        Map<String, PropertySchema> properties = new LinkedHashMap<>();
        properties.put("id", PropertySchema.integer("id", 1, null));

        JsonSchema schema = JsonSchema.object(properties, List.of("id"));
        properties.put("extra", PropertySchema.string("extra"));

        assertThat(schema.properties()).doesNotContainKey("extra");
        assertThat(schema.toMap()).containsKeys("type", "properties", "required");
        assertThrows(IllegalArgumentException.class, () -> new JsonSchema("", Map.of(), null));
    }

    @Test
    void jsonSchema_toMap_shouldOmitEmptyCollections() {
        JsonSchema schema = new JsonSchema("object", Map.of(), List.of());

        assertThat(schema.toMap()).hasSize(1);
    }

    @Test
    void propertySchema_shouldRejectInconsistentBoundsAndEmptyEnum() {
        assertThrows(IllegalArgumentException.class,
                () -> PropertySchema.number("rayon", 100.0, 10.0));
        assertThrows(IllegalArgumentException.class,
                () -> PropertySchema.enumerated("action", List.of()));
        assertThrows(NullPointerException.class,
                () -> new PropertySchema(null, "desc", null, null, null));
    }

    @Test
    void propertySchema_factories_shouldReturnExpectedTypes() {
        assertThat(PropertySchema.string("libelle").type()).isEqualTo("string");
        assertThat(PropertySchema.number("lat", -90.0, 90.0).type()).isEqualTo("number");
        assertThat(PropertySchema.integer("id", 1, null).type()).isEqualTo("integer");
        assertThat(PropertySchema.booleanType("flag").type()).isEqualTo("boolean");
        assertThat(PropertySchema.object("payload").type()).isEqualTo("object");
    }

    @Test
    void toolCall_shouldRejectBlankNameAndDefaultArgumentsToEmptyMap() {
        assertThrows(IllegalArgumentException.class, () -> new ToolCall("", Map.of()));
        assertThrows(NullPointerException.class, () -> new ToolCall(null, Map.of()));

        ToolCall call = new ToolCall("get_details_toilette", null);
        assertThat(call.arguments()).isEmpty();
        assertThat(call.argument("id")).isNull();
    }

    @Test
    void toolCall_shouldCopyArgumentsDefensively() {
        Map<String, Object> arguments = new LinkedHashMap<>();
        arguments.put("id", 1L);

        ToolCall call = new ToolCall("get_details_toilette", arguments);
        arguments.put("id", 2L);

        assertThat(call.argument("id")).isEqualTo(1L);
    }

    @Test
    void toolResult_shouldComputeCount_andRejectInvalidValues() {
        ToolResult listResult = ToolResult.ofList("search_toilettes_proches", "OK", List.of(1, 2, 3), null);
        assertThat(listResult.count()).isEqualTo(3);

        ToolResult oneResult = ToolResult.ofOne("get_details_toilette", "OK", "toilet", "warning");
        assertThat(oneResult.count()).isEqualTo(1);

        assertThrows(IllegalArgumentException.class,
                () -> new ToolResult("tool", "OK", -1, null, null));
        assertThrows(NullPointerException.class,
                () -> ToolResult.ofOne("tool", "OK", null, null));
    }

    @Test
    void crudAction_from_shouldResolveCaseInsensitivelyAndRejectUnknownValues() {
        assertThat(CrudAction.from(" create ")).isEqualTo(CrudAction.CREATE);
        assertThat(CrudAction.allowedValues()).containsExactly("CREATE", "UPDATE", "DELETE");
        assertThrows(IllegalArgumentException.class, () -> CrudAction.from("RENAME"));
        assertThrows(IllegalArgumentException.class, () -> CrudAction.from(null));
    }
}

