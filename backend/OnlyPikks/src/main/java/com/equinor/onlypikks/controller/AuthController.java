package com.equinor.onlypikks.controller;

import com.equinor.onlypikks.api.model.AuthTokensResponse;
import com.equinor.onlypikks.api.model.LoginRequest;
import com.equinor.onlypikks.api.model.RefreshTokenRequest;
import com.equinor.onlypikks.api.model.RegisterRequest;
import com.equinor.onlypikks.auth.Auth0AuthenticationClient;
import com.equinor.onlypikks.exception.UnauthorizedException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@CrossOrigin
@RequestMapping(path = "/auth", produces = MediaType.APPLICATION_JSON_VALUE)
public class AuthController {

    private final Auth0AuthenticationClient authenticationClient;

    public AuthController(Auth0AuthenticationClient authenticationClient) {
        this.authenticationClient = authenticationClient;
    }

    @PostMapping(path = "/register", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> register(@RequestBody RegisterRequest request) {
        if (!StringUtils.hasText(request.email()) || !StringUtils.hasText(request.password())) {
            throw new IllegalArgumentException("Email and password must be provided");
        }
        authenticationClient.register(request.email(), request.password());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping(path = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AuthTokensResponse> login(@RequestBody LoginRequest request) {
        if (!StringUtils.hasText(request.email()) || !StringUtils.hasText(request.password())) {
            throw new UnauthorizedException("Invalid credentials");
        }
        AuthTokensResponse tokens = authenticationClient.login(request.email(), request.password());
        return buildTokenResponse(tokens);
    }

    @PostMapping(path = "/refresh", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AuthTokensResponse> refresh(@RequestBody RefreshTokenRequest request) {
        if (!StringUtils.hasText(request.refreshToken())) {
            throw new UnauthorizedException("Invalid or expired refresh token");
        }
        AuthTokensResponse tokens = authenticationClient.refresh(request.refreshToken());
        return buildTokenResponse(tokens);
    }

    @PostMapping(path = "/logout", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> logout(@RequestBody RefreshTokenRequest request) {
        if (!StringUtils.hasText(request.refreshToken())) {
            throw new UnauthorizedException("Invalid or expired refresh token");
        }
        authenticationClient.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<AuthTokensResponse> buildTokenResponse(AuthTokensResponse body) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Request-Id", UUID.randomUUID().toString());
        return ResponseEntity.ok()
                .headers(headers)
                .body(body);
    }
}
