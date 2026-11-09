package com.misterworld.server.service;

import com.misterworld.server.dto.voice.VoiceDtos.*;
import com.misterworld.server.entity.TravelOption;
import com.misterworld.server.entity.TravelProduct;
import com.misterworld.server.entity.TravelSchedule;
import com.misterworld.server.repository.TravelOptionRepository;
import com.misterworld.server.repository.TravelProductRepository;
import com.misterworld.server.repository.TravelScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VoiceServiceTest {
    private final TravelProductRepository products = mock(TravelProductRepository.class);
    private final TravelScheduleRepository schedules = mock(TravelScheduleRepository.class);
    private final TravelOptionRepository options = mock(TravelOptionRepository.class);
    private final VoiceProcessingClient python = mock(VoiceProcessingClient.class);
    private final VoiceService service = new VoiceService(products, schedules, options, python);
    private LocalDate departure;

    @BeforeEach
    void setup() {
        departure = LocalDate.now().plusDays(30);
        TravelProduct product = new TravelProduct();
        product.setId(1L);
        product.setName("푸껫 허니문 5박 7일");
        product.setTheme("HONEYMOON");
        product.setGrandPrice(2190000);
        product.setPremiumPrice(2790000);

        TravelSchedule schedule = new TravelSchedule();
        schedule.setTravelScheduleId(10L);
        schedule.setTravelProduct(product);
        schedule.setDepartureDate(departure);
        schedule.setStatus(TravelSchedule.Status.RECRUITING_SCHEDULE);
        schedule.setApplicantCount(0);

        TravelOption hotel = new TravelOption();
        hotel.setId(20L);
        hotel.setTravelProduct(product);
        hotel.setType("HOTEL");
        hotel.setName("오션뷰 객실 업그레이드");

        when(products.findAll()).thenReturn(List.of(product));
        when(schedules.findAll()).thenReturn(List.of(schedule));
        when(options.findAll()).thenReturn(List.of(hotel));
    }

    @Test
    void matchesOnlyRegisteredSelections() {
        when(python.interpret(any())).thenReturn(new InternalResponse(new InterpretedFields(
                resolved("푸껫 허니문 여행"), resolved(departure.getMonthValue() + "월 " + departure.getDayOfMonth() + "일"),
                resolved("GRAND"), resolved("오션뷰 객실"), omitted(), omitted(), List.of())));
        InterpretationResponse result = service.interpret(request());

        assertEquals(1L, result.selection().productId());
        assertEquals(10L, result.selection().scheduleId());
        assertEquals("GRAND", result.selection().grade());
        assertEquals(20L, result.selection().optionIds().hotel());
        assertEquals(Status.MATCHED, result.fields().hotel().status());
        assertEquals(Status.NOT_MENTIONED, result.fields().meal().status());
        assertEquals(0, result.failureCount());
        assertEquals(NextAction.REVIEW, result.nextAction());
    }

    @Test
    void countsUnresolvedAndInvalidMentionedFields() {
        when(python.interpret(any())).thenReturn(new InternalResponse(new InterpretedFields(
                resolved("푸껫 허니문"), new InterpretedField(State.UNRESOLVED, null),
                omitted(), resolved("없는 객실"), omitted(), omitted(), List.of())));
        InterpretationResponse result = service.interpret(request());

        assertEquals(Status.UNRESOLVED, result.fields().schedule().status());
        assertEquals(Status.INVALID, result.fields().hotel().status());
        assertNull(result.selection().optionIds().hotel());
        assertEquals(2, result.failureCount());
        assertEquals(NextAction.RETRY_OR_GUI, result.nextAction());
    }

    @Test
    void rejectsUnavailableCurrentSchedule() {
        Selection context = new Selection(1L, 999L, "GRAND", new OptionIds(null, null, null, List.of()));
        assertEquals(400, assertThrows(com.misterworld.server.exception.VoiceProcessingException.class,
                () -> service.interpret(new InterpretationRequest("푸껫 허니문", context))).getStatus().value());
    }

    @Test
    void preservesExistingSelectionWhenNothingIsMentioned() {
        when(python.interpret(any())).thenReturn(new InternalResponse(new InterpretedFields(
                omitted(), omitted(), omitted(), omitted(), omitted(), omitted(), List.of())));
        Selection context = new Selection(1L, 10L, "GRAND", new OptionIds(20L, null, null, List.of()));
        InterpretationResponse result = service.interpret(new InterpretationRequest("다시 확인", context));

        assertEquals(context, result.selection());
        assertEquals(Status.NOT_MENTIONED, result.fields().tour().status());
        assertEquals(0, result.failureCount());
    }

    @Test
    void leavesSameMonthAndDaySchedulesAmbiguous() {
        TravelProduct product = products.findAll().getFirst();
        TravelSchedule first = schedules.findAll().getFirst();
        TravelSchedule second = new TravelSchedule();
        second.setTravelScheduleId(11L);
        second.setTravelProduct(product);
        second.setDepartureDate(departure.plusYears(1));
        second.setStatus(TravelSchedule.Status.RECRUITING_SCHEDULE);
        second.setApplicantCount(0);
        when(schedules.findAll()).thenReturn(List.of(first, second));
        when(python.interpret(any())).thenReturn(new InternalResponse(new InterpretedFields(
                resolved("푸껫 허니문"), resolved(departure.getMonthValue() + "월 " + departure.getDayOfMonth() + "일"),
                omitted(), omitted(), omitted(), omitted(), List.of())));

        InterpretationResponse result = service.interpret(request());
        assertEquals(Status.AMBIGUOUS, result.fields().schedule().status());
        assertNull(result.selection().scheduleId());
        assertEquals(1, result.failureCount());
    }

    private InterpretationRequest request() {
        return new InterpretationRequest("푸껫 허니문", new Selection(null, null, null,
                new OptionIds(null, null, null, List.of())));
    }

    private InterpretedField resolved(String value) { return new InterpretedField(State.RESOLVED, value); }
    private InterpretedField omitted() { return new InterpretedField(State.NOT_MENTIONED, null); }
}
