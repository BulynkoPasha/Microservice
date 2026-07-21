package com.example.microservice.mapper;

import com.example.microservice.dto.request.UserCreateRequestDto;
import com.example.microservice.dto.request.UserUpdateRequestDto;
import com.example.microservice.dto.response.UserResponseDto;
import com.example.microservice.entity.User;
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
