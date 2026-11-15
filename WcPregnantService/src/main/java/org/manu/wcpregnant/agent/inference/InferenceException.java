package org.manu.wcpregnant.agent.inference;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception levée lorsque le serveur d'inférence local est injoignable ou répond en erreur.
 *
 * <p>Traduit une panne d'infrastructure (LLM local non démarré, timeout, réponse
 * HTTP 4xx/5xx) et non une erreur métier : le contrôleur la transforme en 503.</p>
 */
@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class InferenceException extends RuntimeException {

    /**
     * @param message description de la panne, en anglais (message destiné à l'API)
     */
    public InferenceException(String message) {
        super(message);
    }

    /**
     * @param message description de la panne
     * @param cause   cause technique (IO, timeout...)
     */
    public InferenceException(String message, Throwable cause) {
        super(message, cause);
    }
}
