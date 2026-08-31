package com.example.orderservice.service.impl;

import com.example.orderservice.client.UserServiceClient;
import com.example.orderservice.dto.filter.OrderFilter;
import com.example.orderservice.dto.request.OrderCreateRequestDto;
import com.example.orderservice.dto.request.OrderItemRequestDto;
import com.example.orderservice.dto.request.OrderUpdateRequestDto;
import com.example.orderservice.dto.response.OrderResponseDto;
import com.example.orderservice.dto.response.OrderWithUserResponseDto;
import com.example.orderservice.dto.response.UserInfoDto;
import com.example.orderservice.entity.Item;
import com.example.orderservice.entity.Order;
import com.example.orderservice.entity.OrderItem;
import com.example.orderservice.entity.OrderStatus;
import com.example.orderservice.exception.ResourceNotFoundException;
import com.example.orderservice.mapper.OrderMapper;
import com.example.orderservice.repository.ItemRepository;
import com.example.orderservice.repository.OrderRepository;
import com.example.orderservice.repository.specification.OrderSpecification;
import com.example.orderservice.service.OrderService;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ItemRepository itemRepository;
    private final OrderMapper orderMapper;
    private final UserServiceClient userServiceClient;

    @Override
    @Transactional
    public OrderWithUserResponseDto createOrder(OrderCreateRequestDto request, String authorizationHeader) {
        Order order = orderMapper.toEntity(request);
        order.setUserId(request.getUserId());
        order.setItems(new ArrayList<>());

        BigDecimal total = attachItems(order, request.getItems());
        order.setTotalPrice(total);

        Order saved = orderRepository.save(order);
        return buildResponse(saved, authorizationHeader);
    }

    @Override
    public OrderWithUserResponseDto getOrderById(Long id, String authorizationHeader) {
        Order order = findOrderOrThrow(id);
        return buildResponse(order, authorizationHeader);
    }

    @Override
    public Page<OrderWithUserResponseDto> getAllOrders(OrderFilter filter, String authorizationHeader) {
        if (!filter.isDateRangeValid()) {
            throw new IllegalArgumentException("createdFrom must not be after createdTo");
        }
        Page<Order> page = orderRepository.findAll(
                OrderSpecification.filterBy(filter.getCreatedFrom(), filter.getCreatedTo(), filter.getStatuses()),
                PageRequest.of(filter.getPage(), filter.getSize())
        );
        return page.map(order -> buildResponse(order, authorizationHeader));
    }

    @Override
    public List<OrderWithUserResponseDto> getOrdersByUserId(Long userId, String authorizationHeader) {
        return orderRepository.findByUserId(userId).stream()
                .map(order -> buildResponse(order, authorizationHeader))
                .toList();
    }

    @Override
    @Transactional
    public OrderWithUserResponseDto updateOrder(Long id, OrderUpdateRequestDto request, String authorizationHeader) {
        Order order = findOrderOrThrow(id);

        order.getItems().clear();
        BigDecimal total = attachItems(order, request.getItems());
        order.setTotalPrice(total);
        order.setStatus(request.getStatus());

        Order saved = orderRepository.save(order);
        return buildResponse(saved, authorizationHeader);
    }

    @Override
    @Transactional
    public void deleteOrder(Long id) {
        Order order = findOrderOrThrow(id);
        orderRepository.delete(order);
    }

    private Order findOrderOrThrow(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));
    }

    private BigDecimal attachItems(Order order, List<OrderItemRequestDto> itemRequests) {
        BigDecimal total = BigDecimal.ZERO;

        for (OrderItemRequestDto itemRequest : itemRequests) {
            Item item = itemRepository.findById(itemRequest.getItemId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Item not found with id: " + itemRequest.getItemId()));

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .item(item)
                    .quantity(itemRequest.getQuantity())
                    .build();
            order.getItems().add(orderItem);

            total = total.add(item.getPrice().multiply(BigDecimal.valueOf(itemRequest.getQuantity())));
        }

        return total;
    }

    private OrderWithUserResponseDto buildResponse(Order order, String authorizationHeader) {
        OrderResponseDto orderDto = orderMapper.toResponse(order);
        UserInfoDto userInfo = fetchUserInfoSafely(order.getUserId(), authorizationHeader);

        return OrderWithUserResponseDto.builder()
                .order(orderDto)
                .user(userInfo)
                .build();
    }

    private UserInfoDto fetchUserInfoSafely(Long userId, String authorizationHeader) {
        try {
            return userServiceClient.getUserInfo(userId, authorizationHeader);
        } catch (RestClientException | CallNotPermittedException ex) {
            log.warn("User Service unavailable, returning order without user info. userId={}", userId);
            return UserInfoDto.builder()
                    .id(userId)
                    .name("unavailable")
                    .surname("unavailable")
                    .email("unavailable")
                    .build();
        }
    }
}