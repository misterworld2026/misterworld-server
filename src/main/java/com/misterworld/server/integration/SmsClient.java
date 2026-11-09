package com.misterworld.server.integration;

public interface SmsClient {
    void sendSms(String phoneNumber, String message);
}
