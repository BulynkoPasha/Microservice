package com.example.microservice.service;

import com.example.microservice.dto.request.UserCreateRequestDto;
import com.example.microservice.dto.request.UserUpdateRequestDto;
import com.example.microservice.dto.request.filter.UserFilter;
import com.example.microservice.dto.response.PaymentCardResponseDto;
import com.example.microservice.dto.response.UserResponseDto;
import com.example.microservice.dto.response.UserWithCardsResponse;
import com.example.microservice.entity.User;
import com.example.microservice.exception.DuplicateEmailException;
import com.example.microservice.exception.ResourceNotFoundException;
import com.example.microservice.mapper.PaymentCardMapper;
import com.example.microservice.mapper.UserMapper;
import com.example.microservice.repository.PaymentCardRepository;
import com.example.microservice.repository.UserRepository;
import com.example.microservice.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PaymentCardRepository paymentCardRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PaymentCardMapper paymentCardMapper;

    @InjectMocks
    private UserServiceImpl userService;

    private User user;
    private UserResponseDto userResponse;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .name("Ivan")
                .surname("Petrov")
                .email("ivan@test.com")
                .birthDate(LocalDate.of(1999, 1, 1))
                .active(true)
                .build();

        userResponse = UserResponseDto.builder()
                .id(1L)
                .name("Ivan")
                .surname("Petrov")
                .email("ivan@test.com")
                .active(true)
                .build();
    }

    @Test
    void createUser_shouldSaveAndReturnResponse_whenEmailNotTaken() {
        UserCreateRequestDto request = UserCreateRequestDto.builder()
                .name("Ivan").surname("Petrov").email("ivan@test.com")
                .birthDate(LocalDate.of(1999, 1, 1))
                .build();

        when(userRepository.existsById(1L)).thenReturn(false);
        when(userRepository.existsByEmail("ivan@test.com")).thenReturn(false);
        when(userMapper.toEntity(request)).thenReturn(user);
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toDto(user)).thenReturn(userResponse);

        UserResponseDto result = userService.createUser(request, 1L);

        assertThat(result.getEmail()).isEqualTo("ivan@test.com");
        verify(userRepository).save(user);
    }

    @Test
    void createUser_shouldThrowDuplicateEmailException_whenEmailAlreadyExists() {
        UserCreateRequestDto request = UserCreateRequestDto.builder()
                .name("Ivan").surname("Petrov").email("ivan@test.com")
                .build();

        when(userRepository.existsById(1L)).thenReturn(false);
        when(userRepository.existsByEmail("ivan@test.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(request, 1L))
                .isInstanceOf(DuplicateEmailException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void getUserById_shouldReturnResponse_whenUserExists() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userMapper.toDto(user)).thenReturn(userResponse);

        UserResponseDto result = userService.getUserById(1L);

        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void getUserById_shouldThrowResourceNotFoundException_whenUserMissing() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getAllUsers_shouldReturnPagedResponses() {
        UserFilter filter = UserFilter.builder().name("Ivan").page(0).size(20).build();
        Page<User> userPage = new PageImpl<>(List.of(user), PageRequest.of(0, 20), 1);

        when(userRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(userPage);
        when(userMapper.toDto(user)).thenReturn(userResponse);

        Page<UserResponseDto> result = userService.getAllUsers(filter);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getEmail()).isEqualTo("ivan@test.com");
    }

    @Test
    void updateUser_shouldUpdateAndReturnResponse_whenUserExists() {
        UserUpdateRequestDto request = UserUpdateRequestDto.builder()
                .name("Petr").surname("Petrov").email("ivan@test.com")
                .birthDate(LocalDate.of(1990, 1, 1))
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toDto(user)).thenReturn(userResponse);

        UserResponseDto result = userService.updateUser(1L, request);

        verify(userMapper).updateEntityFromRequest(request, user);
        assertThat(result).isNotNull();
    }

    @Test
    void updateUser_shouldThrowResourceNotFoundException_whenUserMissing() {
        UserUpdateRequestDto request = UserUpdateRequestDto.builder().build();
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUser(99L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void activateUser_shouldSetActiveTrue() {
        user.setActive(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userMapper.toDto(user)).thenReturn(userResponse);

        userService.activateUser(1L);

        assertThat(user.isActive()).isTrue();
    }

    @Test
    void deactivateUser_shouldSetActiveFalse() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userMapper.toDto(user)).thenReturn(userResponse);

        userService.deactivateUser(1L);

        assertThat(user.isActive()).isFalse();
    }

    @Test
    void getUserWithCards_shouldReturnUserWithMappedCards() {
        PaymentCardResponseDto cardResponse = PaymentCardResponseDto.builder().id(10L).userId(1L).build();
        UserWithCardsResponse expected = UserWithCardsResponse.builder()
                .id(1L).cards(List.of(cardResponse)).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(paymentCardRepository.findByUserId(1L)).thenReturn(List.of());
        when(userMapper.toWithCardsResponse(eq(user), anyList())).thenReturn(expected);

        UserWithCardsResponse result = userService.getUserWithCards(1L);

        assertThat(result.getCards()).hasSize(1);
    }

    @Test
    void getUserWithCards_shouldThrowResourceNotFoundException_whenUserMissing() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserWithCards(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}