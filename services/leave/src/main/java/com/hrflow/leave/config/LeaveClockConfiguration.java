package com.hrflow.leave.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class LeaveClockConfiguration {
    @Bean Clock leaveClock() { return Clock.systemUTC(); }
}
