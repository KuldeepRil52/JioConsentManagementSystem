package com.jio.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.http.client.HttpClientAutoConfiguration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = {
        org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration.class,
        org.springframework.boot.autoconfigure.http.client.HttpClientAutoConfiguration.class
})
@EnableMongoRepositories(basePackages = "com.jio.auth.repository")
@EnableScheduling
@EnableMongoAuditing
public class AuthApplication {
	public static void main(String[] args) {
		SpringApplication.run(AuthApplication.class, args);
	}

}
