package org.manu.wcpregnant.service.utils;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception métier signalant qu'une ressource demandée n'existe pas.
 *
 * <p>Traduite automatiquement en HTTP {@code 404 Not Found} par Spring grâce
 * à l'annotation {@code @ResponseStatus}. Elle est levée par les services
 * (CRUD des toilettes, exécution des outils agentiques) lorsque l'identifiant
 * fourni ne correspond à aucune entité persistée.</p>
 */
@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException { // Changez Exception en RuntimeException
    /**
     * Crée l'exception avec le message destiné au client.
     *
     * @param message description de la ressource introuvable, par exemple
     *                {@code "Toilette not found with id: 42"}
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }
}