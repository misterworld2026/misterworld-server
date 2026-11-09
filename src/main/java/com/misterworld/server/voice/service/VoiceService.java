package com.misterworld.server.voice.service;

import com.misterworld.server.common.BusinessException;
import com.misterworld.server.travelproduct.service.TravelProductService;
import com.misterworld.server.travelschedule.repository.TravelScheduleRepository;
import com.misterworld.server.voice.dto.VoiceCommandRequest;
import com.misterworld.server.voice.dto.VoiceSelectionResult;
import com.misterworld.server.voice.integration.VoiceProcessingClient;


import lombok.RequiredArgsConstructor;

import com.misterworld.server.voice.integration.VoiceProcessingClient.InferenceState;
import com.misterworld.server.voice.integration.VoiceProcessingClient.VoiceInterpretation;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class VoiceService {
    private final VoiceProcessingClient client;
    private final TravelProductService products;
    private final TravelScheduleRepository schedules;

    public VoiceSelectionResult interpret(VoiceCommandRequest request) {
        // The external call happens before any database read transaction.
        var result = client.interpret(request);
        return matchSelection(result);
    }

    public VoiceSelectionResult interpretVoice(String text, Map<String, String> context) {
        return interpret(new VoiceCommandRequest(text, context));
    }

    public int evaluateInferenceFailures(Map<String, InferenceState> fields) {
        return (int) List.of("tour", "schedule", "hotel", "meal").stream()
            .filter(key -> fields.get(key) == InferenceState.FAILED).count();
    }

    public VoiceSelectionResult matchSelection(VoiceInterpretation result) {
        var fields = new HashMap<>(result.fields() == null ? Map.<String, InferenceState>of() : result.fields());
        var unresolved = new ArrayList<String>();
        if (result.productId() == null)
            unresolved.add("productId");
        if (result.scheduleId() == null)
            unresolved.add("scheduleId");
        if (result.tourGrade() == null)
            unresolved.add("tourGrade");
        if (result.hotelOptionId() == null)
            unresolved.add("hotelOptionId");
        if (result.transportOptionId() == null)
            unresolved.add("transportOptionId");
        if (result.mealOptionId() == null)
            unresolved.add("mealOptionId");
        if (unresolved.isEmpty()) {
            try {
                products.validateSelection(result.productId(), result.tourGrade(),
                    List.of(result.hotelOptionId(), result.transportOptionId(), result.mealOptionId()));
            } catch (BusinessException error) {
                unresolved.add("productAndOptions");
                fields.put("tour", InferenceState.FAILED);
            }
            var schedule = schedules.findById(result.scheduleId());
            if (schedule.isEmpty() || !schedule.get().getTravelProduct().getId().equals(result.productId())) {
                unresolved.add("scheduleId");
                fields.put("schedule", InferenceState.FAILED);
            }
        }
        int failures = evaluateInferenceFailures(fields);
        return new VoiceSelectionResult(result.productId(), result.scheduleId(), result.tourGrade(),
            result.hotelOptionId(), result.transportOptionId(), result.mealOptionId(), failures, true,
            List.copyOf(unresolved), failures >= 2);
    }
}
