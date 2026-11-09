package com.misterworld.server.notification.integration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.sms.provider", havingValue = "unavailable", matchIfMissing = true)
public class UnavailableSmsClient implements SmsClient {
    public void sendSms(String phoneNumber, String message) {
        throw new Rejected("SMS provider is not configured");
    }
}
