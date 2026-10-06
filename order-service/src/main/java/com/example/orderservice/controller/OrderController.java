package com.example.orderservice.controller;

import com.example.orderservice.dto.filter.OrderFilter;
import com.example.orderservice.dto.request.OrderCreateRequestDto;
import com.example.orderservice.dto.request.OrderUpdateRequestDto;
import com.example.orderservice.dto.response.OrderWithUserResponseDto;
import com.example.orderservice.entity.OrderStatus;
import com.example.orderservice.service.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Validated
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderWithUserResponseDto> createOrder(
            @Valid @RequestBody OrderCreateRequestDto request,
            @RequestHeader("Authorization") String authorizationHeader) {
        OrderWithUserResponseDto response = orderService.createOrder(request, authorizationHeader);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderWithUserResponseDto> getOrderById(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authorizationHeader) {

        return ResponseEntity.ok(orderService.getOrderById(id, authorizationHeader));
    }

    @GetMapping
    public ResponseEntity<Page<OrderWithUserResponseDto>> getAllOrders(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdTo,
            @RequestParam(required = false) List<OrderStatus> statuses,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestHeader("Authorization") String authorizationHeader) {

        OrderFilter filter = OrderFilter.builder()
                .createdFrom(createdFrom)
                .createdTo(createdTo)
                .statuses(statuses)
                .page(page)
                .size(size)
                .build();

        return ResponseEntity.ok(orderService.getAllOrders(filter, authorizationHeader));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<OrderWithUserResponseDto>> getOrdersByUserId(
            @PathVariable Long userId,
            @RequestHeader("Authorization") String authorizationHeader) {
        return ResponseEntity.ok(orderService.getOrdersByUserId(userId, authorizationHeader));
    }

    @PutMapping("/{id}")
    public ResponseEntity<OrderWithUserResponseDto> updateOrder(
            @PathVariable Long id,
            @Valid @RequestBody OrderUpdateRequestDto request,
            @RequestHeader("Authorization") String authorizationHeader) {
        return ResponseEntity.ok(orderService.updateOrder(id, request, authorizationHeader));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long id) {
        orderService.deleteOrder(id);
        return ResponseEntity.noContent().build();
    }
}
