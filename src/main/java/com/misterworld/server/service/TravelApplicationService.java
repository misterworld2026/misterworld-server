package com.misterworld.server.service;

import lombok.RequiredArgsConstructor;

import com.misterworld.server.common.BusinessException;
import com.misterworld.server.dto.ApplicationResponse;
import com.misterworld.server.repository.TravelApplicationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.*;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TravelApplicationService {
    private final TravelApplicationRepository applications;

    public Page<ApplicationResponse> getMyApplication(Long memberId, Pageable pageable) {
        return applications.findByMemberId(memberId, pageable).map(ApplicationResponse::from);
    }

    public ApplicationResponse getApplication(Long applicationId, Long memberId) {
        return ApplicationResponse.from(
            applications.findByIdAndMemberId(applicationId, memberId).orElseThrow(BusinessException::notFound));
    }

    public Page<ApplicationResponse> getTravelHistory(Long memberId, Pageable pageable) {
        return applications.findCompletedHistoryByMemberId(memberId, pageable).map(ApplicationResponse::from);
    }

    public Page<ApplicationResponse> getApplications(Pageable pageable) {
        return applications.findAll(pageable).map(ApplicationResponse::from);
    }

    /**
     * 사용자 확정: 완료한 여행이 2회 이상이면 새 신청의 할인율은 10%다.
     * 금액 계산·반올림은 별도 가격 정책 결정 후 연결한다.
     */
    public int applyLoyaltyDiscount(long completedTrips) {
        BusinessException.require(completedTrips >= 0, "완료 여행 횟수는 음수일 수 없습니다.");
        return completedTrips >= 2 ? 10 : 0;
    }

    public void apply(Long memberId) {
        throw BusinessException.policy();
    }

    public void cancelApplication(Long applicationId, Long memberId) {
        getApplication(applicationId, memberId);
        throw BusinessException.policy();
    }
}
