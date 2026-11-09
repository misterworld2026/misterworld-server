package com.misterworld.server;

import com.misterworld.server.sample.SampleDataInitializer;
import com.misterworld.server.member.repository.MemberRepository;
import com.misterworld.server.travelproduct.repository.*;
import com.misterworld.server.travelschedule.repository.TravelScheduleRepository;
import com.misterworld.server.travelapplication.repository.*;
import com.misterworld.server.notification.repository.SmsNotificationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:sample-data-check;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=FALSE")
@ActiveProfiles({"local","sample-data"})
class SampleDataTests {
    @Autowired SampleDataInitializer initializer;
    @Autowired MemberRepository members;
    @Autowired TravelProductRepository products;
    @Autowired TravelOptionRepository options;
    @Autowired TravelScheduleRepository schedules;
    @Autowired TravelApplicationRepository applications;
    @Autowired TravelParticipantRepository participants;
    @Autowired SmsNotificationRepository notifications;

    @Test void createsCoherentDemoDataAndRepeatedInitializationDoesNotDuplicateIt() throws Exception {
        assertThat(members.count()).isEqualTo(4);assertThat(products.count()).isEqualTo(4);
        assertThat(options.count()).isEqualTo(38);assertThat(schedules.count()).isEqualTo(10);
        assertThat(applications.count()).isEqualTo(5);assertThat(participants.count()).isEqualTo(11);
        assertThat(notifications.count()).isEqualTo(1);
        var loyal=members.findByEmail(SampleDataInitializer.LOYAL).orElseThrow();
        assertThat(applications.countCompletedTrips(loyal.getId())).isEqualTo(2);
        initializer.run(new DefaultApplicationArguments());
        assertThat(members.count()).isEqualTo(4);assertThat(products.count()).isEqualTo(4);
        assertThat(applications.count()).isEqualTo(5);assertThat(notifications.count()).isEqualTo(1);
    }
}
