package com.exmple.microservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

// Class for enable audit entity class
@Configuration
@EnableJpaAuditing
public class JpaAuditConfig {
}
