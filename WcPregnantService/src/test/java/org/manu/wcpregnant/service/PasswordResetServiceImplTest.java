package org.manu.wcpregnant.service;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.manu.wcpregnant.bdd.dao.users.UserAccount;
import org.manu.wcpregnant.bdd.repository.UserAccountRepository;
import org.manu.wcpregnant.config.PasswordResetConfig;
import org.manu.wcpregnant.service.impl.PasswordResetServiceImpl;
import org.manu.wcpregnant.service.utils.ResourceNotFoundException;
import org.manu.wcpregnant.web.dto.PasswordResetAcknowledgement;
import org.manu.wcpregnant.web.dto.PasswordResetResult;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests du service de réinitialisation du mot de passe : génération du code,
 * envoi SMTP, expiration et remplacement du mot de passe.
 */
class PasswordResetServiceImplTest {

    private static final String EMAIL = "alice@example.com";

    private PasswordResetServiceImpl service;
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private PasswordResetConfig passwordResetConfig;

    @Mock
    private JavaMailSender mailSender;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
        passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
        service = new PasswordResetServiceImpl(
                userAccountRepository, passwordEncoder, passwordResetConfig, mailSender);
    }

    private UserAccount account() {
        UserAccount account = new UserAccount("alice", passwordEncoder.encode("oldSecret1"), EMAIL, true);
        return account;
    }

    // ========================================================================
    // requestCode
    // ========================================================================

    @Test
    void requestCode_shouldReturnDemoCode_whenMailIsDisabled() {
        // Arrange
        UserAccount account = account();
        when(userAccountRepository.findByUsernameIgnoreCase("alice")).thenReturn(Optional.of(account));
        when(passwordResetConfig.isMailEnabled()).thenReturn(false);
        when(passwordResetConfig.getCodeTtlSeconds()).thenReturn(900);

        // Act
        PasswordResetAcknowledgement acknowledgement = service.requestCode("alice");

        // Assert
        assertThat(acknowledgement.getDemoCode()).matches("\\d{6}");
        assertThat(acknowledgement.getEmail()).isEqualTo(EMAIL);
        assertThat(acknowledgement.getExpiresInSeconds()).isEqualTo(900);
        assertThat(account.getResetCode()).isNotNull();
        assertThat(passwordEncoder.matches(acknowledgement.getDemoCode(), account.getResetCode())).isTrue();
        assertThat(account.getResetCodeExpiresAt()).isAfter(Instant.now());
        verify(userAccountRepository).save(account);
        verifyNoInteractions(mailSender);
    }

    @Test
    void requestCode_shouldSendMailAndHideCode_whenMailIsEnabled() {
        // Arrange
        UserAccount account = account();
        when(userAccountRepository.findByUsernameIgnoreCase("alice")).thenReturn(Optional.of(account));
        when(passwordResetConfig.isMailEnabled()).thenReturn(true);
        when(passwordResetConfig.getCodeTtlSeconds()).thenReturn(900);
        when(passwordResetConfig.getMailFrom()).thenReturn("noreply@example.com");

        // Act
        PasswordResetAcknowledgement acknowledgement = service.requestCode("alice");

        // Assert
        assertThat(acknowledgement.getDemoCode()).isNull();
        assertThat(acknowledgement.getMessage()).contains(EMAIL);
        verify(mailSender).send(ArgumentMatchers.any(SimpleMailMessage.class));
    }

    @Test
    void requestCode_shouldFindByEmail_whenUsernameIsUnknown() {
        // Arrange
        UserAccount account = account();
        when(userAccountRepository.findByUsernameIgnoreCase("ALICE@example.com")).thenReturn(Optional.empty());
        when(userAccountRepository.findByEmailIgnoreCase("ALICE@example.com")).thenReturn(Optional.of(account));
        when(passwordResetConfig.isMailEnabled()).thenReturn(false);
        when(passwordResetConfig.getCodeTtlSeconds()).thenReturn(600);

        // Act
        PasswordResetAcknowledgement acknowledgement = service.requestCode("  ALICE@example.com  ");

        // Assert
        assertThat(acknowledgement.getExpiresInSeconds()).isEqualTo(600);
        assertThat(account.getResetCode()).isNotNull();
    }

    @Test
    void requestCode_shouldThrow_whenLoginIsNull() {
        // Arrange & Act
        NullPointerException exception = assertThrows(NullPointerException.class,
                () -> service.requestCode(null));

        // Assert
        assertThat(exception.getMessage()).contains("login must not be null");
        verify(userAccountRepository, never()).save(ArgumentMatchers.any(UserAccount.class));
    }

    @Test
    void requestCode_shouldThrow_whenLoginIsBlank() {
        // Arrange & Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.requestCode("   "));

        // Assert
        assertThat(exception.getMessage()).contains("login must not be blank");
    }

    @Test
    void requestCode_shouldThrow_whenAccountDoesNotExist() {
        // Arrange
        when(userAccountRepository.findByUsernameIgnoreCase("ghost")).thenReturn(Optional.empty());
        when(userAccountRepository.findByEmailIgnoreCase("ghost")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> service.requestCode("ghost"));
    }

    // ========================================================================
    // confirmCode
    // ========================================================================

    @Test
    void confirmCode_shouldReplacePasswordAndClearCode_whenCodeIsValid() {
        // Arrange
        UserAccount account = account();
        account.setResetCode(passwordEncoder.encode("123456"), Instant.now().plusSeconds(600));
        when(userAccountRepository.findByUsernameIgnoreCase("alice")).thenReturn(Optional.of(account));

        // Act
        PasswordResetResult result = service.confirmCode("alice", "123456", "newSecret123");

        // Assert
        assertThat(result.getLogin()).isEqualTo("alice");
        assertThat(result.getMessage()).isEqualTo("Password successfully reset");
        assertThat(passwordEncoder.matches("newSecret123", account.getPassword())).isTrue();
        assertThat(account.getResetCode()).isNull();
        assertThat(account.getResetCodeExpiresAt()).isNull();
        verify(userAccountRepository).save(account);
    }

    @Test
    void confirmCode_shouldThrow_whenCodeDoesNotMatch() {
        // Arrange
        UserAccount account = account();
        account.setResetCode(passwordEncoder.encode("123456"), Instant.now().plusSeconds(600));
        when(userAccountRepository.findByUsernameIgnoreCase("alice")).thenReturn(Optional.of(account));

        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.confirmCode("alice", "654321", "newSecret123"));

        // Assert
        assertThat(exception.getMessage()).isEqualTo("Reset code is invalid");
        assertThat(passwordEncoder.matches("oldSecret1", account.getPassword())).isTrue();
        verify(userAccountRepository, never()).save(account);
    }

    @Test
    void confirmCode_shouldThrowAndCleanAccount_whenCodeIsExpired() {
        // Arrange
        UserAccount account = account();
        account.setResetCode(passwordEncoder.encode("123456"), Instant.now().minusSeconds(60));
        when(userAccountRepository.findByUsernameIgnoreCase("alice")).thenReturn(Optional.of(account));

        // Act
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> service.confirmCode("alice", "123456", "newSecret123"));

        // Assert
        assertThat(exception.getMessage()).contains("expired");
        assertThat(account.getResetCode()).isNull();
        verify(userAccountRepository).save(account);
    }

    @Test
    void confirmCode_shouldThrow_whenNoCodeIsPending() {
        // Arrange
        UserAccount account = account();
        when(userAccountRepository.findByUsernameIgnoreCase("alice")).thenReturn(Optional.of(account));

        // Act & Assert
        assertThrows(ResourceNotFoundException.class,
                () -> service.confirmCode("alice", "123456", "newSecret123"));
    }

    @Test
    void confirmCode_shouldThrow_whenNewPasswordIsTooShort() {
        // Arrange
        UserAccount account = account();
        account.setResetCode(passwordEncoder.encode("123456"), Instant.now().plusSeconds(600));
        when(userAccountRepository.findByUsernameIgnoreCase("alice")).thenReturn(Optional.of(account));

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> service.confirmCode("alice", "123456", "abc"));
    }

    @Test
    void confirmCode_shouldThrow_whenLoginIsNull() {
        // Arrange & Act
        assertThrows(NullPointerException.class,
                () -> service.confirmCode(null, "123456", "newSecret123"));
    }
}

