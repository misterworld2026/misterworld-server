package com.misterworld.server.auth.service;

import com.misterworld.server.auth.dto.LoginRequest;
import com.misterworld.server.auth.dto.LoginResponse;
import com.misterworld.server.auth.dto.SignUpRequest;
import com.misterworld.server.auth.dto.SignUpResponse;
import com.misterworld.server.common.BusinessException;
import com.misterworld.server.member.entity.Member;
import com.misterworld.server.member.entity.Role;
import com.misterworld.server.member.repository.MemberRepository;
import com.misterworld.server.security.JwtProvider;
import com.misterworld.server.security.TokenSession;
import com.misterworld.server.security.TokenSessionRepository;


import lombok.RequiredArgsConstructor;

import com.misterworld.server.security.*;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;

import static com.misterworld.server.common.BusinessException.require;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final MemberRepository members;
    private final PasswordEncoder encoder;
    private final JwtProvider jwt;
    private final TokenSessionRepository sessions;
    private final Clock clock;

    @Transactional
    public SignUpResponse signup(SignUpRequest request) {
        validatePasswordConfirmation(request);
        String email = request.email().strip().toLowerCase(Locale.ROOT);
        if (members.existsByEmail(email))
            throw new BusinessException(409, "DUPLICATE_EMAIL", "이미 가입한 이메일입니다.");
        require(request.password().getBytes(StandardCharsets.UTF_8).length <= 72, "비밀번호는 UTF-8 72바이트 이하여야 합니다.");
        var member = new Member();
        member.setEmail(email);
        member.setPassword(encoder.encode(request.password()));
        member.setName(request.name());
        member.setAddress(request.address());
        member.setPhoneNumber(request.phoneNumber());
        member.setTravelCount(0);
        member.setRole(Role.MEMBER);
        members.saveAndFlush(member);
        return new SignUpResponse(member.getId(), member.getEmail(), member.getRole());
    }

    public void validatePasswordConfirmation(SignUpRequest request) {
        require(request.password().equals(request.passwordConfirmation()), "비밀번호 확인이 일치하지 않습니다.");
    }

    public SignUpResponse createStaffAccount(SignUpRequest request) {
        throw BusinessException.policy();
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        var member = members.findByEmail(request.email().strip().toLowerCase(Locale.ROOT))
            .orElseThrow(AuthService::unauthorized);
        if (!encoder.matches(request.password(), member.getPassword()))
            throw unauthorized();
        return issue(member);
    }

    private LoginResponse issue(Member member) {
        String id = UUID.randomUUID().toString();
        String refreshToken = jwt.generateRefreshToken(member, id);
        sessions.save(
            new TokenSession(id, member.getId(), hash(refreshToken), clock.instant().plus(Duration.ofDays(14))));
        return new LoginResponse(jwt.generateAccessToken(member, id), refreshToken, member.getRole());
    }

    @Transactional
    public LoginResponse refresh(String refreshToken) {
        var claims = jwt.validate(refreshToken, "REFRESH");
        try {
            var session = sessions.findByIdForUpdate(claims.getStringClaim("sid"))
                .orElseThrow(AuthService::unauthorized);
            if (session.isRevoked() || !session.getExpiresAt().isAfter(clock.instant()) || !session.getMemberId()
                .toString().equals(claims.getSubject()) || !MessageDigest.isEqual(
                session.getRefreshHash().getBytes(StandardCharsets.UTF_8),
                hash(refreshToken).getBytes(StandardCharsets.UTF_8)))
                throw unauthorized();
            session.revoke();
            return issue(members.findById(session.getMemberId()).orElseThrow(AuthService::unauthorized));
        } catch (java.text.ParseException error) {
            throw unauthorized();
        }
    }

    @Transactional
    public void logout(String sessionId) {
        sessions.findByIdForUpdate(sessionId).ifPresent(TokenSession::revoke);
    }

    private static String hash(String value) {
        try {
            return HexFormat.of()
                .formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception error) {
            throw new IllegalStateException();
        }
    }

    private static BusinessException unauthorized() {
        return new BusinessException(401, "AUTHENTICATION_FAILED", "인증 정보가 올바르지 않습니다.");
    }
}
