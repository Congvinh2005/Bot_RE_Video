package com.aivideo.auth;

import com.aivideo.auth.dto.AuthResponse;
import com.aivideo.auth.dto.LoginRequest;
import com.aivideo.auth.dto.RegisterRequest;
import com.aivideo.common.exception.BadRequestException;
import com.aivideo.user.Role;
import com.aivideo.user.User;
import com.aivideo.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    UserRepository userRepository;
    @Mock
    PasswordEncoder passwordEncoder;

    AuthService authService;
    JwtService jwtService = new JwtService(
            new AuthProperties("test-secret-at-least-32-chars-long!", 24));

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, jwtService);
    }

    @Test
    void registerHashesPasswordAndReturnsToken() {
        when(userRepository.existsByEmail("a@b.com")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        AuthResponse response = authService.register(new RegisterRequest("a@b.com", "secret123"));

        assertThat(response.token()).isNotBlank();
        assertThat(response.role()).isEqualTo(Role.USER.name());
    }

    @Test
    void registerDuplicateEmailThrows() {
        when(userRepository.existsByEmail("a@b.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(new RegisterRequest("a@b.com", "secret123")))
                .isInstanceOf(BadRequestException.class)
                .satisfies(ex -> assertThat(((BadRequestException) ex).getCode())
                        .isEqualTo("EMAIL_ALREADY_EXISTS"));
    }

    @Test
    void loginSuccessReturnsToken() {
        User user = User.builder().email("a@b.com")
                .passwordHash("hashed").role(Role.USER).build();
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret123", "hashed")).thenReturn(true);

        AuthResponse response = authService.login(new LoginRequest("a@b.com", "secret123"));

        assertThat(response.token()).isNotBlank();
    }

    @Test
    void loginWrongPasswordThrows() {
        User user = User.builder().email("a@b.com")
                .passwordHash("hashed").role(Role.USER).build();
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("a@b.com", "wrong")))
                .isInstanceOf(BadRequestException.class)
                .satisfies(ex -> assertThat(((BadRequestException) ex).getCode())
                        .isEqualTo("INVALID_CREDENTIALS"));
    }
}
