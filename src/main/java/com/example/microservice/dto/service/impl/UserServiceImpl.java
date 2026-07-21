package com.example.microservice.dto.service.impl;

import com.example.microservice.dto.request.UserCreateRequestDto;
import com.example.microservice.dto.request.UserUpdateRequestDto;
import com.example.microservice.dto.request.filter.UserFilter;
import com.example.microservice.dto.response.UserResponseDto;
import com.example.microservice.dto.service.UserService;
import com.example.microservice.entity.User;
import com.example.microservice.exception.DuplicateEmailException;
import com.example.microservice.exception.ResourceNotFoundException;
import com.example.microservice.mapper.UserMapper;
import com.example.microservice.repository.UserRepository;
import com.example.microservice.repository.specification.UserSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    public UserResponseDto createUser(UserCreateRequestDto requestDto) {
        if (userRepository.existsByEmail(requestDto.getEmail())) {
            throw new DuplicateEmailException("User with email " + requestDto.getEmail() + " already exists");
        }
        User user = userMapper.toEntity(requestDto);
        User saved = userRepository.save(user);
        return userMapper.toDto(saved);
    }

    @Override
    public UserResponseDto updateUser(Long id, UserUpdateRequestDto requestDto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        userMapper.updateEntityFromRequest(requestDto, user);
        User saved = userRepository.save(user);
        return userMapper.toDto(saved);
    }

    @Override
    public UserResponseDto getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return userMapper.toDto(user);
    }

    @Override
    public UserResponseDto activateUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        user.setActive(true);
        User saved = userRepository.save(user);
        return userMapper.toDto(saved);
    }

    @Override
    public UserResponseDto deactivateUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        user.setActive(false);
        User saved = userRepository.save(user);
        return userMapper.toDto(saved);
    }

    @Override
    public Page<UserResponseDto> getAllUsers(UserFilter userFilter) {
        Page<User> page = userRepository.findAll(
                UserSpecification.filterBy(userFilter.getName(), userFilter.getSurname()),
                PageRequest.of(userFilter.getPage(), userFilter.getSize())
        );
        return page.map(userMapper::toDto);
    }
}
