package com.misterworld.server.voice.controller;

import com.misterworld.server.voice.dto.VoiceCommandRequest;
import com.misterworld.server.voice.dto.VoiceSelectionResult;
import com.misterworld.server.voice.service.VoiceService;


import lombok.RequiredArgsConstructor;

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
