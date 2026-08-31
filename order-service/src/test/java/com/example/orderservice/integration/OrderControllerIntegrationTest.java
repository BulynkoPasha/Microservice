package com.example.orderservice.integration;

import com.example.orderservice.dto.request.OrderCreateRequestDto;
import com.example.orderservice.dto.request.OrderItemRequestDto;
import com.example.orderservice.entity.Item;
import com.example.orderservice.repository.ItemRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

class OrderControllerIntegrationTest extends AbstractIntegrationTest {

    private static final String RANDOM_TEST_TOKEN = "random-token-for-tests-not-a-real-credential";

    @Autowired
    private ItemRepository itemRepository;

    private HttpEntity<OrderCreateRequestDto> requestWithAuth(OrderCreateRequestDto body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(RANDOM_TEST_TOKEN);
        return new HttpEntity<>(body, headers);
    }

    private void stubUserServiceSuccess(Long userId) {
        wireMockServer.stubFor(get(urlEqualTo("/api/v1/users/" + userId))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"id": %d, "name": "Ivan", "surname": "Petrov", "email": "ivan@test.com"}
                                """.formatted(userId))));
    }

    private void stubUserServiceFailure(Long userId) {
        wireMockServer.stubFor(get(urlEqualTo("/api/v1/users/" + userId))
                .willReturn(aResponse().withStatus(500)));
    }

    @Test
    void createOrder_shouldReturn201WithOrderAndUserInfo_whenUserServiceRespondsSuccessfully() {
        Item item = itemRepository.save(Item.builder().name("Widget").price(BigDecimal.valueOf(15)).build());
        stubUserServiceSuccess(7L);

        OrderCreateRequestDto request = OrderCreateRequestDto.builder()
                .userId(7L)
                .items(List.of(OrderItemRequestDto.builder().itemId(item.getId()).quantity(2).build()))
                .build();

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/orders", HttpMethod.POST, requestWithAuth(request), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Map<String, Object> order = (Map<String, Object>) response.getBody().get("order");
        Map<String, Object> user = (Map<String, Object>) response.getBody().get("user");

        assertThat(order.get("totalPrice")).isEqualTo(30.0);
        assertThat(user.get("email")).isEqualTo("ivan@test.com");
    }

    @Test
    void createOrder_shouldReturnPlaceholderUser_whenUserServiceIsDown() {
        Item item = itemRepository.save(Item.builder().name("Gadget").price(BigDecimal.valueOf(20)).build());
        stubUserServiceFailure(8L);

        OrderCreateRequestDto request = OrderCreateRequestDto.builder()
                .userId(8L)
                .items(List.of(OrderItemRequestDto.builder().itemId(item.getId()).quantity(1).build()))
                .build();

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/orders", HttpMethod.POST, requestWithAuth(request), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        Map<String, Object> user = (Map<String, Object>) response.getBody().get("user");
        assertThat(user).isNotNull();
        assertThat(user.get("email")).isEqualTo("unavailable");

        Map<String, Object> order = (Map<String, Object>) response.getBody().get("order");
        assertThat(order.get("totalPrice")).isEqualTo(20.0);
    }

    @Test
    void createOrder_shouldReturn404_whenItemMissing() {
        stubUserServiceSuccess(9L);

        OrderCreateRequestDto request = OrderCreateRequestDto.builder()
                .userId(9L)
                .items(List.of(OrderItemRequestDto.builder().itemId(999999L).quantity(1).build()))
                .build();

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/orders", HttpMethod.POST, requestWithAuth(request), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void deleteOrder_shouldReturn204_andSoftDeleteOrder() {
        Item item = itemRepository.save(Item.builder().name("Thing").price(BigDecimal.valueOf(5)).build());
        stubUserServiceSuccess(10L);

        OrderCreateRequestDto createRequest = OrderCreateRequestDto.builder()
                .userId(10L)
                .items(List.of(OrderItemRequestDto.builder().itemId(item.getId()).quantity(1).build()))
                .build();

        ResponseEntity<Map> created = restTemplate.exchange(
                "/api/v1/orders", HttpMethod.POST, requestWithAuth(createRequest), Map.class);
        Map<String, Object> order = (Map<String, Object>) created.getBody().get("order");
        Long orderId = ((Number) order.get("id")).longValue();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("test-token");
        restTemplate.exchange("/api/v1/orders/" + orderId, HttpMethod.DELETE,
                new HttpEntity<>(headers), Void.class);

        ResponseEntity<Map> afterDelete = restTemplate.exchange(
                "/api/v1/orders/" + orderId, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
        assertThat(afterDelete.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}