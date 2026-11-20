package org.manu.wcpregnant.web;

import java.util.Collections;
import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.Test;
import org.manu.wcpregnant.service.utils.ResourceNotFoundException;
import org.manu.wcpregnant.web.dto.ApiError;
import org.manu.wcpregnant.web.dto.Toilet;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests du conseil global de gestion des erreurs : chaque exception doit être
 * traduite dans le code HTTP annoncé par le contrat OpenAPI, avec un corps
 * {@code ApiError} renseigné.
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    /**
     * Construit une {@code ConstraintViolationException} portant une violation
     * unique dont le chemin et le message sont contrôlés par le test.
     *
     * @param propertyPath chemin de la propriété fautive, ex. "getToilet.id"
     * @param message      message de la contrainte
     * @return l'exception à soumettre au handler
     */
    private ConstraintViolationException singleViolation(String propertyPath, String message) {
        return new ConstraintViolationException(Set.of(violation(propertyPath, message)));
    }

    /**
     * Fabrique une violation de contrainte dont le chemin et le message
     * sont entièrement déterminés par le test.
     *
     * @param propertyPath chemin de la propriété fautive
     * @param message      message de la contrainte
     * @return la violation mockée
     */
    private ConstraintViolation<?> violation(String propertyPath, String message) {
        Path path = mock(Path.class);
        when(path.toString()).thenReturn(propertyPath);
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn(message);
        return violation;
    }

    // ========================================================================
    // handleConstraintViolation
    // ========================================================================

    @Test
    void handleConstraintViolation_shouldReturn400_whenPathParamBelowMinimum() {
        // Arrange
        ConstraintViolationException exception = singleViolation("getToilet.id", "must be greater than or equal to 1");

        // Act
        ResponseEntity<ApiError> response = handler.handleConstraintViolation(exception);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getMessage()).isEqualTo("getToilet.id: must be greater than or equal to 1");
        assertThat(response.getBody().getTimestamp()).isNotNull();
    }

    @Test
    void handleConstraintViolation_shouldSortAndJoin_whenSeveralParamsAreInvalid() {
        // Arrange
        ConstraintViolation<?> sizeViolation = violation("getToilets.size", "must be less than or equal to 100");
        ConstraintViolation<?> pageViolation = violation("getToilets.page", "must be greater than or equal to 0");

        // Act
        ResponseEntity<ApiError> response =
                handler.handleConstraintViolation(new ConstraintViolationException(Set.of(sizeViolation, pageViolation)));

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage())
                .isEqualTo("getToilets.page: must be greater than or equal to 0, "
                        + "getToilets.size: must be less than or equal to 100");
    }

    @Test
    void handleConstraintViolation_shouldFallBackToGenericMessage_whenNoViolation() {
        // Arrange
        ConstraintViolationException exception = new ConstraintViolationException(Collections.emptySet());

        // Act
        ResponseEntity<ApiError> response = handler.handleConstraintViolation(exception);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).isEqualTo("Invalid request parameters");
    }

    // ========================================================================
    // handleBindException (corps de requête invalide)
    // ========================================================================

    @Test
    void handleBindException_shouldReturn400_withFieldNames() {
        // Arrange
        BindException exception = new BindException(new Toilet(), "toilet");
        exception.rejectValue("libelle", "NotBlank", "must not be blank");
        exception.rejectValue("distance", "Min", "must be greater than or equal to 0");

        // Act
        ResponseEntity<ApiError> response = handler.handleBindException(exception);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage())
                .contains("libelle: must not be blank")
                .contains("distance: must be greater than or equal to 0");
    }

    @Test
    void handleBindException_shouldCoverMethodArgumentNotValid_whenRequestBodyIsInvalid() throws Exception {
        // Arrange
        BindException binding = new BindException(new Toilet(), "toilet");
        binding.rejectValue("libelle", "NotNull", "may not be null");
        MethodParameter parameter =
                new MethodParameter(ToilettesApiImpl.class.getMethod("createToilet", Toilet.class), 0);
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(parameter, binding);

        // Act
        ResponseEntity<ApiError> response = handler.handleBindException(exception);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).isEqualTo("libelle: may not be null");
    }

    @Test
    void handleBindException_shouldFallBackToGenericMessage_whenNoFieldError() {
        // Arrange
        BindException exception = new BindException(new Toilet(), "toilet");

        // Act
        ResponseEntity<ApiError> response = handler.handleBindException(exception);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).isEqualTo("Invalid request body");
    }

    // ========================================================================
    // handleIllegalArgument
    // ========================================================================

    @Test
    void handleIllegalArgument_shouldReturn400_withAssertionMessage() {
        // Arrange
        IllegalArgumentException exception = new IllegalArgumentException("latitude and longitude must not be null");

        // Act
        ResponseEntity<ApiError> response = handler.handleIllegalArgument(exception);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getMessage())
                .isEqualTo("latitude and longitude must not be null");
    }

    @Test
    void handleIllegalArgument_shouldFallBackToGenericMessage_whenMessageIsBlank() {
        // Arrange
        IllegalArgumentException exception = new IllegalArgumentException();

        // Act
        ResponseEntity<ApiError> response = handler.handleIllegalArgument(exception);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).isEqualTo("Invalid request");
    }

    @Test
    void handleIllegalArgument_shouldNotLeakStackTrace_inBody() {
        // Arrange
        IllegalArgumentException exception = new IllegalArgumentException("size must be positive");

        // Act
        ResponseEntity<ApiError> response = handler.handleIllegalArgument(exception);

        // Assert
        assertThat(response.getBody().getMessage()).doesNotContain("Exception").doesNotContain("at org.manu");
    }

    // ========================================================================
    // handleResourceNotFound
    // ========================================================================

    @Test
    void handleResourceNotFound_shouldReturn404_withMessage() {
        // Arrange
        ResourceNotFoundException exception = new ResourceNotFoundException("Toilet with id 9999 not found");

        // Act
        ResponseEntity<ApiError> response = handler.handleResourceNotFound(exception);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getStatus()).isEqualTo(404);
        assertThat(response.getBody().getMessage()).isEqualTo("Toilet with id 9999 not found");
    }

    @Test
    void handleResourceNotFound_shouldFallBackToGenericMessage_whenMessageIsBlank() {
        // Arrange
        ResourceNotFoundException exception = new ResourceNotFoundException("");

        // Act
        ResponseEntity<ApiError> response = handler.handleResourceNotFound(exception);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getMessage()).isEqualTo("Resource not found");
    }

    @Test
    void handleResourceNotFound_shouldKeepTimestamp_forTraceability() {
        // Arrange
        ResourceNotFoundException exception = new ResourceNotFoundException("missing grade");

        // Act
        ResponseEntity<ApiError> response = handler.handleResourceNotFound(exception);

        // Assert
        assertThat(response.getBody().getTimestamp()).isNotNull();
    }
}
