package com.example.userservice.service;

import com.example.common.dto.UserEvent;
import com.example.common.dto.UserEventType;
import com.example.userservice.dto.PaginationRequest;
import com.example.userservice.dto.UserRequest;
import com.example.userservice.dto.UserResponse;
import com.example.userservice.entity.UserEntity;
import com.example.userservice.eventproducer.UserEventProducer;
import com.example.userservice.exception.UserAlreadyExistsException;
import com.example.userservice.exception.UserNotFoundException;
import com.example.userservice.mapper.UserMapper;
import com.example.userservice.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.example.userservice.mapper.UserMapper.toEntity;
import static com.example.userservice.mapper.UserMapper.toResponse;

/**
 * Service с бизнес-логикой приложения и обработкой исключений.
 */
@Service
public class UserService {
    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private final UserRepository userRepository;
    private final UserEventProducer userEventProducer;

    public UserService(UserRepository userRepository, UserEventProducer userEventProducer) {
        this.userRepository = userRepository;
        this.userEventProducer = userEventProducer;
    }

    @Transactional
    public UserResponse createUser(UserRequest request) {
        log.info("Создание пользователя: name={}, email={}, age={}."
                , request.name(), request.email(), request.age());
        if (userRepository.existsByEmail(request.email())) {
            log.warn("Пользователь с email={} уже существует.", request.email());
            throw new UserAlreadyExistsException("Пользователь с таким mail " + request.email() + " уже существует");
        }
        try {
            UserEntity entity = toEntity(request);
            UserEntity saved = userRepository.save(entity);
            log.info("Пользователь успешно сохранён: {}.", saved);
            UserEvent event = new UserEvent(UUID.randomUUID(), saved.getEmail(), UserEventType.CREATE_USER);
            userEventProducer.sendEvent(event);
            return toResponse(saved);
        } catch (DataIntegrityViolationException e) {
            throw new UserAlreadyExistsException("Пользователь с таким mail " + request.email() + " уже существует");
        }
    }

    @Transactional
    public UserResponse updateUser(Integer id, UserRequest request) {
        log.info("Изменение пользователя: id={} name={}, email={}, age={}."
                , id, request.name(), request.email(), request.age());
        UserEntity user = userRepository.findById(id).orElseThrow(() -> {
            log.warn("Пользователь с id={} не найден.", id);
            return new UserNotFoundException("Пользователь с id=" + id + " не найден");
        });
        Optional<UserEntity> byEmail = userRepository.findByEmail(request.email());
        if (byEmail.isPresent() && !byEmail.get().getId().equals(id)) {
            log.warn("Пользователь с email={} уже существует.", request.email());
            throw new UserAlreadyExistsException("Пользователь с таким mail " + request.email() + " уже существует");
        }
        user.setEmail(request.email());
        user.setName(request.name());
        user.setAge(request.age());
        log.info("Пользователь успешно изменен: {}.", user);
        return toResponse(user);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Integer id) {
        log.info("Поиск пользователя по id={}.", id);
        return userRepository.findById(id)
                .map(user -> {
                    log.info("Пользователь с id={} найден", id);
                    return UserMapper.toResponse(user);
                })
                .orElseThrow(() -> {
                    log.warn("Пользователь с id={} не найден", id);
                    return new UserNotFoundException("Пользователь с таким id " + id + " не найден");
                });
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAll() {
        log.info("Вывод всех пользователей");
        List<UserResponse> result = userRepository.findAll().stream().map(UserMapper::toResponse).toList();
        log.info("Найдено пользователей: {}", result.size());
        return result;
    }

    @Transactional
    public void delete(Integer id) {
        log.info("Удаление пользователя по id={}.", id);
        UserEntity user = userRepository.findById(id).orElseThrow(() -> {
            log.warn("Пользователь с id={} не найден.", id);
            return new UserNotFoundException("Пользователь с id=" + id + " не найден");
        });
        userRepository.deleteById(id);
        log.info("Пользователь с id={} удален. ", id);
        UserEvent event = new UserEvent(UUID.randomUUID(), user.getEmail(), UserEventType.DELETE_USER);
        userEventProducer.sendEvent(event);
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> getAllWithPagination(PaginationRequest request) {
        log.info("Вывод пользователей страница={}, количество={}, сортируем={}",
                request.page(), request.size(), request.sortBy());

        String sortBy = request.sortBy();

        if (sortBy == null || sortBy.isBlank()) {
            sortBy = "id";
        }
        Pageable pageable = PageRequest.of(request.page(), request.size(), Sort.by(sortBy).ascending());
        Page<UserResponse> result = userRepository.findAll(pageable).map(UserMapper::toResponse);
        log.info("Найдено пользователей: {}", result.getContent().size());
        return result;
    }
}