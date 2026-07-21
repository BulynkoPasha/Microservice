package com.exmple.microservice.dto.service;

import com.exmple.microservice.dto.request.UserCreateRequestDto;
import com.exmple.microservice.dto.request.UserUpdateRequestDto;
import com.exmple.microservice.dto.request.filter.UserFilter;
import com.exmple.microservice.dto.response.UserResponseDto;
import org.springframework.data.domain.Page;

public interface UserService {

    UserResponseDto createUser(UserCreateRequestDto requestDto);

    UserResponseDto updateUser(Long id, UserUpdateRequestDto requestDto);

    UserResponseDto getUserById(Long id);

    UserResponseDto activateUser(Long id);

    UserResponseDto deactivateUser(Long id);

    Page<UserResponseDto> getAllUsers(UserFilter userFilter);

}
