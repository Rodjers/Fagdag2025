package com.equinor.onlypikks.controller;

import com.equinor.onlypikks.api.model.AuthTokensResponse;
import com.equinor.onlypikks.api.model.LoginRequest;
import com.equinor.onlypikks.api.model.RefreshTokenRequest;
import com.equinor.onlypikks.auth.MockAuthService;
import com.equinor.onlypikks.exception.UnauthorizedException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.UUID;

@RestController
@RequestMapping(path = "/auth", produces = MediaType.APPLICATION_JSON_VALUE)
public class AuthController {

    private static final Duration ACCESS_TOKEN_TTL = Duration.ofHours(1);
    private final MockAuthService authService;

    public AuthController(MockAuthService authService) {
        this.authService = authService;
    }

    @PostMapping(path = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AuthTokensResponse> login(@RequestBody LoginRequest request) {
        if (!StringUtils.hasText(request.email()) || !StringUtils.hasText(request.password())) {
            throw new UnauthorizedException("Invalid credentials");
        }
        return buildTokenResponse();
    }

    @PostMapping(path = "/refresh", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AuthTokensResponse> refresh(@RequestBody RefreshTokenRequest request) {
        if (!StringUtils.hasText(request.refreshToken())) {
            throw new UnauthorizedException("Invalid or expired refresh token");
        }
        return buildTokenResponse();
    }

    @PostMapping(path = "/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization
    ) {
        authService.resolve(authorization)
                .orElseThrow(() -> new UnauthorizedException("Not authenticated or token missing/invalid"));
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<AuthTokensResponse> buildTokenResponse() {
        String accessToken = "access-" + UUID.randomUUID();
        String refreshToken = "refresh-" + UUID.randomUUID();
        AuthTokensResponse body = new AuthTokensResponse(
                accessToken,
                refreshToken,
                "Bearer",
                ACCESS_TOKEN_TTL.toSeconds()
        );
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Request-Id", UUID.randomUUID().toString());
        return ResponseEntity.ok()
                .headers(headers)
                .body(body);
    }
}
