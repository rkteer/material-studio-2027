package com.materialstudio.controller;

import com.materialstudio.dto.AuthResponse;
import com.materialstudio.dto.DemoLoginRequest;
import com.materialstudio.error.ApiException;
import com.materialstudio.security.SessionService;
import com.materialstudio.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final SessionService sessionService;
    private final boolean secureCookie;

    public AuthController(
            AuthService authService,
            SessionService sessionService,
            @Value("${app.session.secure-cookie}") boolean secureCookie
    ) {
        this.authService = authService;
        this.sessionService = sessionService;
        this.secureCookie = secureCookie;
    }

    @GetMapping("/me")
    AuthResponse me(HttpServletRequest request) {
        return authService.currentState(request);
    }

    @PostMapping("/demo")
    ResponseEntity<AuthResponse> login(@RequestBody DemoLoginRequest request) {
        if (request.userId() == null || request.userId().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "USER_REQUIRED", "请选择演示账号");
        }
        AuthResponse response = authService.loginAs(request.userId());
        ResponseCookie cookie = baseCookie(
                sessionService.createToken(request.userId()),
                true
        );
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(response);
    }

    @PostMapping("/logout")
    ResponseEntity<Void> logout() {
        ResponseCookie cookie = baseCookie("", false);
        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .build();
    }

    private ResponseCookie baseCookie(String value, boolean httpOnly) {
        return ResponseCookie.from(SessionService.COOKIE_NAME, value)
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite("Lax")
                .path("/")
                .maxAge(httpOnly ? Duration.ofSeconds(sessionService.maxAgeSeconds()) : Duration.ZERO)
                .build();
    }
}
