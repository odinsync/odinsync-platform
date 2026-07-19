package com.odinsync.shared.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TimeConfiguration {

	/**
	 * Provides the shared UTC clock used by time-sensitive application services.
	 */
	@Bean
	Clock clock() {
		return Clock.systemUTC();
	}
}
