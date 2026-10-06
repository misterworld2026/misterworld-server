package com.misterworld.server.service;

import com.misterworld.server.dto.request.LoginRequest;
import com.misterworld.server.dto.response.LoginResponse;
import com.misterworld.server.exception.AuthenticationFailedException;
import com.misterworld.server.repository.MemberRepository;
import com.misterworld.server.dto.request.SignUpRequest;
import com.misterworld.server.dto.response.SignUpResponse;
import com.misterworld.server.entity.Member;
import com.misterworld.server.security.JwtProvider;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    // 일반 유저 회원가입
    @Transactional
    public SignUpResponse signup(SignUpRequest request) {
        if(!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        if(memberRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        Member member = Member.create(
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()),
                request.getName(),
                request.getAddress(),
                request.getPhoneNumber(),
                Member.Role.MEMBER
        );

        Member savedMember = memberRepository.save(member);

        return new SignUpResponse(
                savedMember.getId(),
                savedMember.getEmail(),
                savedMember.getName(),
                savedMember.getRole()
        );
    }

    // 직원 회원가입
    @Transactional
    public SignUpResponse createStaffAccount(SignUpRequest request) {
        if(!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        if(memberRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        Member member = Member.create(
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()),
                request.getName(),
                request.getAddress(),
                request.getPhoneNumber(),
                Member.Role.STAFF
        );

        Member savedMember = memberRepository.save(member);

        return new SignUpResponse(
                savedMember.getId(),
                savedMember.getEmail(),
                savedMember.getName(),
                savedMember.getRole()
        );
    }

    // 일반 회원 로그인
    public LoginResponse login(LoginRequest request) {

        Member member = memberRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AuthenticationFailedException("이메일 또는 비밀번호가 올바르지 않습니다."));

        if(member.getPassword() == null || !passwordEncoder.matches(request.getPassword(), member.getPassword())) {
            throw new AuthenticationFailedException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        String accessToken = jwtProvider.generateAccessToken(member);
        String refreshToken = jwtProvider.generateRefreshToken(member);

        return new LoginResponse(
                member.getId(),
                member.getEmail(),
                member.getName(),
                member.getRole(),
                accessToken,
                refreshToken,
                "Bearer"
        );
    }

    // 직원 로그인
    public LoginResponse staffLogin(LoginRequest request) {

        Member member = memberRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AuthenticationFailedException("이메일 또는 비밀번호가 올바르지 않습니다."));

        if(member.getPassword() == null || !passwordEncoder.matches(request.getPassword(), member.getPassword())) {
            throw new AuthenticationFailedException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        if(member.getRole() != Member.Role.STAFF) {
            throw new AuthenticationFailedException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        String accessToken = jwtProvider.generateAccessToken(member);
        String refreshToken = jwtProvider.generateRefreshToken(member);

        return new LoginResponse(
                member.getId(),
                member.getEmail(),
                member.getName(),
                member.getRole(),
                accessToken,
                refreshToken,
                "Bearer"
        );
    }
}
