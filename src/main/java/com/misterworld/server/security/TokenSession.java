package com.misterworld.server.security;

import lombok.Getter;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "인증세션")
@Getter
public class TokenSession {
    @Id
    @Column(length = 36)
    private String id;
    @Column(nullable = false)
    private Long memberId;
    @Column(nullable = false, length = 64)
    private String refreshHash;
    @Column(nullable = false)
    private Instant expiresAt;
    @Column(nullable = false)
    private boolean revoked;

    protected TokenSession() {
    }

    public TokenSession(String id, Long memberId, String refreshHash, Instant expiresAt) {
        this.id = id;
        this.memberId = memberId;
        this.refreshHash = refreshHash;
        this.expiresAt = expiresAt;
    }

    public void revoke() {
        revoked = true;
    }
}
