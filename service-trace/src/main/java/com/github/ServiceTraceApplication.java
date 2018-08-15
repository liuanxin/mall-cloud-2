package com.github;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import zipkin.server.EnableZipkinServer;

@SuppressWarnings("deprecation")
@EnableZipkinServer
@SpringBootApplication
public class ServiceTraceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServiceTraceApplication.class, args);
    }
}
