package com.example.paymentservice.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class RandomNumberClient {

    private static final String RANDOM_ORG_URL =
            "https://www.random.org/integers/?num=1&min=1&max=100&col=1&base=10&format=plain&rnd=new";

    private final RestClient restClient;

    public RandomNumberClient() {
        this.restClient = RestClient.builder().build();
    }

    public int generateRandomNumber() {
        try {
            String response = restClient.get()
                    .uri(RANDOM_ORG_URL)
                    .retrieve()
                    .body(String.class);
            return Integer.parseInt(response.trim());
        } catch (Exception ex) {
            log.warn("Failed to fetch random number from external API, falling back to local random", ex);
            return (int) (Math.random() * 100) + 1;
        }
    }
}