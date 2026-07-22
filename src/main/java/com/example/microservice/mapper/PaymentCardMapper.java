package com.example.microservice.mapper;

import com.example.microservice.dto.request.PaymentCardCreateRequestDto;
import com.example.microservice.dto.request.PaymentCardUpdateRequestDto;
import com.example.microservice.dto.response.PaymentCardResponseDto;
import com.example.microservice.entity.PaymentCard;
import lombok.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface PaymentCardMapper {

    @Mapping(target = "active", constant = "true")
    @Mapping(target = "user", ignore = true)
    PaymentCard toEntity(PaymentCardCreateRequestDto request);

    @Mapping(target = "userId", source = "user.id")
    PaymentCardResponseDto toDto(PaymentCard card);

    void updateEntityFromRequest(PaymentCardUpdateRequestDto request, @MappingTarget PaymentCard card);

}
