package com.aivideo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AivideoApplication {
    public static void main(String[] args) {
        SpringApplication.run(AivideoApplication.class, args);
    }
}
