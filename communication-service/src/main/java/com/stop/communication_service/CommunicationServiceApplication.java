package com.stop.communication_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class CommunicationServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(CommunicationServiceApplication.class, args);
	}

}
