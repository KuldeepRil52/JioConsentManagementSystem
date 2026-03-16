package com.jio.digigov.fides;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableMongoAuditing
@EnableAsync
public class SystemRegistryApplication {
    public static void main(String[] args) {
        SpringApplication.run(SystemRegistryApplication.class, args);
    }
}
