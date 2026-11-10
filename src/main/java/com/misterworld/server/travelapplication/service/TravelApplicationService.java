package com.misterworld.server.travelapplication.service;

import com.misterworld.server.common.BusinessException;
import com.misterworld.server.member.repository.MemberRepository;
import com.misterworld.server.notification.entity.NotificationType;
import com.misterworld.server.notification.service.SmsNotificationService;
import com.misterworld.server.travelapplication.dto.ApplicationResponse;
import com.misterworld.server.travelapplication.dto.TravelApplicationRequest;
import com.misterworld.server.travelapplication.dto.TravelApplicationResponse;
import com.misterworld.server.travelapplication.dto.TravelHistoryResponse;
import com.misterworld.server.travelapplication.dto.TravelPriceResponse;
import com.misterworld.server.travelapplication.dto.TravelQuoteResponse;
import com.misterworld.server.travelapplication.entity.ApplicationStatus;
import com.misterworld.server.travelapplication.entity.ParticipantType;
import com.misterworld.server.travelapplication.entity.TravelApplication;
import com.misterworld.server.travelapplication.entity.TravelParticipant;
import com.misterworld.server.travelapplication.repository.TravelApplicationRepository;
import com.misterworld.server.travelapplication.repository.TravelParticipantRepository;
import com.misterworld.server.travelproduct.dto.TravelOptionResponse;
import com.misterworld.server.domain.TravelOption;
import com.misterworld.server.domain.TravelProduct;
import com.misterworld.server.repository.TravelProductRepository;
import com.misterworld.server.travelproduct.service.TravelProductService;
import com.misterworld.server.domain.ScheduleStatus;
import com.misterworld.server.domain.TravelSchedule;
import com.misterworld.server.repository.TravelScheduleRepository;
import com.misterworld.server.travelschedule.service.TravelScheduleService;


import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.*;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TravelApplicationService {
    private final TravelApplicationRepository applications;
    private final TravelParticipantRepository participants;
    private final MemberRepository members;
    private final TravelProductRepository products;
    private final TravelProductService productService;
    private final TravelScheduleRepository scheduleRepository;
    private final TravelScheduleService schedules;
    private final SmsNotificationService notifications;
    private final BookingPolicy policy;
    private final BookingQuoteSigner signer;
    private final java.time.Clock clock;
    private final jakarta.validation.Validator validator;
    private final ApplicationScheduleLookup applicationScheduleLookup;

    public Page<ApplicationResponse> getMyApplication(Long memberId, Pageable pageable) {
        return applications.findByMemberId(memberId, pageable).map(ApplicationResponse::from);
    }

    public ApplicationResponse getApplication(Long applicationId, Long memberId) {
        return ApplicationResponse.from(
            applications.findByIdAndMemberId(applicationId, memberId).orElseThrow(BusinessException::notFound));
    }

    public Page<TravelHistoryResponse> getTravelHistory(Long memberId, Pageable pageable) {
        // Repository owns the fixed history ordering; caller sorting must not override it.
        return buildTravelHistoryResponse(applications.findCompletedHistoryByMemberId(memberId,
            PageRequest.of(pageable.getPageNumber(), pageable.getPageSize())));
    }

    public Page<TravelHistoryResponse> buildTravelHistoryResponse(Page<TravelApplication> applications) {
        return applications.map(TravelHistoryResponse::from);
    }

    public Page<ApplicationResponse> getApplications(Pageable pageable) {
        return applications.findAll(pageable).map(ApplicationResponse::from);
    }

    /**
     * 사용자 확정: 완료한 여행이 2회 이상이면 새 신청의 할인율은 10%다.
     * 확정된 1인당 가격·할인액 HALF_UP 정책으로 금액 계산에 연결한다.
     */
    public int applyLoyaltyDiscount(long completedTrips) {
        BusinessException.require(completedTrips >= 0, "완료 여행 횟수는 음수일 수 없습니다.");
        return completedTrips >= 2 ? 10 : 0;
    }


    /** Additional internal immutable selection for exact price calculation and quote binding. */
    public record Selection(TravelProduct product,
            TravelSchedule schedule, String grade,
            java.util.List<TravelOption> options) {}

    public TravelQuoteResponse quote(Long memberId,
            TravelApplicationRequest request) {
        validateRequest(request);
        members.findById(memberId).orElseThrow(BusinessException::notFound);
        var schedule=scheduleRepository.findById(request.scheduleId()).orElseThrow(BusinessException::notFound);
        var selection=selection(request,schedule);
        schedules.validateAvailability(schedule,request.applicantCount());
        long completedTrips=applications.countCompletedTrips(memberId);
        var price=discount(calculatePrice(selection,request.applicantCount()),completedTrips);
        var expiresAt=clock.instant().plusSeconds(900);
        return new TravelQuoteResponse(
            productService.getProduct(request.productId()),schedules.getScheduleResponse(schedule),
            request.grade(),request.applicantCount(),selection.options().stream()
                .map(TravelOptionResponse::from).toList(),
            completedTrips,price,signer.issue(memberId,requestHash(request),selectionHash(selection,price),expiresAt),expiresAt);
    }

    @Transactional
    public TravelApplicationResponse apply(Long memberId,
            TravelApplicationRequest request) {
        validateRequest(request);
        // Uniform ordering: schedule -> product -> member. No external call is made under these locks.
        var schedule=scheduleRepository.findByIdForUpdate(request.scheduleId()).orElseThrow(BusinessException::notFound);
        var product=products.findByIdForUpdate(schedule.getTravelProduct().getId()).orElseThrow(BusinessException::notFound);
        var member=members.findByIdForUpdate(memberId).orElseThrow(BusinessException::notFound);
        String requestHash=requestHash(request);
        var previous=applications.findByMemberIdAndRequestKey(memberId,request.requestKey());
        if(previous.isPresent()) {
            if(!requestHash.equals(previous.get().getRequestHash()))
                throw new BusinessException(409,"IDEMPOTENCY_CONFLICT","같은 요청키로 다른 신청을 저장할 수 없습니다.");
            return response(previous.get());
        }
        var selection=selection(request,schedule);
        schedules.validateAndLockSchedule(request.scheduleId(),request.applicantCount());
        long completedTrips=applications.countCompletedTrips(memberId);
        var price=discount(calculatePrice(selection,request.applicantCount()),completedTrips);
        signer.verify(request.quoteToken(),memberId,requestHash,selectionHash(selection,price),clock.instant());
        var application=new TravelApplication(); application.setMember(member); application.setTravelSchedule(schedule);
        application.setTourGrade(request.grade());
        application.setHotelOption(optionName(selection,"HOTEL"));
        application.setTransportOption(optionName(selection,"TRANSPORT")); application.setMealOption(optionName(selection,"MEAL"));
        String extras=selection.options().stream().filter(o -> o.getType().equals("EXTRA"))
            .map(TravelOption::getName).collect(java.util.stream.Collectors.joining(", "));
        BusinessException.require(extras.length()<=2000,"추가 옵션 구성 길이를 초과했습니다.");
        application.setExtraOption(extras.isEmpty()?null:extras); application.setApplicantCount(request.applicantCount());
        application.setBasePrice(price.basePrice()); application.setAdditionalOptionPrice(price.additionalOptionPrice());
        application.setDiscountRate(price.discountRate()); application.setDiscountAmount(price.discountAmount());
        application.setTotalPrice(price.totalPrice());
        boolean alreadyConfirmed=schedule.getStatus()==ScheduleStatus.CONFIRMED_SCHEDULE;
        application.setStatus(alreadyConfirmed ? ApplicationStatus.CONFIRMED :
            ApplicationStatus.PENDING);
        application.setAppliedAt(java.time.LocalDateTime.ofInstant(clock.instant(),java.time.ZoneOffset.UTC));
        application.setRequestKey(request.requestKey()); application.setRequestHash(requestHash);
        applications.saveAndFlush(application);
        for(var participant:request.participants()) {
            var savedParticipant=new TravelParticipant();
            savedParticipant.setTravelApplication(application); savedParticipant.setName(participant.name());
            savedParticipant.setPhoneNumber(participant.phoneNumber()); savedParticipant.setBirthDate(participant.birthDate());
            savedParticipant.setParticipantType(participant.participantType()); participants.save(savedParticipant);
        }
        schedules.updateApplicantCount(request.scheduleId());
        if(alreadyConfirmed) notifications.sendConfirmationNotification(application);
        else if (!schedules.checkDepartureCondition(request.scheduleId())) {
            notifications.createNotification(application, NotificationType.APPLICATION_COMPLETED);
        }
        return response(application);
    }

    private TravelApplicationResponse response(TravelApplication application) {
        return new TravelApplicationResponse(ApplicationResponse.from(application),
            application.getTravelSchedule().getStatus(),application.getStatus()==ApplicationStatus.CONFIRMED);
    }
    private Selection selection(TravelApplicationRequest request,
            TravelSchedule schedule) {
        BusinessException.require(schedule.getTravelProduct().getId().equals(request.productId()),"다른 상품의 일정입니다.");
        var selected=productService.validateSelection(request.productId(),request.grade(),request.optionIds()).stream()
            .sorted(java.util.Comparator.comparing(TravelOption::getType)
                .thenComparing(TravelOption::getId)).toList();
        return new Selection(schedule.getTravelProduct(),schedule,request.grade(),selected);
    }
    private void validateRequest(TravelApplicationRequest request) {
        BusinessException.require(request!=null && validator.validate(request).isEmpty(),"신청 입력값을 확인하세요.");
        BusinessException.require(request.participants().size()==request.applicantCount(),"신청 인원과 여행객 수가 다릅니다.");
        BusinessException.require(request.participants().stream().filter(p -> p.participantType()==
            ParticipantType.APPLICANT).count()==1,"신청 여행객은 정확히 한 명이어야 합니다.");
    }
    public TravelPriceResponse calculatePrice(Selection selection,int applicantCount) {
        policy.requirePricing();
        try {
            int unit=switch(selection.grade()) { case "CLASSIC" -> selection.product().getClassicPrice();
                case "GRAND" -> selection.product().getGrandPrice(); default -> selection.product().getPremiumPrice(); };
            int basePrice=Math.multiplyExact(unit,applicantCount);
            int optionUnit=0;
            for(var option:selection.options()) if(!selection.grade().equals(option.getDefaultForGrade()))
                optionUnit=Math.addExact(optionUnit,option.getAdditionalPrice());
            int additionalOptionPrice=Math.multiplyExact(optionUnit,applicantCount);
            return new TravelPriceResponse(basePrice,additionalOptionPrice,0,0,
                Math.addExact(basePrice,additionalOptionPrice));
        } catch(ArithmeticException e) { throw new BusinessException(400,"PRICE_OVERFLOW","지원하는 금액 범위를 초과했습니다."); }
    }
    private TravelPriceResponse discount(
            TravelPriceResponse price,long completedTrips) {
        int discountRate=applyLoyaltyDiscount(completedTrips);
        int discountAmount=java.math.BigDecimal.valueOf(price.totalPrice()).multiply(java.math.BigDecimal.valueOf(discountRate))
            .divide(java.math.BigDecimal.valueOf(100),0,policy.discountRounding()).intValueExact();
        return new TravelPriceResponse(price.basePrice(),price.additionalOptionPrice(),
            discountRate,discountAmount,price.totalPrice()-discountAmount);
    }
    private String optionName(Selection selection,String type) {
        return selection.options().stream().filter(o -> o.getType().equals(type)).findFirst().orElseThrow().getName();
    }
    private String canonical(Object... values) {
        var text=new StringBuilder(); for(Object value:values) { String v=String.valueOf(value); text.append(v.length()).append(':').append(v); }
        return text.toString();
    }
    private String requestHash(TravelApplicationRequest request) {
        var text=new StringBuilder(canonical(request.productId(),request.scheduleId(),request.grade(),request.applicantCount(),request.requestKey()));
        request.optionIds().stream().sorted().forEach(id -> text.append(canonical(id)));
        for(var p:request.participants()) text.append(canonical(p.name(),p.phoneNumber(),p.birthDate(),p.participantType()));
        return signer.hash(text.toString());
    }
    private String selectionHash(Selection selection,TravelPriceResponse price) {
        var product=selection.product(); var schedule=selection.schedule();
        var text=new StringBuilder(canonical(product.getId(),product.getName(),product.getTheme(),product.getDescription(),
            schedule.getTravelScheduleId(),schedule.getDepartureDate(),schedule.getArrivalDate(),selection.grade(),price));
        for(var o:selection.options()) text.append(canonical(o.getId(),o.getType(),o.getName(),o.getGrade(),o.getAdditionalPrice(),
            o.getDefaultForGrade(),o.getAllowedTourGrades(),o.getBenefitCode()));
        return signer.hash(text.toString());
    }
    @Transactional
    public void cancelApplication(Long applicationId,Long memberId) {
        Long scheduleId=applicationScheduleLookup.scheduleId(applicationId,memberId);
        var schedule=scheduleRepository.findByIdForUpdate(scheduleId).orElseThrow(BusinessException::notFound);
        // The ownership lookup can precede a competing confirmation; refresh under the schedule lock.
        var application=applications.findByIdAndMemberId(applicationId,memberId).orElseThrow(BusinessException::notFound);
        entityManager.refresh(application);
        if(application.getStatus()==ApplicationStatus.CANCELLED) return;
        if(application.getStatus()!=ApplicationStatus.PENDING ||
            (schedule.getStatus()!=ScheduleStatus.RECRUITING_SCHEDULE &&
             schedule.getStatus()!=ScheduleStatus.CLOSED_SCHEDULE))
            throw new BusinessException(409,"CANCELLATION_NOT_ALLOWED","출발 확정 전 대기 신청만 취소할 수 있습니다.");
        if(!policy.cancelAfterDeadline() && !policy.withinDeadline(schedule.getDepartureDate(),clock))
            throw new BusinessException(409,"CANCELLATION_NOT_ALLOWED","신청 마감 후에는 취소할 수 없습니다.");
        application.setStatus(ApplicationStatus.CANCELLED);
        application.setCancelledAt(java.time.LocalDateTime.ofInstant(clock.instant(),java.time.ZoneOffset.UTC));
        schedules.updateApplicantCount(schedule.getTravelScheduleId());
    }
    @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager entityManager;
    // Class-diagram names delegate to the p.23 pricing flow, without duplicating formulas.
    public TravelQuoteResponse calculateFinalPrice(Long memberId,
            TravelApplicationRequest request) {
        return quote(memberId,request);
    }
    public TravelPriceResponse calculateDiscount(Long memberId,
            TravelApplicationRequest request) {
        return calculateFinalPrice(memberId,request).price();
    }

}
