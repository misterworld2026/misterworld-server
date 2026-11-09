package com.misterworld.server.security;

import com.misterworld.server.domain.Member;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.*;
import com.nimbusds.jwt.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;

@Component
public class JwtProvider {
    private final byte[] secret;
    private final Clock clock;

    public JwtProvider(@Value("${app.jwt-secret}") String secret, Clock clock) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.clock = clock;
        if (this.secret.length < 32)
            throw new IllegalArgumentException("JWT_SECRET must contain at least 32 UTF-8 bytes");
    }

    public String generateAccessToken(Member member, String sessionId) {
        return generate(member, sessionId, "ACCESS", Duration.ofMinutes(15));
    }

    public String generateRefreshToken(Member member, String sessionId) {
        return generate(member, sessionId, "REFRESH", Duration.ofDays(14));
    }

    private String generate(Member member, String sessionId, String type, Duration ttl) {
        try {
            var claims = new JWTClaimsSet.Builder().issuer("misterworld").subject(member.getId().toString())
                .issueTime(Date.from(clock.instant())).expirationTime(Date.from(clock.instant().plus(ttl)))
                .jwtID(UUID.randomUUID().toString()).claim("sid", sessionId).claim("email", member.getEmail())
                .claim("role", member.getRole().name()).claim("type", type).build();
            var token = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            token.sign(new MACSigner(secret));
            return token.serialize();
        } catch (Exception error) {
            throw new IllegalStateException("Token generation failed");
        }
    }

    public JWTClaimsSet validate(String token, String type) {
        try {
            var jwt = SignedJWT.parse(token);
            if (!JWSAlgorithm.HS256.equals(jwt.getHeader().getAlgorithm()) || !jwt.verify(new MACVerifier(secret)))
                throw new IllegalArgumentException();
            var c = jwt.getJWTClaimsSet();
            if (!"misterworld".equals(c.getIssuer()) || !type.equals(c.getStringClaim("type")) ||
                c.getExpirationTime() == null || !c.getExpirationTime().toInstant().isAfter(clock.instant()) ||
                c.getIssueTime() == null || c.getIssueTime().toInstant().isAfter(clock.instant().plusSeconds(30)) ||
                c.getStringClaim("sid") == null || c.getSubject() == null)
                throw new IllegalArgumentException();
            Long.parseLong(c.getSubject());
            return c;
        } catch (Exception error) {
            throw new com.misterworld.server.common.BusinessException(401, "INVALID_TOKEN", "유효한 인증 토큰이 필요합니다.");
        }
    }
}
