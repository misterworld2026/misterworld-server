package com.misterworld.server.sample;

import com.misterworld.server.member.entity.Member;
import com.misterworld.server.member.entity.Role;
import com.misterworld.server.member.repository.MemberRepository;
import com.misterworld.server.travelproduct.dto.TravelOptionRequest;
import com.misterworld.server.travelproduct.dto.TravelProductRequest;
import com.misterworld.server.travelproduct.entity.TravelProduct;
import com.misterworld.server.travelproduct.repository.TravelProductRepository;
import com.misterworld.server.travelproduct.service.TravelProductService;
import com.misterworld.server.travelschedule.dto.TravelScheduleRequest;
import com.misterworld.server.travelschedule.entity.TravelSchedule;
import com.misterworld.server.travelschedule.entity.ScheduleStatus;
import com.misterworld.server.travelschedule.repository.TravelScheduleRepository;
import com.misterworld.server.travelschedule.service.TravelScheduleService;
import com.misterworld.server.travelapplication.entity.*;
import com.misterworld.server.travelapplication.repository.*;
import com.misterworld.server.notification.service.SmsNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import javax.sql.DataSource;
import java.time.*;
import java.util.*;

/** Opt-in demo fixtures only. Neither Flyway nor normal application startup seeds accounts. */
@Component
@Profile("local & (sample-data | sms-test)")
@RequiredArgsConstructor
public class SampleDataInitializer implements ApplicationRunner {
    public static final String PASSWORD="DemoTrip!2026";
    public static final String CUSTOMER="customer@misterworld.test";
    public static final String LOYAL="loyal@misterworld.test";
    public static final String OTHER="other@misterworld.test";
    public static final String STAFF="staff@misterworld.test";
    public static final String SMS_CUSTOMER="sms-test@misterworld.test";
    private final DataSource dataSource;
    private final MemberRepository members;
    private final PasswordEncoder encoder;
    private final TravelProductService productService;
    private final TravelProductRepository products;
    private final TravelScheduleService scheduleService;
    private final TravelScheduleRepository schedules;
    private final TravelApplicationRepository applications;
    private final TravelParticipantRepository participants;
    private final SmsNotificationService notifications;
    private final Clock clock;
    private final Environment environment;
    @Value("${app.sample.sms-test-phone:}")
    private String smsTestPhone;

    @Override @Transactional
    public void run(ApplicationArguments args) throws Exception {
        try(var connection=dataSource.getConnection()) {
            if(!connection.getMetaData().getURL().startsWith("jdbc:h2:mem:"))
                throw new IllegalStateException("Sample data requires the local in-memory H2 database");
        }
        if (environment.matchesProfiles("sms-test")) {
            createSmsTestData();
            return;
        }
        if(members.existsByEmail(CUSTOMER)) {
            // Refuse to replace a coincidentally existing account. A complete seeded set is a no-op.
            for(String email:List.of(CUSTOMER,LOYAL,OTHER,STAFF)) {
                var existing=members.findByEmail(email).orElseThrow(() -> new IllegalStateException("Sample account collision"));
                if(!encoder.matches(PASSWORD,existing.getPassword()) ||
                    existing.getRole()!=(email.equals(STAFF)?Role.STAFF:Role.MEMBER))
                    throw new IllegalStateException("Sample account collision");
            }
            return;
        }
        for(String email:List.of(LOYAL,OTHER,STAFF))
            if(members.existsByEmail(email)) throw new IllegalStateException("Sample account collision");
        var customer=member(CUSTOMER,"샘플 고객",Role.MEMBER,0,"01000000001");
        var loyal=member(LOYAL,"샘플 단골 고객",Role.MEMBER,2,"01000000002");
        var other=member(OTHER,"샘플 다른 고객",Role.MEMBER,1,"01000000003");
        member(STAFF,"샘플 직원",Role.STAFF,0,"01000000004");
        var future=new LinkedHashMap<String,TravelSchedule>();
        var seededProducts=new LinkedHashMap<String,TravelProduct>();
        var today=LocalDate.now(clock.withZone(ZoneId.of("Asia/Seoul")));
        for(String theme:List.of("허니문","효도","골프","트레킹")) {
            var product=createProduct(theme);seededProducts.put(theme,product);
            for(int days:List.of(21,35)) {
                var created=scheduleService.saveSchedule(product.getId(),new TravelScheduleRequest(today.plusDays(days),today.plusDays(days+3)));
                var schedule=schedules.findById(created.travelScheduleId()).orElseThrow();
                if(days==21)future.put(theme,schedule);
            }
        }
        // A new 1-person booking crosses the general departure threshold on this schedule.
        var pending=future.get("골프");
        application(customer,pending,"CLASSIC",2,ApplicationStatus.PENDING);
        pending.setApplicantCount(2);
        // Already-confirmed schedule for additional-booking and staff-cancellation demonstrations.
        var confirmed=future.get("효도");confirmed.setStatus(ScheduleStatus.CONFIRMED_SCHEDULE);
        var confirmedApplication=application(other,confirmed,"GRAND",3,ApplicationStatus.CONFIRMED);
        confirmed.setApplicantCount(3);notifications.createNotification(confirmedApplication);
        var golfPast=pastSchedule(seededProducts.get("골프"),today.minusDays(70));
        application(loyal,golfPast,"GRAND",1,ApplicationStatus.CONFIRMED);
        application(other,golfPast,"CLASSIC",2,ApplicationStatus.CONFIRMED);golfPast.setApplicantCount(3);
        var trekkingPast=pastSchedule(seededProducts.get("트레킹"),today.minusDays(40));
        application(loyal,trekkingPast,"PREMIUM",3,ApplicationStatus.CONFIRMED);trekkingPast.setApplicantCount(3);
    }

    private void createSmsTestData() {
        if (!smsTestPhone.matches("010[0-9]{8}")) {
            throw new IllegalStateException("Set SMS_TEST_PHONE to an 11-digit mobile number");
        }
        if (members.existsByEmail(SMS_CUSTOMER)) return;
        member(SMS_CUSTOMER, "문자 테스트 고객", Role.MEMBER, 0, smsTestPhone);
        var product = createProduct("골프", "[문자 테스트] 골프 여행", "3명 신청 시 출발확정 문자가 발송되는 로컬 테스트 상품");
        var departure = LocalDate.now(clock.withZone(ZoneId.of("Asia/Seoul"))).plusDays(21);
        scheduleService.saveSchedule(product.getId(), new TravelScheduleRequest(departure, departure.plusDays(3)));
    }
    private Member member(String email,String name,Role role,int travelCount,String phoneNumber) {
        var member=new Member();member.setEmail(email);member.setName(name);member.setRole(role);
        member.setPassword(encoder.encode(PASSWORD));member.setAddress("서울 샘플 주소");
        member.setPhoneNumber(phoneNumber);member.setTravelCount(travelCount);return members.save(member);
    }
    private TravelProduct createProduct(String theme) {
        return createProduct(theme, "[샘플] " + theme + " 테마 여행",
            "Swagger 동작 확인용 데이터. 실제 예약·결제·문자 발송이 아닙니다.");
    }

    private TravelProduct createProduct(String theme, String name, String description) {
        boolean restricted=Set.of("허니문","효도").contains(theme);
        var grades=restricted?List.of("GRAND","PREMIUM"):List.of("CLASSIC","GRAND","PREMIUM");
        var options=new ArrayList<TravelOptionRequest>();
        for(String grade:grades) {
            int level=switch(grade){case "CLASSIC"->3;case "GRAND"->4;default->5;};
            options.add(new TravelOptionRequest(null,"HOTEL",level+"등급 호텔",grade,(level-3)*50000,
                "샘플 기본 호텔",grade,grades,null));
            options.add(new TravelOptionRequest(null,"TRANSPORT",theme.equals("허니문")?"2인 전용 고급차량":"10인 고급 승합차",
                grade,0,"샘플 기본 교통",grade,grades,null));
            String meal=switch(grade){case "CLASSIC"->"도시락";case "GRAND"->"현지식 레스토랑";default->"스테이크와 기본 샴페인";};
            options.add(new TravelOptionRequest(null,"MEAL",meal,grade,(level-3)*20000,"샘플 기본 식사",grade,grades,null));
        }
        options.add(new TravelOptionRequest(null,"EXTRA","추가 샴페인","GRAND",30000,
            "프리미엄에는 기본 포함되어 선택 불가",null,restricted?List.of("GRAND"):List.of("CLASSIC","GRAND"),"CHAMPAGNE"));
        options.add(new TravelOptionRequest(null,"EXTRA","커피","GRAND",5000,"샘플 추가 음료",null,grades,"COFFEE"));
        int classic=switch(theme){case "골프"->450000;case "트레킹"->250000;default->0;};
        int grand=switch(theme){case "허니문"->900000;case "효도"->650000;case "골프"->650000;default->400000;};
        int premium=switch(theme){case "허니문"->1300000;case "효도","골프"->950000;default->650000;};
        var response=productService.saveProduct(new TravelProductRequest(name,theme,
            description,restricted?null:classic,grand,premium,options));
        return products.findById(response.id()).orElseThrow();
    }
    private TravelSchedule pastSchedule(TravelProduct product,LocalDate start) {
        var schedule=new TravelSchedule();schedule.setTravelProduct(product);schedule.setDepartureDate(start);
        schedule.setArrivalDate(start.plusDays(3));schedule.setStatus(ScheduleStatus.COMPLETED_SCHEDULE);
        schedule.setApplicantCount(0);return schedules.save(schedule);
    }
    private TravelApplication application(Member member,TravelSchedule schedule,String grade,int count,ApplicationStatus status) {
        var application=new TravelApplication();application.setMember(member);application.setTravelSchedule(schedule);
        application.setTourGrade(grade);application.setApplicantCount(count);application.setStatus(status);
        int level=switch(grade){case "CLASSIC"->3;case "GRAND"->4;default->5;};
        application.setHotelOption(level+"등급 호텔");application.setTransportOption("10인 고급 승합차");
        application.setMealOption(switch(grade){case "CLASSIC"->"도시락";case "GRAND"->"현지식 레스토랑";default->"스테이크와 기본 샴페인";});
        var product=schedule.getTravelProduct();int unit=switch(grade){case "CLASSIC"->product.getClassicPrice();case "GRAND"->product.getGrandPrice();default->product.getPremiumPrice();};
        application.setBasePrice(unit*count);application.setAdditionalOptionPrice(0);application.setDiscountRate(0);
        application.setDiscountAmount(0);application.setTotalPrice(unit*count);
        application.setAppliedAt(schedule.getDepartureDate().isBefore(LocalDate.now(clock.withZone(ZoneId.of("Asia/Seoul"))))
            ? schedule.getDepartureDate().minusDays(20).atTime(12,0)
            : LocalDateTime.ofInstant(clock.instant(),ZoneOffset.UTC));
        applications.saveAndFlush(application);
        for(int i=0;i<count;i++) {
            var participant=new TravelParticipant();participant.setTravelApplication(application);
            participant.setName(i==0?member.getName():"샘플 동행자 "+i);participant.setPhoneNumber(member.getPhoneNumber());
            participant.setBirthDate(LocalDate.of(1990,1,1).plusYears(i));
            participant.setParticipantType(i==0?ParticipantType.APPLICANT:ParticipantType.COMPANION);participants.save(participant);
        }
        return application;
    }
}
