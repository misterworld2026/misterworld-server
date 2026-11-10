package com.misterworld.server.config;

import java.time.Clock;

import org.springframework.context.annotation.*;

@Configuration
public class TimeConfig {
    // Storage timestamps use UTC. BookingPolicy applies the confirmed Asia/Seoul calendar deadline.
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
