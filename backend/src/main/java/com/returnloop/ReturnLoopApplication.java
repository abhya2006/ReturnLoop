package com.returnloop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * ReturnLoop - a reverse logistics and circular commerce platform.
 *
 * <p>The application is a modular monolith: each bounded context lives under
 * {@code com.returnloop.<context>} and follows Controller -> Service -> Repository boundaries.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class ReturnLoopApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReturnLoopApplication.class, args);
    }
}
