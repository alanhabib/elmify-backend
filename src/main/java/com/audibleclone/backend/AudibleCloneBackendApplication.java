package com.audibleclone.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * AudibleClone Backend Application
 * 
 * Spring Boot application migrated from Cloudflare Workers + D1 
 * to PostgreSQL + R2 storage with Clerk authentication.
 * 
 * @author Migration Tool
 * @version 1.0.0
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableTransactionManagement
public class AudibleCloneBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(AudibleCloneBackendApplication.class, args);
    }
}