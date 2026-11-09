package com.misterworld.server.integration;

import org.springframework.stereotype.Component;

@Component
public class UnavailableSmsClient implements SmsClient {
    public void sendSms(String phoneNumber, String message) {
        throw new IllegalStateException("SMS provider is not configured");
    }
}
