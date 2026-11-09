package com.misterworld.server;

import com.misterworld.server.member.repository.MemberRepository;
import com.misterworld.server.notification.repository.SmsNotificationRepository;
import com.misterworld.server.sample.SampleDataInitializer;
import com.misterworld.server.travelapplication.dto.TravelApplicationRequest;
import com.misterworld.server.travelapplication.dto.TravelParticipantRequest;
import com.misterworld.server.travelapplication.entity.ParticipantType;
import com.misterworld.server.travelapplication.service.TravelApplicationService;
import com.misterworld.server.domain.TravelOption;
import com.misterworld.server.repository.TravelOptionRepository;
import com.misterworld.server.domain.ScheduleStatus;
import com.misterworld.server.repository.TravelScheduleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
    "app.sms.provider=unavailable", "app.sms.dispatch-enabled=false", "app.sample.sms-test-phone=01012345678"
})
@ActiveProfiles({"local", "sms-test"})
class SmsTestDataTests {
    @Autowired MemberRepository members;
    @Autowired PasswordEncoder encoder;
    @Autowired TravelScheduleRepository schedules;
    @Autowired TravelOptionRepository options;
    @Autowired TravelApplicationService bookings;
    @Autowired SmsNotificationRepository notifications;

    @Test
    void oneThreePersonBookingConfirmsDepartureAndQueuesOneText() {
        var customer = members.findByEmail(SampleDataInitializer.SMS_CUSTOMER).orElseThrow();
        assertThat(encoder.matches(SampleDataInitializer.PASSWORD, customer.getPassword())).isTrue();
        assertThat(members.count()).isEqualTo(1);
        assertThat(notifications.count()).isZero();
        var schedule = schedules.findAll().getFirst();
        long scheduleId = schedule.getTravelScheduleId();
        long productId = schedule.getTravelProduct().getId();
        var optionIds = options.findByTravelProductId(productId).stream()
            .filter(option -> "CLASSIC".equals(option.getDefaultForGrade()))
            .map(TravelOption::getId).toList();
        var birthDate = LocalDate.of(1990, 1, 1);
        var travelers = List.of(
            new TravelParticipantRequest("신청자", customer.getPhoneNumber(), birthDate, ParticipantType.APPLICANT),
            new TravelParticipantRequest("동행 1", customer.getPhoneNumber(), birthDate, ParticipantType.COMPANION),
            new TravelParticipantRequest("동행 2", customer.getPhoneNumber(), birthDate, ParticipantType.COMPANION)
        );
        var request = new TravelApplicationRequest(productId, scheduleId, "CLASSIC", optionIds, 3,
            travelers, "sms-test-booking", null);
        var quote = bookings.quote(customer.getId(), request);
        bookings.apply(customer.getId(), new TravelApplicationRequest(productId, scheduleId, "CLASSIC", optionIds, 3,
            travelers, request.requestKey(), quote.quoteToken()));

        assertThat(schedules.findById(scheduleId).orElseThrow().getStatus()).isEqualTo(ScheduleStatus.CONFIRMED_SCHEDULE);
        assertThat(notifications.findAll()).singleElement().satisfies(notification -> {
            assertThat(notification.getMessage()).isEqualTo("출발확정");
            assertThat(notification.getRecipientNumber()).isEqualTo("01012345678");
        });
    }
}
