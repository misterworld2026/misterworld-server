package com.misterworld.server.controller;

import lombok.RequiredArgsConstructor;

import com.misterworld.server.dto.*;
import com.misterworld.server.service.VoiceService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/voice")
@RequiredArgsConstructor
public class VoiceController {
    private final VoiceService service;

    @PostMapping("/interpret")
    public VoiceSelectionResult interpret(@Valid @RequestBody VoiceCommandRequest request) {
        return service.interpret(request);
    }
}
