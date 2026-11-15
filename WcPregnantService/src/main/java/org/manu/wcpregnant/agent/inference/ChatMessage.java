package org.manu.wcpregnant.agent.inference;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Message d'une conversation envoyée au serveur d'inférence local.
 *
 * <p>Le format est celui de l'API compatible OpenAI {@code /chat/completions} :
 * un rôle (<code>system</code>, <code>user</code>, <code>assistant</code>,
 * <code>tool</code>), un contenu textuel et, pour le rôle assistant, la liste
 * des appels d'outils que le modèle souhaite exécuter.</p>
 *
 * @param role       rôle du message dans la conversation
 * @param content    contenu textuel du message, peut être {@code null}
 * @param toolCalls  appels d'outils émis par le modèle, {@code null} s'il n'y en a pas
 * @param toolCallId identifiant de l'appel d'outil auquel ce message répond (rôle {@code tool})
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatMessage(
        String role,
        String content,
        @JsonProperty("tool_calls") List<ToolCallPayload> toolCalls,
        @JsonProperty("tool_call_id") String toolCallId) {

    /**
     * Construit le message de contexte qui décrit au modèle sa mission et les outils disponibles.
     *
     * @param content instruction système
     * @return un message de rôle {@code system}
     */
    public static ChatMessage system(String content) {
        return new ChatMessage("system", content, null, null);
    }

    /**
     * Construit le message de l'utilisateur.
     *
     * @param content question ou instruction en langage naturel
     * @return un message de rôle {@code user}
     */
    public static ChatMessage user(String content) {
        return new ChatMessage("user", content, null, null);
    }

    /**
     * Construit le message du modèle, éventuellement porteur d'appels d'outils.
     *
     * @param content  contenu textuel produit par le modèle, peut être {@code null}
     * @param toolCalls appels d'outils demandés par le modèle, peut être {@code null}
     * @return un message de rôle {@code assistant}
     */
    public static ChatMessage assistant(String content, List<ToolCallPayload> toolCalls) {
        return new ChatMessage("assistant", content, toolCalls, null);
    }

    /**
     * Construit le message renvoyant au modèle le résultat d'un outil exécuté côté serveur.
     *
     * @param toolCallId identifiant de l'appel d'outil concerné
     * @param content    résultat de l'outil sérialisé en JSON
     * @return un message de rôle {@code tool}
     */
    public static ChatMessage tool(String toolCallId, String content) {
        return new ChatMessage("tool", content, null, toolCallId);
    }
}
