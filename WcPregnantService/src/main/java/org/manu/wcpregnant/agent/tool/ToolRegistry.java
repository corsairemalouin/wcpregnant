package org.manu.wcpregnant.agent.tool;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Contrat d'un registre d'outils agentiques.
 *
 * <p>Respecte le principe <strong>DIP</strong> : l'exécuteur et le contrôleur
 * dépendent de cette abstraction et non d'une implémentation concrète, ce qui
 * permet d'ajouter un nouveau domaine d'outils (santé, transport…) sans
 * modifier le code existant (<strong>OCP</strong>).</p>
 *
 * @see ToiletToolRegistry
 * @see ToolExecutor
 */
public interface ToolRegistry {

    /**
     * Retourne l'ensemble des outils déclarés.
     *
     * @return liste immuable des {@link ToolDefinition} du registre
     */
    List<ToolDefinition> getTools();

    /**
     * Recherche un outil par son nom exact.
     *
     * @param name nom de l'outil recherché
     * @return l'outil trouvé, ou {@link Optional#empty()} si le nom est inconnu
     */
    Optional<ToolDefinition> findTool(String name);

    /**
     * Retourne les outils au format enveloppe OpenAI Function Calling, prêt à
     * être injecté dans le champ {@code tools} d'une requête d'inférence.
     *
     * @return liste des enveloppes {@code {"type":"function","function":{...}}}
     */
    List<Map<String, Object>> openAiTools();

    /**
     * Sérialise le registre en JSON (tableau d'outils OpenAI) destiné au prompt
     * système ou à un endpoint de découverte.
     *
     * @return le registre sous forme de chaîne JSON indentée
     */
    String toJson();
}
