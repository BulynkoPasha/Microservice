package com.example.orderservice.mapper;

import com.example.orderservice.dto.request.OrderCreateRequestDto;
import com.example.orderservice.dto.response.OrderItemResponseDto;
import com.example.orderservice.dto.response.OrderResponseDto;
import com.example.orderservice.entity.Order;
import com.example.orderservice.entity.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", constant = "CREATED")
    @Mapping(target = "totalPrice", ignore = true)
    @Mapping(target = "items", ignore = true)
    Order toEntity(OrderCreateRequestDto request);

    @Mapping(target = "itemId", source = "item.id")
    @Mapping(target = "itemName", source = "item.name")
    @Mapping(target = "itemPrice", source = "item.price")
    OrderItemResponseDto toItemResponse(OrderItem orderItem);

    List<OrderItemResponseDto> toItemResponseList(List<OrderItem> items);

    OrderResponseDto toResponse(Order order);
}