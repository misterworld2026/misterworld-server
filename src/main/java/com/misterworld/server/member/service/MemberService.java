package com.misterworld.server.member.service;

import com.misterworld.server.common.BusinessException;
import com.misterworld.server.member.dto.MemberUpdateRequest;
import com.misterworld.server.member.entity.Member;
import com.misterworld.server.member.repository.MemberRepository;


import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MemberService {
    private final MemberRepository members;

    public Member getMyInfo(Long memberId) {
        return members.findById(memberId).orElseThrow(BusinessException::notFound);
    }

    @Transactional
    public Member updateMyInfo(Long memberId, MemberUpdateRequest request) {
        Member member = getMyInfo(memberId);
        member.setName(request.name());
        member.setAddress(request.address());
        member.setPhoneNumber(request.phoneNumber());
        return member;
    }
}
