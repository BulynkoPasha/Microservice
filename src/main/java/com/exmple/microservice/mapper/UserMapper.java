package com.exmple.microservice.mapper;

import com.exmple.microservice.dto.request.UserCreateRequestDto;
import com.exmple.microservice.dto.request.UserUpdateRequestDto;
import com.exmple.microservice.dto.response.UserResponseDto;
import com.exmple.microservice.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "active", constant = "true")
    User toEntity(UserCreateRequestDto requestDto);

    UserResponseDto toDto(User user);

    void updateEntityFromRequest(UserUpdateRequestDto requestDto, @MappingTarget User user);
}
