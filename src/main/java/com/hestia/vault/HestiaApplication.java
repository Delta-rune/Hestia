package com.hestia.vault;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HestiaApplication {

    public static void main(String[] args) {
        SpringApplication.run(HestiaApplication.class, args);
    }

}
