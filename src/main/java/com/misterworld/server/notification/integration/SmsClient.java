package com.misterworld.server.notification.integration;

public interface SmsClient {
    void sendSms(String phoneNumber, String message);

    // The delivery key is stable across retries and can also be used for provider-side tracing.
    default void sendSms(String phoneNumber, String message, String deliveryKey) {
        sendSms(phoneNumber, message);
    }

    default boolean supportsSafeRetry() {
        return false;
    }

    class Rejected extends RuntimeException {
        public Rejected(String message) {
            super(message);
        }
    }
}
