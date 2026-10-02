package com.aivideo.auth;

import com.aivideo.common.exception.BadRequestException;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private final JwtService jwtService =
            new JwtService(new AuthProperties("test-secret-at-least-32-chars-long!", 24));

    @Test
    void generateAndParseRoundTrip() {
        String token = jwtService.generateToken("a@b.com", "USER");

        Claims claims = jwtService.parseToken(token);

        assertThat(claims.getSubject()).isEqualTo("a@b.com");
        assertThat(claims.get("role", String.class)).isEqualTo("USER");
    }

    @Test
    void rejectsTamperedToken() {
        String token = jwtService.generateToken("a@b.com", "USER") + "x";

        assertThatThrownBy(() -> jwtService.parseToken(token))
                .isInstanceOf(BadRequestException.class)
                .satisfies(ex -> assertThat(((BadRequestException) ex).getCode())
                        .isEqualTo("INVALID_TOKEN"));
    }

    @Test
    void rejectsShortSecret() {
        assertThatThrownBy(() -> new JwtService(new AuthProperties("short", 24)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void expiredTokenFails() {
        JwtService negative = new JwtService(
                new AuthProperties("test-secret-at-least-32-chars-long!", -1));

        assertThatThrownBy(() -> negative.parseToken(negative.generateToken("a@b.com", "USER")))
                .isInstanceOf(BadRequestException.class);
    }
}
