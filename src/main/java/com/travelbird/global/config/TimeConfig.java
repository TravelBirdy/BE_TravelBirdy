package com.travelbird.global.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class TimeConfig {
    @Bean
    public Clock clock() {
        // AI job timestamps and quota query boundaries are stored/calculated in UTC.
        return Clock.systemUTC();
    }
}
