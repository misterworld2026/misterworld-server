package com.misterworld.server.controller.member;

import com.misterworld.server.dto.voice.VoiceDtos.InterpretationRequest;
import com.misterworld.server.dto.voice.VoiceDtos.InterpretationResponse;
import com.misterworld.server.dto.voice.VoiceDtos.Transcription;
import com.misterworld.server.service.VoiceService;
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
public class VoiceController {
    private final VoiceService voiceService;

    @PostMapping(value = "/transcriptions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Transcription transcribe(@RequestPart(value = "audio", required = false) MultipartFile audio) {
        return voiceService.transcribe(audio);
    }

    @PostMapping(value = "/interpretations", consumes = MediaType.APPLICATION_JSON_VALUE)
    public InterpretationResponse interpret(@Valid @RequestBody InterpretationRequest request) {
        return voiceService.interpret(request);
    }
}
