package com.jio.multitranslator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

@SpringBootApplication
@EnableMongoAuditing
@EnableCaching
public class MultitranslatorApplication {

	public static void main(String[] args) {
		SpringApplication.run(MultitranslatorApplication.class, args);
	}

}
