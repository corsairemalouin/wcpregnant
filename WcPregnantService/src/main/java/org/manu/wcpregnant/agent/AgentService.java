package org.manu.wcpregnant.agent;

import org.manu.wcpregnant.web.dto.AgentChatRequest;
import org.manu.wcpregnant.web.dto.AgentChatResponse;

/**
 * Contrat du cerveau agentique de WcPregnant.
 *
 * <p>La couche agent est un <strong>adaptateur</strong> (DIP) : elle orchestre un
 * modèle de langage servi localement par Strata AI et exécute les appels d'outils
 * via {@link org.manu.wcpregnant.agent.tool.ToolExecutor}, qui délègue ensuite aux
 * services métier existants. Elle ne contient ni SQL, ni mapping manuel.</p>
 *
 * @see AgentServiceImpl
 * @see org.manu.wcpregnant.agent.tool.ToolExecutor
 */
public interface AgentService {

    /**
     * Traite une question en langage naturel : le modèle décide quels outils appeler,
     * la couche agent les exécute, puis le modèle formule la réponse finale.
     *
     * @param request la demande de l'utilisateur. Ne doit pas être {@code null}.
     * @return la réponse de l'agent, avec la trace des outils exécutés
     * @throws IllegalArgumentException si la demande est vide
     * @throws org.manu.wcpregnant.agent.inference.InferenceException si le serveur d'inférence local est injoignable
     * @throws org.springframework.security.access.AccessDeniedException si l'utilisateur n'a pas le rôle requis par un outil
     */
    AgentChatResponse chat(AgentChatRequest request);
}
