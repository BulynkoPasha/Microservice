package com.example.orderservice.service;

import com.example.orderservice.dto.filter.OrderFilter;
import com.example.orderservice.dto.request.OrderCreateRequestDto;
import com.example.orderservice.dto.request.OrderUpdateRequestDto;
import com.example.orderservice.dto.response.OrderWithUserResponseDto;
import org.springframework.data.domain.Page;

import java.util.List;

public interface OrderService {

    OrderWithUserResponseDto createOrder(OrderCreateRequestDto request, String authorizationHeader);

    OrderWithUserResponseDto getOrderById(Long id, String authorizationHeader);

    Page<OrderWithUserResponseDto> getAllOrders(OrderFilter filter, String authorizationHeader);

    List<OrderWithUserResponseDto> getOrdersByUserId(Long userId, String authorizationHeader);

    OrderWithUserResponseDto updateOrder(Long id, OrderUpdateRequestDto request, String authorizationHeader);

    void deleteOrder(Long id);
}