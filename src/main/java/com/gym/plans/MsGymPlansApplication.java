package com.gym.plans;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.gym.plans", "com.gym.common"})
public class MsGymPlansApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsGymPlansApplication.class, args);
    }
}
