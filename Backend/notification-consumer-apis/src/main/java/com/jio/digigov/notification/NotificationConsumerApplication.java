package com.jio.digigov.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import jakarta.annotation.PostConstruct;
import org.springframework.scheduling.annotation.EnableScheduling;

@Slf4j
@SpringBootApplication
@EnableScheduling
public class NotificationConsumerApplication {
    public static void main(String[] args) {
        SpringApplication.run(NotificationConsumerApplication.class, args);
    }
}