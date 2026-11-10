package com.misterworld.server;

import com.misterworld.server.inventory.dto.ItemInventoryRequest;
import com.misterworld.server.inventory.entity.InventoryStatus;
import com.misterworld.server.inventory.entity.ItemInventory;
import com.misterworld.server.inventory.service.ItemInventoryService;
import com.misterworld.server.member.entity.Member;
import com.misterworld.server.member.entity.Role;
import com.misterworld.server.notification.entity.NotificationStatus;
import com.misterworld.server.notification.entity.SmsNotification;
import com.misterworld.server.travelapplication.entity.ParticipantType;
import com.misterworld.server.travelapplication.entity.TravelApplication;
import com.misterworld.server.travelapplication.entity.TravelParticipant;
import com.misterworld.server.travelapplication.service.TravelApplicationService;
import com.misterworld.server.domain.TravelOption;
import com.misterworld.server.domain.TravelProduct;
import com.misterworld.server.domain.ScheduleStatus;
import com.misterworld.server.domain.TravelSchedule;

import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.*;

import jakarta.persistence.Column;

import static org.assertj.core.api.Assertions.*;

class NameContractTests {
    // Independently transcribed from design PDF p.6 image, not source-code string searches.
    @Test
    void entityFieldOwnersTypesAndKoreanMapping() throws Exception {
        check(Member.class,
            Map.of("id", Long.class, "email", String.class, "password", String.class, "name", String.class, "address",
                String.class, "phoneNumber", String.class, "travelCount", Integer.class, "role", Role.class));
        check(TravelProduct.class,
            Map.of("name", String.class, "theme", String.class, "description", String.class, "classicPrice",
                Integer.class, "grandPrice", Integer.class, "premiumPrice", Integer.class));
        check(TravelOption.class,
            Map.of("type", String.class, "name", String.class, "grade", String.class, "additionalPrice", Integer.class,
                "defaultForGrade", String.class));
        check(TravelSchedule.class,
            Map.of("travelScheduleId", Long.class, "departureDate", LocalDate.class, "arrivalDate", LocalDate.class,
                "status", ScheduleStatus.class, "applicantCount", Integer.class));
        check(TravelApplication.class,
            Map.of("tourGrade", String.class, "hotelOption", String.class, "transportOption", String.class,
                "mealOption", String.class, "extraOption", String.class, "totalPrice", Integer.class, "appliedAt",
                LocalDateTime.class));
        check(TravelParticipant.class,
            Map.of("name", String.class, "phoneNumber", String.class, "birthDate", LocalDate.class, "participantType",
                ParticipantType.class));
        check(ItemInventory.class,
            Map.of("itemName", String.class, "quantity", Integer.class, "status", InventoryStatus.class, "updatedAt",
                LocalDateTime.class));
        check(SmsNotification.class,
            Map.of("recipientNumber", String.class, "message", String.class, "status", NotificationStatus.class,
                "sentAt", LocalDateTime.class));
        assertThat(Member.class.getDeclaredField("travelCount").getAnnotation(Column.class).name()).isEqualTo("Field");
        assertThat(TravelSchedule.class.getDeclaredField("arrivalDate").getAnnotation(Column.class).name()).isEqualTo(
            "종료일");
    }

    private void check(Class<?> owner, Map<String, Class<?>> fields) throws Exception {
        for (var field : fields.entrySet()) {
            var actual = owner.getDeclaredField(field.getKey());
            assertThat(actual.getDeclaringClass()).isEqualTo(owner);
            assertThat(actual.getType()).isEqualTo(field.getValue());
        }
    }

    @Test
    void inventoryNamesFromClassDiagramAndNarrativeCoexist() throws Exception {
        assertThat(
            ItemInventoryService.class.getDeclaredMethod("registerItem", ItemInventoryRequest.class)).isNotNull();
        assertThat(
            ItemInventoryService.class.getDeclaredMethod("registerInventory", ItemInventoryRequest.class)).isNotNull();
        assertThat(
            ItemInventoryService.class.getDeclaredMethod("updateQuantity", Long.class, Integer.class)).isNotNull();
        assertThat(
            ItemInventoryService.class.getDeclaredMethod("updateInventory", Long.class, Integer.class)).isNotNull();
        assertThat(ItemInventoryService.class.getDeclaredMethod("determineInventoryStatus", Integer.class)).isNotNull();
        assertThat(TravelApplicationService.class.getDeclaredMethod("getMyApplication", Long.class,
            org.springframework.data.domain.Pageable.class)).isNotNull();
    }
}
