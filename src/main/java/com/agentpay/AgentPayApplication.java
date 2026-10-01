package com.agentpay;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AgentPayApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgentPayApplication.class, args);
    }
}
