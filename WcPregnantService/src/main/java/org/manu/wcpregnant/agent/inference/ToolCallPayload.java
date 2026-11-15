package org.manu.wcpregnant.agent.inference;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Enveloppe d'un appel d'outil au format OpenAI Function Calling.
 *
 * <p>Utilisée dans les deux sens : le serveur local renvoie cette structure dans
 * le choix {@code assistant} lorsqu'il veut utiliser un outil, et la couche agent
 * la renvoie dans le message {@code assistant} pour poursuivre la conversation.</p>
 *
 * @param id       identifiant de l'appel d'outil
 * @param type     type d'appel, toujours {@code function}
 * @param function nom de la fonction et arguments JSON
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ToolCallPayload(String id, String type, Function function) {

    /**
     * Nom de l'outil et ses arguments bruts.
     *
     * @param name      nom de l'outil demandé par le modèle
     * @param arguments arguments de l'outil, exprimés comme une chaîne JSON
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Function(String name, String arguments) {
    }

    /**
     * Construit une enveloppe d'appel d'outil prête à être sérialisée.
     *
     * @param id       identifiant de l'appel
     * @param name     nom de l'outil
     * @param arguments arguments JSON déjà sérialisés
     * @return l'enveloppe {@code {"id":...,"type":"function","function":{...}}}
     */
    public static ToolCallPayload of(String id, String name, String arguments) {
        return new ToolCallPayload(id, "function", new Function(name, arguments));
    }

    /**
     * Nom de l'outil appelé, en tolérant les serveurs locaux qui renvoient un format plat.
     *
     * @return le nom de la fonction, ou {@code null} si le modèle n'a pas nommé d'outil
     */
    public String toolName() {
        return function != null ? function.name() : null;
    }

    /**
     * Arguments bruts de l'appel d'outil.
     *
     * @return la chaîne JSON des arguments, ou {@code "{}"} si le modèle n'a pas fourni d'arguments
     */
    public String rawArguments() {
        if (function == null || function.arguments() == null || function.arguments().isBlank()) {
            return "{}";
        }
        return function.arguments();
    }
}
