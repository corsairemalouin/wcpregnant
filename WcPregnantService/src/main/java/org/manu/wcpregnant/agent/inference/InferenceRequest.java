package org.manu.wcpregnant.agent.inference;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.Map;

/**
 * Requête envoyée au serveur d'inférence local (API compatible OpenAI).
 *
 * @param model   nom du modèle servi localement par Strata AI
 * @param messages conversation complète, du prompt système jusqu'au dernier résultat d'outil
 * @param tools   outils exposés au modèle au format OpenAI Function Calling
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record InferenceRequest(String model, List<ChatMessage> messages, List<Map<String, Object>> tools) {
}
