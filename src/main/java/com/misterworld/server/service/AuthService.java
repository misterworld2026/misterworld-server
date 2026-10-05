package com.misterworld.server.service;

import com.misterworld.server.repository.MemberRepository;
import com.misterworld.server.dto.request.SignUpRequest;
import com.misterworld.server.dto.response.SignUpResponse;
import com.misterworld.server.entity.Member;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

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
                request.getPassword(),
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
                request.getPassword(),
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
}
