package org.manu.wcpregnant.agent.tool;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Appel d'outil émis par le LLM (ou par un client) : le nom de la fonction
 * demandée et ses arguments JSON.
 *
 * <p>Les arguments sont copiés défensivement et rendus immuables afin qu'un
 * appel déjà validé ne puisse pas être altéré pendant son exécution.</p>
 *
 * @see ToolExecutor
 */
public record ToolCall(
        String name,
        Map<String, Object> arguments
) {

    /**
     * Constructeur validant : le nom de l'outil est obligatoire, les arguments
     * {@code null} sont normalisés en map vide.
     *
     * @param name      nom de l'outil à exécuter. Ne doit pas être {@code null}.
     * @param arguments arguments JSON de l'appel, {@code null} accepté
     * @throws IllegalArgumentException si {@code name} est vide
     */
    public ToolCall {
        Objects.requireNonNull(name, "name must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        arguments = arguments == null
                ? Map.of()
                : Map.copyOf(new LinkedHashMap<>(arguments));
    }

    /**
     * Retourne la valeur d'un argument.
     *
     * @param key nom de l'argument
     * @return la valeur associée, ou {@code null} si l'argument n'a pas été fourni
     */
    public Object argument(String key) {
        return arguments.get(key);
    }
}
