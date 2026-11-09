package com.misterworld.server.config;

import java.time.Clock;

import org.springframework.context.annotation.*;

@Configuration
public class TimeConfig {
    // Storage timestamps use UTC. Business deadline timezone awaits user decision.
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
