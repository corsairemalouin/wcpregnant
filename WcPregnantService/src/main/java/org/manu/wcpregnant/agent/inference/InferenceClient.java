package org.manu.wcpregnant.agent.inference;

/**
 * Contrat d'un client d'inférence capable de dialoguer avec un modèle de langage.
 *
 * <p>Respecte le principe <strong>DIP</strong> : la couche agent dépend de cette
 * abstraction et non d'une implémentation HTTP concrète, ce qui permet de brancher
 * un autre serveur local (ou un faux client dans les tests) sans modifier
 * {@link org.manu.wcpregnant.agent.AgentService}.</p>
 *
 * @see LocalInferenceClient
 */
public interface InferenceClient {

    /**
     * Envoie la conversation au modèle et retourne sa réponse.
     *
     * @param request la requête d'inférence. Ne doit pas être {@code null}.
     * @return la réponse du modèle, jamais {@code null}
     * @throws InferenceException si le serveur d'inférence est injoignable ou renvoie une erreur
     */
    InferenceResponse chat(InferenceRequest request);
}
