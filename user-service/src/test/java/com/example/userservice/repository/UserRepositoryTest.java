package com.example.userservice.repository;

import com.example.userservice.entity.UserEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Testcontainers
@Sql(
        scripts = "/insert-users.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS
)
class UserRepositoryTest {

    @Container
    @ServiceConnection
    private static final PostgreSQLContainer<?> POSTGRES_CONTAINER = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private UserRepository userRepository;

    @Test
    void save_success() {
        UserEntity newUser = new UserEntity("Dave", "dave@example.com", 40);
        UserEntity savedUser = userRepository.save(newUser);

        assertNotNull(savedUser.getId(), "После сохранения id должен быть присвоен");
        assertEquals(newUser.getName(), savedUser.getName());
        assertEquals("dave@example.com", savedUser.getEmail());
        assertEquals(40, savedUser.getAge());

        UserEntity fromDb = userRepository.findById(savedUser.getId()).get();
        assertEquals(newUser.getName(), fromDb.getName());
        assertEquals(newUser.getEmail(), fromDb.getEmail());
        assertEquals(newUser.getAge(), fromDb.getAge());
        assertNotNull(fromDb.getCreatedAt(), "createdAt должен проставиться через @PrePersist");
    }

    @Test
    void save_ifEmailIsAlreadyOccupied() {
        UserEntity user = new UserEntity("Fake Alice", "alice@example.com", 99);
        assertThrows(
                DataIntegrityViolationException.class,
                () -> userRepository.save(user)
        );
    }

    @Test
    void existById_ifIdExists() {
        Integer id = 2;
        assertEquals(true, userRepository.existsById(id));
    }

    @Test
    void existById_ifIdNotExists() {
        Integer id = 999;
        assertEquals(false, userRepository.existsById(id));
    }

    @Test
    void existsByEmail_ifEmailExists() {
        String email = "bob@example.com";
        assertEquals(true, userRepository.existsByEmail(email));
    }

    @Test
    void existsByEmail_ifEmailNotExists() {
        String email = "johndoe@example.com";
        assertEquals(false, userRepository.existsByEmail(email));
    }

    @Test
    void findById_ifUserExists() {
        Integer id = 1;
        Optional<UserEntity> optionalUser = userRepository.findById(id);

        assertThat(optionalUser.isPresent());
        assertEquals("Alice", optionalUser.get().getName());
        assertEquals("alice@example.com", optionalUser.get().getEmail());
        assertEquals(25, optionalUser.get().getAge());
    }

    @Test
    void findById_ifUserNotExists() {
        Integer id = 999;
        Optional<UserEntity> optionalUser = userRepository.findById(id);

        assertThat(optionalUser.isEmpty());
        assertThatCode(() -> userRepository.findById(id))
                .doesNotThrowAnyException();
    }

    @Test
    void findByEmail_ifUserExists() {
        String email = "charlie@example.com";
        Optional<UserEntity> optionalUser = userRepository.findByEmail(email);

        assertThat(optionalUser.isPresent());
        assertEquals("Charlie", optionalUser.get().getName());
        assertEquals("charlie@example.com", optionalUser.get().getEmail());
        assertEquals(35, optionalUser.get().getAge());
    }

    @Test
    void findByEmail_ifUserNotExists() {
        String email = "johndoe@example.com";
        Optional<UserEntity> optionalUser = userRepository.findByEmail(email);

        assertThat(optionalUser.isEmpty());
        assertThatCode(() -> userRepository.findByEmail(email))
                .doesNotThrowAnyException();
    }

    @Test
    void findAll_exists() {
        List<UserEntity> users = userRepository.findAll();
        assertEquals(5, users.size());
    }

    @Test
    void deleteById_ifUserExists() {
        Integer id = 3;
        userRepository.deleteById(id);
        assertEquals(false, userRepository.existsById(id));
    }

    @Test
    void deleteById_ifUserNotExists() {
        Integer id = 999;
        userRepository.deleteById(id);
        assertThatCode(() -> userRepository.deleteById(id))
                .doesNotThrowAnyException();
    }

    @Test
    void findAllWithPagination_ifUserExists(){
        Pageable pageable = PageRequest.of(2, 2, Sort.by("age").ascending());
        Page<UserEntity> page = userRepository.findAll(pageable);
        //нумерация страниц начинается с 0. page = 2 — это 3-я страница.
        //смещение: 2 * 2 = 4, поэтому пропускаются первые 4 записи.
        assertEquals(2, page.getNumber());
        //показывает количество страниц, если мы передали 2 элемента на странице
        assertEquals(3, page.getTotalPages());
        //показывает сколько мы запросили элементов на страницу
        assertEquals(2, page.getSize());
        //показывает сколько всего записей
        assertEquals(5, page.getTotalElements());
        //это актуальное количество записей на странице
        assertEquals(1, page.getContent().size());
    }
}