package org.manu.wcpregnant.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.manu.wcpregnant.service.PasswordResetService;
import org.manu.wcpregnant.service.utils.ResourceNotFoundException;
import org.manu.wcpregnant.web.dto.PasswordResetAcknowledgement;
import org.manu.wcpregnant.web.dto.PasswordResetConfirmRequest;
import org.manu.wcpregnant.web.dto.PasswordResetRequest;
import org.manu.wcpregnant.web.dto.PasswordResetResult;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests du contrôleur d'authentification : mapping des erreurs du service
 * de réinitialisation vers les codes HTTP du contrat OpenAPI.
 */
class AuthApiImplTest {

    private AuthApiImpl api;

    @Mock
    private PasswordResetService passwordResetService;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
        api = new AuthApiImpl(passwordResetService);
    }

    // ========================================================================
    // requestPasswordReset
    // ========================================================================

    @Test
    void requestPasswordReset_shouldReturnAcknowledgement_whenServiceSucceeds() {
        // Arrange
        PasswordResetAcknowledgement acknowledgement = new PasswordResetAcknowledgement(
                "Reset code sent to alice@example.com", "alice@example.com", 900, "123456");
        when(passwordResetService.requestCode("alice")).thenReturn(acknowledgement);

        // Act
        ResponseEntity<PasswordResetAcknowledgement> response =
                api.requestPasswordReset(new PasswordResetRequest("alice"));

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(acknowledgement);
        verify(passwordResetService).requestCode("alice");
    }

    @Test
    void requestPasswordReset_shouldReturnBadRequest_whenServiceRejectsLogin() {
        // Arrange
        when(passwordResetService.requestCode("bob")).thenThrow(new IllegalArgumentException("Login is invalid"));

        // Act
        ResponseEntity<PasswordResetAcknowledgement> response =
                api.requestPasswordReset(new PasswordResetRequest("bob"));

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).isEqualTo("Login is invalid");
        assertThat(response.getBody().getDemoCode()).isNull();
    }

    @Test
    void requestPasswordReset_shouldReturnNotFound_whenAccountIsUnknown() {
        // Arrange
        when(passwordResetService.requestCode("ghost"))
                .thenThrow(new ResourceNotFoundException("Account not found: ghost"));

        // Act
        ResponseEntity<PasswordResetAcknowledgement> response =
                api.requestPasswordReset(new PasswordResetRequest("ghost"));

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getMessage()).contains("Account not found");
    }

    // ========================================================================
    // confirmPasswordReset
    // ========================================================================

    @Test
    void confirmPasswordReset_shouldReturnResult_whenCodeIsValid() {
        // Arrange
        PasswordResetResult result = new PasswordResetResult("Password successfully reset", "alice");
        when(passwordResetService.confirmCode("alice", "123456", "newSecret123")).thenReturn(result);

        // Act
        ResponseEntity<PasswordResetResult> response =
                api.confirmPasswordReset(new PasswordResetConfirmRequest("alice", "123456", "newSecret123"));

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getLogin()).isEqualTo("alice");
    }

    @Test
    void confirmPasswordReset_shouldReturnBadRequest_whenCodeIsInvalid() {
        // Arrange
        when(passwordResetService.confirmCode("alice", "000000", "newSecret123"))
                .thenThrow(new IllegalArgumentException("Reset code is invalid"));

        // Act
        ResponseEntity<PasswordResetResult> response =
                api.confirmPasswordReset(new PasswordResetConfirmRequest("alice", "000000", "newSecret123"));

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).isEqualTo("Reset code is invalid");
    }

    @Test
    void confirmPasswordReset_shouldReturnNotFound_whenCodeHasExpired() {
        // Arrange
        when(passwordResetService.confirmCode("alice", "123456", "newSecret123"))
                .thenThrow(new ResourceNotFoundException("Reset code expired for account alice"));

        // Act
        ResponseEntity<PasswordResetResult> response =
                api.confirmPasswordReset(new PasswordResetConfirmRequest("alice", "123456", "newSecret123"));

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getMessage()).contains("expired");
    }
}
