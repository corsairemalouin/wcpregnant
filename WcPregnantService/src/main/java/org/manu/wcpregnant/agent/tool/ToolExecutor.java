package org.manu.wcpregnant.agent.tool;

import java.util.List;

/**
 * Contrat d'exécution d'un appel d'outil émis par le LLM.
 *
 * <p>L'implémentation est un <strong>adaptateur</strong> : elle ne contient ni
 * SQL, ni mapping manuel, elle délègue exclusivement aux interfaces de service
 * existantes ({@link org.manu.wcpregnant.service.ToiletteCrudService},
 * {@link org.manu.wcpregnant.service.ToiletteProximityService},
 * {@link org.manu.wcpregnant.service.ToiletteRatingService}).</p>
 *
 * @see ToolCall
 * @see ToolResult
 */
public interface ToolExecutor {

    /**
     * Exécute l'appel d'outil demandé après validation du nom et des arguments.
     *
     * @param call l'appel d'outil à exécuter. Ne doit pas être {@code null}.
     * @return le résultat normalisé de l'opération
     * @throws IllegalArgumentException si l'outil est inconnu ou ses arguments sont invalides
     * @throws org.manu.wcpregnant.service.utils.ResourceNotFoundException si la ressource n'existe pas
     * @throws org.springframework.security.access.AccessDeniedException si le rôle requis n'est pas présent
     */
    ToolResult execute(ToolCall call);
}
