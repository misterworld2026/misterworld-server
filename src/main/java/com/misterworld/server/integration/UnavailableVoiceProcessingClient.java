package com.misterworld.server.integration;

import org.springframework.stereotype.Component;
import com.misterworld.server.common.BusinessException;
import com.misterworld.server.dto.VoiceCommandRequest;

@Component
public class UnavailableVoiceProcessingClient implements VoiceProcessingClient {
    public VoiceInterpretation interpret(VoiceCommandRequest request) {
        throw new BusinessException(503, "VOICE_NOT_CONFIGURED", "음성 처리 서비스가 연결되지 않았습니다. GUI를 이용하세요.");
    }
}
