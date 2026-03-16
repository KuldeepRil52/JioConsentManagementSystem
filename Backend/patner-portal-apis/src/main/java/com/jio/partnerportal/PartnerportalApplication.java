package com.jio.partnerportal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableMongoAuditing
@EnableAsync
public class PartnerportalApplication {

	public static void main(String[] args) {
		SpringApplication.run(PartnerportalApplication.class, args);
	}

}
