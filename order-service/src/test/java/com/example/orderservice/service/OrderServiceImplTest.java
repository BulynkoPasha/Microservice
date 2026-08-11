package com.example.orderservice.service;

import com.example.orderservice.client.UserServiceClient;
import com.example.orderservice.dto.filter.OrderFilter;
import com.example.orderservice.dto.request.OrderCreateRequestDto;
import com.example.orderservice.dto.request.OrderItemRequestDto;
import com.example.orderservice.dto.request.OrderUpdateRequestDto;
import com.example.orderservice.dto.response.OrderResponseDto;
import com.example.orderservice.dto.response.UserInfoDto;
import com.example.orderservice.entity.Item;
import com.example.orderservice.entity.Order;
import com.example.orderservice.entity.OrderStatus;
import com.example.orderservice.exception.ResourceNotFoundException;
import com.example.orderservice.mapper.OrderMapper;
import com.example.orderservice.repository.ItemRepository;
import com.example.orderservice.repository.OrderRepository;
import com.example.orderservice.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private UserServiceClient userServiceClient;

    @InjectMocks
    private OrderServiceImpl orderService;

    private Item item;
    private Order order;
    private OrderResponseDto orderResponse;
    private UserInfoDto userInfo;
    private static final String AUTH_HEADER = "Bearer test-token";

    @BeforeEach
    void setUp() {
        item = Item.builder().id(1L).name("Widget").price(BigDecimal.valueOf(10)).build();

        order = Order.builder()
                .id(1L).userId(5L).status(OrderStatus.CREATED)
                .totalPrice(BigDecimal.valueOf(20))
                .items(new java.util.ArrayList<>())
                .build();

        orderResponse = OrderResponseDto.builder().id(1L).userId(5L).status(OrderStatus.CREATED).build();
        userInfo = UserInfoDto.builder().id(5L).name("Ivan").surname("Petrov").email("ivan@test.com").build();
    }

    @Test
    void createOrder_shouldCalculateTotalPriceAndReturnResponseWithUser() {
        OrderCreateRequestDto request = OrderCreateRequestDto.builder()
                .userId(5L)
                .items(List.of(OrderItemRequestDto.builder().itemId(1L).quantity(2).build()))
                .build();

        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(orderMapper.toResponse(order)).thenReturn(orderResponse);
        when(userServiceClient.getUserInfo(5L, AUTH_HEADER)).thenReturn(userInfo);

        var result = orderService.createOrder(request, AUTH_HEADER);

        assertThat(result.getOrder()).isEqualTo(orderResponse);
        assertThat(result.getUser()).isEqualTo(userInfo);
    }

    @Test
    void createOrder_shouldThrowResourceNotFoundException_whenItemMissing() {
        OrderCreateRequestDto request = OrderCreateRequestDto.builder()
                .userId(5L)
                .items(List.of(OrderItemRequestDto.builder().itemId(99L).quantity(1).build()))
                .build();

        when(itemRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.createOrder(request, AUTH_HEADER))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createOrder_shouldReturnNullUser_whenUserServiceUnavailable() {
        OrderCreateRequestDto request = OrderCreateRequestDto.builder()
                .userId(5L)
                .items(List.of(OrderItemRequestDto.builder().itemId(1L).quantity(1).build()))
                .build();

        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(orderMapper.toResponse(order)).thenReturn(orderResponse);
        when(userServiceClient.getUserInfo(5L, AUTH_HEADER))
                .thenThrow(new org.springframework.web.client.ResourceAccessException("Connection refused"));

        var result = orderService.createOrder(request, AUTH_HEADER);

        assertThat(result.getOrder()).isEqualTo(orderResponse);
        assertThat(result.getUser()).isNull();
    }

    @Test
    void getOrderById_shouldReturnResponse_whenOrderExists() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderMapper.toResponse(order)).thenReturn(orderResponse);
        when(userServiceClient.getUserInfo(5L, AUTH_HEADER)).thenReturn(userInfo);

        var result = orderService.getOrderById(1L, AUTH_HEADER);

        assertThat(result.getOrder().getId()).isEqualTo(1L);
        assertThat(result.getUser().getEmail()).isEqualTo("ivan@test.com");
    }

    @Test
    void getOrderById_shouldThrowResourceNotFoundException_whenOrderMissing() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderById(99L, AUTH_HEADER))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getAllOrders_shouldReturnPagedResponses() {
        OrderFilter filter = OrderFilter.builder().page(0).size(20).build();
        Page<Order> orderPage = new PageImpl<>(List.of(order), PageRequest.of(0, 20), 1);

        when(orderRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(orderPage);
        when(orderMapper.toResponse(order)).thenReturn(orderResponse);
        when(userServiceClient.getUserInfo(5L, AUTH_HEADER)).thenReturn(userInfo);

        Page<?> result = orderService.getAllOrders(filter, AUTH_HEADER);

        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void getOrdersByUserId_shouldReturnMappedList() {
        when(orderRepository.findByUserId(5L)).thenReturn(List.of(order));
        when(orderMapper.toResponse(order)).thenReturn(orderResponse);
        when(userServiceClient.getUserInfo(5L, AUTH_HEADER)).thenReturn(userInfo);

        var result = orderService.getOrdersByUserId(5L, AUTH_HEADER);

        assertThat(result).hasSize(1);
    }

    @Test
    void updateOrder_shouldRecalculateTotalAndUpdateStatus() {
        OrderUpdateRequestDto request = OrderUpdateRequestDto.builder()
                .status(OrderStatus.PAID)
                .items(List.of(OrderItemRequestDto.builder().itemId(1L).quantity(3).build()))
                .build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(orderRepository.save(order)).thenReturn(order);
        when(orderMapper.toResponse(order)).thenReturn(orderResponse);
        when(userServiceClient.getUserInfo(5L, AUTH_HEADER)).thenReturn(userInfo);

        orderService.updateOrder(1L, request, AUTH_HEADER);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(order.getTotalPrice()).isEqualByComparingTo(BigDecimal.valueOf(30));
    }

    @Test
    void updateOrder_shouldThrowResourceNotFoundException_whenOrderMissing() {
        OrderUpdateRequestDto request = OrderUpdateRequestDto.builder()
                .status(OrderStatus.PAID)
                .items(List.of(OrderItemRequestDto.builder().itemId(1L).quantity(1).build()))
                .build();

        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.updateOrder(99L, request, AUTH_HEADER))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteOrder_shouldCallRepositoryDelete() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        orderService.deleteOrder(1L);

        org.mockito.Mockito.verify(orderRepository).delete(order);
    }

    @Test
    void deleteOrder_shouldThrowResourceNotFoundException_whenOrderMissing() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.deleteOrder(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}