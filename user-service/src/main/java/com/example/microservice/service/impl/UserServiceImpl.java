package com.example.microservice.service.impl;

import com.example.microservice.config.CacheNames;
import com.example.microservice.dto.request.UserCreateRequestDto;
import com.example.microservice.dto.request.UserUpdateRequestDto;
import com.example.microservice.dto.request.filter.PaymentCardFilter;
import com.example.microservice.dto.request.filter.UserFilter;
import com.example.microservice.dto.response.PaymentCardResponseDto;
import com.example.microservice.dto.response.UserResponseDto;
import com.example.microservice.dto.response.UserWithCardsResponse;
import com.example.microservice.entity.PaymentCard;
import com.example.microservice.mapper.PaymentCardMapper;
import com.example.microservice.repository.PaymentCardRepository;
import com.example.microservice.repository.specification.PaymentCardSpecification;
import com.example.microservice.service.UserService;
import com.example.microservice.entity.User;
import com.example.microservice.exception.DuplicateEmailException;
import com.example.microservice.exception.ResourceNotFoundException;
import com.example.microservice.mapper.UserMapper;
import com.example.microservice.repository.UserRepository;
import com.example.microservice.repository.specification.UserSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PaymentCardRepository paymentCardRepository;
    private final PaymentCardMapper paymentCardMapper;

    @Override
    public UserResponseDto createUser(UserCreateRequestDto requestDto, Long authId) {
        if (userRepository.existsById(authId)) {
            throw new IllegalStateException("Profile already exists for this user");
        }
        if (userRepository.existsByEmail(requestDto.getEmail())) {
            throw new DuplicateEmailException("User with email " + requestDto.getEmail() + " already exists");
        }
        User user = userMapper.toEntity(requestDto);
        user.setId(authId);
        User saved = userRepository.save(user);
        return userMapper.toDto(saved);
    }

    @Override
    @Transactional
    @CacheEvict(value = CacheNames.USERS_WITH_CARDS, key = "#id")
    public UserResponseDto updateUser(Long id, UserUpdateRequestDto requestDto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        if (!user.getEmail().equalsIgnoreCase(requestDto.getEmail())
                && userRepository.existsByEmail(requestDto.getEmail())) {
            throw new DuplicateEmailException("Email already taken: " + requestDto.getEmail());
        }

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
    @Transactional
    @CacheEvict(value = CacheNames.USERS_WITH_CARDS, key = "#id")
    public UserResponseDto activateUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        userRepository.updateActiveStatus(id, true);
        user.setActive(true);
        return userMapper.toDto(user);
    }

    @Override
    @Transactional
    @CacheEvict(value = CacheNames.USERS_WITH_CARDS, key = "#id")
    public UserResponseDto deactivateUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        userRepository.updateActiveStatus(id, false);
        user.setActive(false);
        return userMapper.toDto(user);
    }

    @Override
    public Page<UserResponseDto> getAllUsers(UserFilter userFilter) {
        Page<User> page = userRepository.findAll(
                UserSpecification.filterBy(userFilter.getName(), userFilter.getSurname()),
                PageRequest.of(userFilter.getPage(), userFilter.getSize())
        );
        return page.map(userMapper::toDto);
    }

    @Override
    @Cacheable(value = CacheNames.USERS_WITH_CARDS, key = "#id")
    public UserWithCardsResponse getUserWithCards(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        List<PaymentCardResponseDto> cards = paymentCardRepository.findByUserId(id).stream()
                .map(paymentCardMapper::toDto)
                .toList();

        return userMapper.toWithCardsResponse(user, cards);
    }

    @Override
    public UserResponseDto getActiveUserById(Long id) {
        User user = userRepository.findActiveUserById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Active user not found with id: " + id));
        return userMapper.toDto(user);
    }

    @Override
    @Transactional
    @CacheEvict(value = CacheNames.USERS_WITH_CARDS, key = "#id")
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        userRepository.delete(user);
    }

    @Override
    public Page<PaymentCardResponseDto> getAllCards(PaymentCardFilter filter) {
        Page<PaymentCard> page = paymentCardRepository.findAll(
                PaymentCardSpecification.filterByOwnerName(filter.getOwnerName(), filter.getOwnerSurname()),
                PageRequest.of(filter.getPage(), filter.getSize())
        );
        return page.map(paymentCardMapper::toDto);
    }

}
