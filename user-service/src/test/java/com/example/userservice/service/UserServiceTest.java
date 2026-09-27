package com.example.userservice.service;

import com.example.userservice.dto.PaginationRequest;
import com.example.userservice.dto.UserRequest;
import com.example.userservice.dto.UserResponse;
import com.example.userservice.entity.UserEntity;
import com.example.userservice.exception.UserAlreadyExistsException;
import com.example.userservice.exception.UserNotFoundException;
import com.example.userservice.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void createUser_successfully() {
        UserRequest userRequest = new UserRequest("Dave", "dave@example.com", 40);
        UserEntity savedUser = new UserEntity("Dave", "dave@example.com", 40);
        savedUser.setId(1);

        when(userRepository.existsByEmail(userRequest.email())).thenReturn(false);
        when(userRepository.save(any(UserEntity.class))).thenReturn(savedUser);

        UserResponse userResponse = userService.createUser(userRequest);
        assertNotNull(userResponse);
        assertEquals(userRequest.name(), userResponse.name());
        assertEquals(userRequest.email(), userResponse.email());
        assertEquals(userRequest.age(), userResponse.age());
        verify(userRepository).existsByEmail(userRequest.email());
        verify(userRepository).save(any(UserEntity.class));
    }

    @Test
    void createUser_ifEmailIsAlreadyOccupied() {
        UserRequest userRequest = new UserRequest("Dave", "bob@example.com", 40);

        when(userRepository.existsByEmail(userRequest.email())).thenReturn(true);

        UserAlreadyExistsException exception = assertThrows(
                UserAlreadyExistsException.class,
                () -> userService.createUser(userRequest)
        );
        assertEquals("Пользователь с таким mail bob@example.com уже существует", exception.getMessage());
        verify(userRepository).existsByEmail(userRequest.email());
        verify(userRepository, never()).save(any(UserEntity.class));
    }

    @Test
    void updateUser_ifUserExists() {
        Integer id = 1;
        UserRequest userRequest = new UserRequest("Alice Updated", "alice.new@example.com", 25);
        UserEntity existingUser = new UserEntity("Alice", "alice@example.com", 24);
        existingUser.setId(id);
        UserEntity updatedUser = new UserEntity("Alice Updated", "alice.new@example.com", 25);
        updatedUser.setId(id);

        when(userRepository.findById(id)).thenReturn(Optional.of(existingUser));
        when(userRepository.findByEmail(userRequest.email())).thenReturn(Optional.empty());

        UserResponse userResponse = userService.updateUser(id, userRequest);
        assertNotNull(userResponse);
        assertEquals(id, userResponse.id());
        assertEquals(userRequest.name(), userResponse.name());
        assertEquals(userRequest.email(), userResponse.email());
        assertEquals(userRequest.age(), userResponse.age());
        verify(userRepository).findById(id);
        verify(userRepository).findByEmail(userRequest.email());
    }

    @Test
    void updateUser_ifUserNotExists() {
        Integer id = 99;
        UserRequest userRequest = new UserRequest("Ghost", "ghost@example.com", 30);

        when(userRepository.findById(id)).thenReturn(Optional.empty());

        UserNotFoundException exception = assertThrows(
                UserNotFoundException.class,
                () -> userService.updateUser(id, userRequest)
        );
        assertEquals("Пользователь с таким id 99 не найден", exception.getMessage());
        verify(userRepository).findById(id);
        verify(userRepository, never()).findByEmail(userRequest.email());
    }

    @Test
    void updateUser_ifEmailIsAlreadyOccupied() {
        Integer id = 1;
        UserRequest userRequest = new UserRequest("Alice", "bob@example.com", 25);
        UserEntity foundByIdUser = new UserEntity("Alice", "alice@example.com", 24);
        foundByIdUser.setId(id);
        UserEntity foundByEmailUser = new UserEntity("Bob", "bob@example.com", 37);
        foundByEmailUser.setId(2);

        when(userRepository.findById(id)).thenReturn(Optional.of(foundByIdUser));
        when(userRepository.findByEmail(userRequest.email())).thenReturn(Optional.of(foundByEmailUser));

        UserAlreadyExistsException exception = assertThrows(
                UserAlreadyExistsException.class,
                () -> userService.updateUser(id, userRequest)
        );
        assertEquals(
                "Пользователь с таким mail bob@example.com уже существует",
                exception.getMessage()
        );
        verify(userRepository).findById(id);
        verify(userRepository).findByEmail(userRequest.email());
    }

    @Test
    void getUserById_ifUserExists() {
        Integer id = 1;
        UserEntity user = new UserEntity("Alice", "alice@example.com", 24);

        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        UserResponse userResponse = userService.getUserById(id);
        assertEquals(user.getName(), userResponse.name());
        assertEquals(user.getEmail(), userResponse.email());
        assertEquals(user.getAge(), userResponse.age());
        verify(userRepository).findById(id);
    }

    @Test
    void getUserById_ifUserNotExists() {
        Integer id = 99;

        when(userRepository.findById(id)).thenReturn(Optional.empty());

        UserNotFoundException exception = assertThrows(
                UserNotFoundException.class,
                () -> userService.getUserById(id)
        );
        assertEquals("Пользователь с таким id 99 не найден", exception.getMessage());
        verify(userRepository).findById(id);
    }

    @Test
    void getAll_ifUsersExists() {
        UserEntity user1 = new UserEntity("Alice", "alice@example.com", 24);
        UserEntity user2 = new UserEntity("Bob", "bob@example.com", 31);
        List<UserEntity> userEntities = List.of(user1, user2);

        when(userRepository.findAll()).thenReturn(userEntities);

        List<UserResponse> userResponses = userService.getAll();
        assertEquals(2, userResponses.size());
        verify(userRepository).findAll();
    }

    @Test
    void getAll_ifUsersNotExists() {
        when(userRepository.findAll()).thenReturn(Collections.emptyList());
        List<UserResponse> userResponses = userService.getAll();
        assertEquals(0, userResponses.size());
        verify(userRepository).findAll();
    }

    @Test
    void deleteUser_ifUsersExists() {
        Integer id = 1;

        when(userRepository.existsById(id)).thenReturn(true);

        userService.delete(id);
        verify(userRepository).existsById(id);
        verify(userRepository).deleteById(id);
    }

    @Test
    void deleteUser_ifUserNotFoundTest() {
        Integer id = 99;

        when(userRepository.existsById(id)).thenReturn(false);

        UserNotFoundException exception = assertThrows(
                UserNotFoundException.class, () -> userService.delete(id)
        );
        assertEquals("Пользователь с id=99 не найден", exception.getMessage());
        verify(userRepository).existsById(id);
        verify(userRepository, never()).deleteById(id);
    }

    @Test
    void getAllUserWithPagination_ifUsersExists(){
        UserEntity user1 = new UserEntity("Bob", "bob@email.com", 25);
        user1.setId(1);
        UserEntity user2 = new UserEntity("Lilu", "lilu@email.com", 55);
        user2.setId(2);
        UserEntity user3 = new UserEntity("John", "john@email.com", 48);
        user3.setId(3);

        List<UserEntity> firstPage = List.of(user1, user2, user3);
        Pageable pageable = PageRequest.of(0,3, Sort.by("age").ascending());
        Page<UserEntity> page = new PageImpl<>(firstPage, pageable, 5);
        when(userRepository.findAll(pageable)).thenReturn(page);
        PaginationRequest request = new PaginationRequest(0, 3, "age");
        Page<UserResponse> result = userService.getAllWithPagination(request);
        assertEquals(3, result.getContent().size());
        assertEquals(5, result.getTotalElements());
        assertEquals(2, result.getTotalPages());
        assertEquals(0, result.getNumber());
        assertEquals(3, result.getSize());
        verify(userRepository).findAll(pageable);
    }

    @Test
    void getAllUserWithPagination_ifUsersNotExists(){
        List<UserEntity> users = List.of();
        Pageable pageable = PageRequest.of(0,3, Sort.by("age").ascending());
        Page<UserEntity> page = new PageImpl<>(users, pageable, 0);
        when(userRepository.findAll(pageable)).thenReturn(page);
        PaginationRequest request = new PaginationRequest(0, 3, "age");
        Page<UserResponse> result = userService.getAllWithPagination(request);
        assertEquals(0, result.getContent().size());
        assertEquals(0, result.getTotalElements());
        assertEquals(0, result.getTotalPages());
        assertEquals(0, result.getNumber());
        assertEquals(3, result.getSize());
        verify(userRepository).findAll(pageable);
    }

}