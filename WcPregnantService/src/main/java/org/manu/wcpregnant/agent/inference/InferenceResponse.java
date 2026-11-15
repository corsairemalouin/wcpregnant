package org.manu.wcpregnant.agent.inference;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Réponse d'un serveur d'inférence compatible OpenAI.
 *
 * @param model   nom du modèle ayant répondu, si le serveur le renvoie
 * @param choices liste des choix produits par le modèle
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record InferenceResponse(String model, List<InferenceChoice> choices) {

    /**
     * Un choix de réponse du modèle.
     *
     * @param message      message produit par le modèle
     * @param finishReason raison d'arrêt (<code>stop</code>, <code>tool_calls</code>...)
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record InferenceChoice(
            @JsonProperty("message") InferenceMessage message,
            @JsonProperty("finish_reason") String finishReason) {
    }

    /**
     * Le message du modèle : soit une réponse textuelle, soit une demande d'appel d'outils.
     *
     * @param role      rôle du message, normalement {@code assistant}
     * @param content   contenu textuel de la réponse
     * @param toolCalls appels d'outils demandés par le modèle
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record InferenceMessage(String role, String content,
            @JsonProperty("tool_calls") List<ToolCallPayload> toolCalls) {
    }

    /**
     * Retourne le premier message de la réponse, ou {@code null} si le serveur n'a produit aucun choix.
     *
     * @return le message du modèle, {@code null} si la réponse est vide
     */
    public InferenceMessage firstMessage() {
        if (choices == null || choices.isEmpty()) {
            return null;
        }
        return choices.get(0).message();
    }

    /**
     * Les appels d'outils demandés par le modèle.
     *
     * @return la liste des outils à exécuter, vide si le modèle a répondu directement
     */
    public List<ToolCallPayload> toolCalls() {
        InferenceMessage message = firstMessage();
        if (message == null || message.toolCalls() == null) {
            return List.of();
        }
        return message.toolCalls();
    }

    /**
     * Le contenu textuel de la réponse du modèle.
     *
     * @return la réponse textuelle, ou {@code null} si le modèle n'a produit que des appels d'outils
     */
    public String content() {
        InferenceMessage message = firstMessage();
        return message == null ? null : message.content();
    }

    /**
     * Indique si la réponse doit déclencher une exécution d'outils côté serveur.
     *
     * @return {@code true} si au moins un appel d'outil est présent
     */
    public boolean hasToolCalls() {
        return !toolCalls().isEmpty();
    }

    /**
     * Raison d'arrêt du premier choix, utile pour les logs et le diagnostic
     * d'une conversation agentique (<code>stop</code>, <code>tool_calls</code>...).
     *
     * @return la raison d'arrêt, ou {@code null} si le serveur n'a renvoyé aucun choix
     */
    public String finishReason() {
        if (choices == null || choices.isEmpty()) {
            return null;
        }
        return choices.get(0).finishReason();
    }
}
