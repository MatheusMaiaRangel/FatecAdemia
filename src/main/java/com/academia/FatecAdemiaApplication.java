package com.academia;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class FatecAdemiaApplication {

    public static void main(String[] args) {
        SpringApplication.run(FatecAdemiaApplication.class, args);
    }
}
