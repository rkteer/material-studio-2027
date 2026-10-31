package com.materialstudio.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

@Component
public class SessionService {
    public static final String COOKIE_NAME = "material_session";
    private static final long SESSION_SECONDS = 60L * 60 * 24 * 30;

    private final byte[] secret;

    public SessionService(@Value("${app.session.secret}") String secret) {
        if (secret == null || secret.length() < 24) {
            throw new IllegalArgumentException("APP_SESSION_SECRET must contain at least 24 characters");
        }
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    public long maxAgeSeconds() {
        return SESSION_SECONDS;
    }

    public String createToken(String userId) {
        long expiresAt = Instant.now().getEpochSecond() + SESSION_SECONDS;
        String payload = userId + ":" + expiresAt;
        String encodedPayload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        return encodedPayload + "." + sign(encodedPayload);
    }

    public Optional<String> verifyToken(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        String[] parts = token.split("\\.", -1);
        if (parts.length != 2) {
            return Optional.empty();
        }
        byte[] actual = parts[1].getBytes(StandardCharsets.US_ASCII);
        byte[] expected = sign(parts[0]).getBytes(StandardCharsets.US_ASCII);
        if (!MessageDigest.isEqual(actual, expected)) {
            return Optional.empty();
        }

        try {
            String payload = new String(
                    Base64.getUrlDecoder().decode(parts[0]),
                    StandardCharsets.UTF_8
            );
            int separator = payload.lastIndexOf(':');
            if (separator < 1) {
                return Optional.empty();
            }
            long expiresAt = Long.parseLong(payload.substring(separator + 1));
            if (expiresAt <= Instant.now().getEpochSecond()) {
                return Optional.empty();
            }
            return Optional.of(payload.substring(0, separator));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.US_ASCII)));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to create session signature", exception);
        }
    }
}
