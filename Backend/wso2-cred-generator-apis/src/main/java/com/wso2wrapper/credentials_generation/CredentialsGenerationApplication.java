package com.wso2wrapper.credentials_generation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

@EnableMongoAuditing
@SpringBootApplication(scanBasePackages = "com.wso2wrapper.credentials_generation")
public class CredentialsGenerationApplication {
	public static void main(String[] args) {
		SpringApplication.run(CredentialsGenerationApplication.class, args);
	}
}
