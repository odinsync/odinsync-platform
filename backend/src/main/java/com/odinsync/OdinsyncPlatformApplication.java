package com.odinsync;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class OdinsyncPlatformApplication {
	public static void main(String[] args) {
		SpringApplication.run(OdinsyncPlatformApplication.class, args);
	}
}
