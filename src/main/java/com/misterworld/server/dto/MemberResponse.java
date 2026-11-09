package com.misterworld.server.dto;

import com.misterworld.server.domain.*;

public record MemberResponse(
    Long id,
    String email,
    String name,
    String address,
    String phoneNumber,
    Integer travelCount,
    Role role
) {
    public static MemberResponse from(Member member) {
        return new MemberResponse(member.getId(), member.getEmail(), member.getName(), member.getAddress(),
            member.getPhoneNumber(), member.getTravelCount(), member.getRole());
    }
}
