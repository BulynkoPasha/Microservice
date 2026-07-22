package com.example.microservice.service;

import com.example.microservice.dto.request.UserCreateRequestDto;
import com.example.microservice.dto.request.UserUpdateRequestDto;
import com.example.microservice.dto.request.filter.UserFilter;
import com.example.microservice.dto.response.UserResponseDto;
import com.example.microservice.dto.response.UserWithCardsResponse;
import org.springframework.data.domain.Page;

public interface UserService {

    UserResponseDto createUser(UserCreateRequestDto requestDto);

    UserResponseDto updateUser(Long id, UserUpdateRequestDto requestDto);

    UserResponseDto getUserById(Long id);

    UserResponseDto activateUser(Long id);

    UserResponseDto deactivateUser(Long id);

    Page<UserResponseDto> getAllUsers(UserFilter userFilter);

    UserWithCardsResponse getUserWithCards(Long id);

}
