package org.manu.wcpregnant.web;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.manu.wcpregnant.service.utils.ResourceNotFoundException;
import org.manu.wcpregnant.web.dto.ApiError;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Gestion centralisée des erreurs HTTP de l'API REST.
 *
 * <p>Les interfaces d'API générées à partir du contrat OpenAPI
 * ({@code web/api/*}) sont annotées {@code @Validated} : les contraintes
 * {@code @Min}, {@code @Max} et {@code @Pattern} portées par les paramètres
 * de chemin et de requête lèvent une {@link ConstraintViolationException}
 * avant même d'atteindre l'implémentation. Sans ce conseil, Spring traduit
 * ces violations en {@code 500 Internal Server Error}, ce qui contredit le
 * contrat OpenAPI qui annonce {@code 400 Bad Request} avec un corps
 * {@code ApiError}.</p>
 *
 * <p>Le conseil est placé sous {@code @Order(Ordered.HIGHEST_PRECEDENCE)}
 * afin que ces traductions soient appliquées avant tout autre handler
 * d'exception présent dans l'application.</p>
 */
@Slf4j
@ControllerAdvice(name = "productionGlobalExceptionHandler")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalExceptionHandler {

    /**
     * Traduit une violation de contrainte de paramètre (chemin ou requête)
     * en {@code 400 Bad Request}.
     *
     * <p>C'est le cas typique de {@code GET /toilets/0} ({@code @Min(1)}) ou
     * de {@code GET /toilets?size=1000} ({@code @Max(100)}).</p>
     *
     * @param exception exception levée par le validateur Spring
     * @return {@code 400} avec un {@link ApiError} listant chaque violation
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException exception) {
        String details = describeViolations(exception.getConstraintViolations());
        log.warn("Constraint violation rejected request: {}", details);
        return buildError(HttpStatus.BAD_REQUEST, details);
    }

    /**
     * Traduit une invalidité du corps JSON reçu en {@code 400 Bad Request}.
     *
     * <p>Couvre {@code MethodArgumentNotValidException}, sous-type de
     * {@link BindException}, déclenché par {@code @Valid} sur un
     * {@code @RequestBody}.</p>
     *
     * @param exception exception de liaison portant le résultat de validation
     * @return {@code 400} avec un {@link ApiError} listant les champs fautifs
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiError> handleBindException(BindException exception) {
        String details = describeFieldErrors(exception.getAllErrors());
        log.warn("Invalid request body rejected: {}", details);
        return buildError(HttpStatus.BAD_REQUEST, details);
    }

    /**
     * Traduit une erreur d'argument métier en {@code 400 Bad Request}.
     *
     * <p>Les contrôleurs utilisent {@code Assert.notNull} / {@code Assert.hasText}
     * pour valider les entrées ; sans ce handler ces assertions aboutiraient
     * à un {@code 500}.</p>
     *
     * @param exception exception levée par une assertion d'argument
     * @return {@code 400} avec le message explicatif de l'assertion
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException exception) {
        log.warn("Illegal argument rejected request: {}", exception.getMessage());
        return buildError(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    /**
     * Traduit une ressource introuvable en {@code 404 Not Found}.
     *
     * <p>Permet de retourner un corps {@link ApiError} homogène plutôt que
     * la réponse vide produite par l'annotation {@code @ResponseStatus}
     * de {@link ResourceNotFoundException}.</p>
     *
     * @param exception exception métier signalant l'absence de la ressource
     * @return {@code 404} avec le message de l'exception
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleResourceNotFound(ResourceNotFoundException exception) {
        log.warn("Resource not found: {}", exception.getMessage());
        return buildError(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    /**
     * Construit le corps d'erreur normalisé conforme au schéma {@code ApiError}
     * du contrat OpenAPI.
     *
     * @param status  code HTTP à retourner, non {@code null}
     * @param message message explicatif destiné au client
     * @return une réponse HTTP portant le {@link ApiError} renseigné
     */
    private ResponseEntity<ApiError> buildError(HttpStatus status, String message) {
        ApiError error = new ApiError();
        error.setStatus(status.value());
        error.setMessage((message == null || message.isBlank()) ? fallbackMessage(status) : message);
        error.setTimestamp(OffsetDateTime.now());
        return ResponseEntity.status(status).body(error);
    }

    /**
     * Fournit un libellé générique quand l'exception ne porte aucun message.
     *
     * @param status code HTTP de la réponse, non {@code null}
     * @return un message lisible et stable pour le client
     */
    private String fallbackMessage(HttpStatus status) {
        return switch (status) {
            case BAD_REQUEST -> "Invalid request";
            case NOT_FOUND -> "Resource not found";
            default -> status.name().replace('_', ' ');
        };
    }

    /**
     * Rend lisible l'ensemble des violations de contrainte signalées.
     *
     * @param violations collection des violations, peut être {@code null}
     * @return description "propertyPath: message" triée et séparée par des
     *         virgules, ou un libellé générique si aucune violation n'est détaillée
     */
    private String describeViolations(Set<ConstraintViolation<?>> violations) {
        if (violations == null || violations.isEmpty()) {
            return "Invalid request parameters";
        }
        return violations.stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .sorted()
                .collect(Collectors.joining(", "));
    }

    /**
     * Rend lisible l'ensemble des erreurs de liaison du corps de requête.
     *
     * @param errors liste des erreurs relevées par le résultat de validation
     * @return description "champ: message" triée et séparée par des virgules
     */
    private String describeFieldErrors(List<ObjectError> errors) {
        if (errors == null || errors.isEmpty()) {
            return "Invalid request body";
        }
        return errors.stream()
                .map(this::describeFieldError)
                .sorted()
                .collect(Collectors.joining(", "));
    }

    /**
     * Formate une erreur de liaison unique en "champ: message".
     *
     * @param error erreur relevée, non {@code null}
     * @return la description de l'erreur
     */
    private String describeFieldError(ObjectError error) {
        String field = (error instanceof FieldError fieldError) ? fieldError.getField() : error.getObjectName();
        return field + ": " + error.getDefaultMessage();
    }
}

