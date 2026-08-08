package com.example.microservice.integration;

import com.example.microservice.repository.PaymentCardRepository;
import com.example.microservice.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractIntegrationTest {

    protected static final String TEST_JWT_SECRET =
            "test-secret-key-for-integration-tests-minimum-32-chars";

    static final PostgreSQLContainer<?> postgres;
    static final GenericContainer<?> redis;

    static {
        postgres = new PostgreSQLContainer<>("postgres:16")
                .withDatabaseName("user_service_test_db")
                .withUsername("test")
                .withPassword("test");
        postgres.start();

        redis = new GenericContainer<>(DockerImageName.parse("redis:7"))
                .withExposedPorts(6379);
        redis.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("jwt.secret", () -> TEST_JWT_SECRET);
    }

    @Autowired
    protected TestRestTemplate restTemplate;

    @Autowired
    protected PaymentCardRepository paymentCardRepository;

    @Autowired
    protected UserRepository userRepository;

    @BeforeEach
    void cleanDatabase() {
        paymentCardRepository.deleteAll();
        userRepository.deleteAll();
    }

    protected String generateTestToken(Long userId, String role) {
        SecretKey key = Keys.hmacShaKeyFor(TEST_JWT_SECRET.getBytes(StandardCharsets.UTF_8));
        Date now = new Date();
        Date expiry = new Date(now.getTime() + 900_000);

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .issuer("auth-service")
                .subject(String.valueOf(userId))
                .claim("role", role)
                .claim("type", "ACCESS")
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    protected HttpEntity<Void> authHeader(Long userId, String role) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(generateTestToken(userId, role));
        return new HttpEntity<>(headers);
    }

    protected <T> HttpEntity<T> authHeader(T body, Long userId, String role) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(generateTestToken(userId, role));
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }
}