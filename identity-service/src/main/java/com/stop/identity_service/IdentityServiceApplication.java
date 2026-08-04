package com.stop.identity_service;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

@SpringBootApplication
@EnableCaching
public class IdentityServiceApplication {

	private static final Logger log = LoggerFactory.getLogger(IdentityServiceApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(IdentityServiceApplication.class, args);
	}

	@Bean
	ApplicationRunner deployVerificationMarker() {
		return args -> log.info("STOP identity-service deploy verification marker: {}", Instant.now());
	}
}
