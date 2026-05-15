package com.classpet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableScheduling
@ComponentScan(basePackages = "com.classpet")
public class ClassPetGardenApplication {
    public static void main(String[] args) {
        SpringApplication.run(ClassPetGardenApplication.class, args);
    }
}