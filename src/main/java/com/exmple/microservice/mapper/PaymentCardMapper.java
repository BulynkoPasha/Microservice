package com.exmple.microservice.mapper;

import com.exmple.microservice.dto.request.PaymentCardCreateRequestDto;
import com.exmple.microservice.dto.request.PaymentCardUpdateRequestDto;
import com.exmple.microservice.dto.response.PaymentCardResponseDto;
import com.exmple.microservice.entity.PaymentCard;
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
