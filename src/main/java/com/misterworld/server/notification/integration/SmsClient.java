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

    class Failure extends IllegalStateException {
        private final String reasonCode;
        private final Integer httpStatus;
        private final String providerCode;

        public Failure(String message, String reasonCode, Integer httpStatus, String providerCode) {
            super(message);
            this.reasonCode = reasonCode;
            this.httpStatus = httpStatus;
            this.providerCode = providerCode;
        }

        public String reasonCode() { return reasonCode; }
        public Integer httpStatus() { return httpStatus; }
        public String providerCode() { return providerCode; }
    }

    class Rejected extends Failure {
        public Rejected(String message) {
            this(message, "PROVIDER_REJECTED", null, null);
        }

        public Rejected(String message, String reasonCode, Integer httpStatus, String providerCode) {
            super(message, reasonCode, httpStatus, providerCode);
        }
    }
}
