package com.misterworld.server.voice.controller;

import com.misterworld.server.voice.dto.VoiceDtos.InterpretationRequest;
import com.misterworld.server.voice.dto.VoiceDtos.InterpretationResponse;
import com.misterworld.server.voice.dto.VoiceDtos.Transcription;
import com.misterworld.server.voice.service.VoicePreviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/voice")
@RequiredArgsConstructor
public class VoicePreviewController {
    private final VoicePreviewService voiceService;

    @PostMapping(value = "/transcriptions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Transcription transcribe(@RequestPart(value = "audio", required = false) MultipartFile audio) {
        return voiceService.transcribe(audio);
    }

    @PostMapping(value = "/interpretations", consumes = MediaType.APPLICATION_JSON_VALUE)
    public InterpretationResponse interpret(@Valid @RequestBody InterpretationRequest request) {
        return voiceService.interpret(request);
    }
}
