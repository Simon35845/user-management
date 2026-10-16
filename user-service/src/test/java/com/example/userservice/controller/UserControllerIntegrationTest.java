package com.example.userservice.controller;

import com.example.common.dto.UserEvent;
import com.example.userservice.dto.UserRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Transactional
@Sql(
        scripts = "/insert-users.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS
)
class UserControllerIntegrationTest {

    @Container
    @ServiceConnection
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private KafkaTemplate<String, UserEvent> kafkaTemplate;

    @Test
    @DisplayName("POST /users — создаёт пользователя и возвращает 201")
    void createUser_ShouldReturn201_WhenDataIsValid() throws Exception {
        UserRequest request = new UserRequest("Иван", "ivan@example.com", 25);

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaTypes.HAL_JSON))
                .andExpect(jsonPath("$.id").value(6))
                .andExpect(jsonPath("$.name").value("Иван"))
                .andExpect(jsonPath("$.email").value("ivan@example.com"))
                .andExpect(jsonPath("$.age").value(25))
                .andExpect(jsonPath("$._links.self.href").value(containsString("/users/6")))
                .andExpect(jsonPath("$._links.users.href").value(containsString("/users")))
                .andExpect(jsonPath("$._links.pagination.href")
                        .value(containsString("/users/pagination")));
        verify(kafkaTemplate).send(eq("notifications"), eq("ivan@example.com"), any(UserEvent.class));
    }

    @Test
    @DisplayName("POST /users — возвращает 400, если email невалидный")
    void createUser_ShouldReturn400_WhenEmailIsInvalid() throws Exception {
        UserRequest request = new UserRequest("Иван", "invalid-email", 25);

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        verify(kafkaTemplate, never())
                .send(eq("notifications"), eq("ivan@example.com"), any(UserEvent.class));
    }

    @Test
    @DisplayName("POST /users — возвращает 409, если email уже занят")
    void createUser_ShouldReturn409_WhenEmailIsAlreadyOccupied() throws Exception {
        UserRequest request = new UserRequest("Иван", "bob@example.com", 25);

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        verify(kafkaTemplate, never())
                .send(eq("notifications"), eq("ivan@example.com"), any(UserEvent.class));
    }

    @Test
    @DisplayName("GET /users/{id} — возвращает 200 и пользователя")
    void getUserById_ShouldReturn200_WhenUserExists() throws Exception {
        mockMvc.perform(get("/users/3"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaTypes.HAL_JSON))
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.name").value("Charlie"))
                .andExpect(jsonPath("$.email").value("charlie@example.com"))
                .andExpect(jsonPath("$._links.self.href").value(containsString("/users/3")))
                .andExpect(jsonPath("$._links.users.href").value(containsString("/users")))
                .andExpect(jsonPath("$._links.pagination.href")
                        .value(containsString("/users/pagination")));
    }

    @Test
    @DisplayName("GET /users/{id} — возвращает 404, если пользователь не найден")
    void getUserById_ShouldReturn404_WhenUserNotFound() throws Exception {
        mockMvc.perform(get("/users/999"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("GET /users — возвращает 200 и список пользователей")
    void getAllUsers_ShouldReturn200AndList() throws Exception {
        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaTypes.HAL_JSON))
                .andExpect(jsonPath("$._embedded.userResponseList.size()").value(5))
                .andExpect(jsonPath("$._embedded.userResponseList[*].name",
                        containsInAnyOrder("Alice", "Bob", "Charlie", "Diana", "Ethan")))
                .andExpect(jsonPath("$._links.self.href").value(containsString("/users")));
    }

    @Test
    @DisplayName("PUT /users/{id} — возвращает 200 и обновлённого пользователя")
    void updateUser_ShouldReturn200_WhenDataIsValid() throws Exception {
        UserRequest request = new UserRequest("Иван", "ivan@example.com", 25);

        mockMvc.perform(put("/users/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaTypes.HAL_JSON))
                .andExpect(jsonPath("$.name").value("Иван"))
                .andExpect(jsonPath("$.email").value("ivan@example.com"))
                .andExpect(jsonPath("$.age").value(25))
                .andExpect(jsonPath("$._links.self.href").value(containsString("/users/2")))
                .andExpect(jsonPath("$._links.users.href").value(containsString("/users")))
                .andExpect(jsonPath("$._links.pagination.href")
                        .value(containsString("/users/pagination")));
    }

    @Test
    @DisplayName("PUT /users/{id} — возвращает 400, если данные невалидны")
    void updateUser_ShouldReturn400_WhenDataIsInvalid() throws Exception {
        UserRequest request = new UserRequest("", "invalid-email", -5);

        mockMvc.perform(put("/users/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("PUT /users/{id} — возвращает 404, если пользователь не найден")
    void updateUser_ShouldReturn404_WhenUserNotFound() throws Exception {
        UserRequest request = new UserRequest("Иван", "ivan@example.com", 25);

        mockMvc.perform(put("/users/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("PUT /users/{id} — возвращает 409, если email уже занят")
    void updateUser_ShouldReturn409_WhenEmailIsAlreadyOccupied() throws Exception {
        UserRequest request = new UserRequest("Иван", "charlie@example.com", 25);

        mockMvc.perform(put("/users/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("DELETE /users/{id} — возвращает 204")
    void deleteUser_ShouldReturn204_WhenUserExists() throws Exception {
        mockMvc.perform(delete("/users/5"))
                .andExpect(status().isNoContent());
        verify(kafkaTemplate).send(eq("notifications"), eq("ethan@example.com"), any(UserEvent.class));
    }

    @Test
    @DisplayName("DELETE /users/{id} — возвращает 404, если пользователь не найден")
    void deleteUser_ShouldReturn404_WhenUserNotFound() throws Exception {
        mockMvc.perform(delete("/users/999"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        verify(kafkaTemplate, never())
                .send(eq("notifications"), eq("ivan@example.com"), any(UserEvent.class));
    }

    @Test
    @DisplayName("GET /users/pagination — возвращает 200 и список пользователей с пагинацией")
    void getUsersWithPagination_ShouldReturn200AndPage() throws Exception {
        mockMvc.perform(get("/users/pagination")
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaTypes.HAL_JSON))
                .andExpect(jsonPath("$._embedded.userResponseList.size()").value(2))
                .andExpect(jsonPath("$._embedded.userResponseList[0].id").value(1))
                .andExpect(jsonPath("$._embedded.userResponseList[0].name").value("Alice"))
                .andExpect(jsonPath("$._embedded.userResponseList[1].id").value(2))
                .andExpect(jsonPath("$._embedded.userResponseList[1].name").value("Bob"))
                .andExpect(jsonPath("$._links.self.href")
                        .value(containsString("/users/pagination?page=0&size=2&sort=id,asc")))
                .andExpect(jsonPath("$.page.totalElements").value(5))
                .andExpect(jsonPath("$.page.totalPages").value(3))
                .andExpect(jsonPath("$.page.size").value(2))
                .andExpect(jsonPath("$.page.number").value(0));
    }

    @Test
    @DisplayName("GET /users/pagination — с sortBy возвращает 200 и пробрасывает sortBy в сервис")
    void getUsersWithPagination_WithSortBy_ShouldReturn200AndPassSortBy() throws Exception {
        mockMvc.perform(get("/users/pagination")
                        .param("page", "0")
                        .param("size", "2")
                        .param("sortBy", "age"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaTypes.HAL_JSON))
                .andExpect(jsonPath("$._embedded.userResponseList.size()").value(2))
                .andExpect(jsonPath("$._embedded.userResponseList[0].age").value(25))
                .andExpect(jsonPath("$._embedded.userResponseList[1].age").value(30))
                .andExpect(jsonPath("$._links.self.href")
                        .value(containsString("/users/pagination?page=0&size=2&sort=age,asc")))
                .andExpect(jsonPath("$.page.totalElements").value(5))
                .andExpect(jsonPath("$.page.totalPages").value(3))
                .andExpect(jsonPath("$.page.size").value(2))
                .andExpect(jsonPath("$.page.number").value(0));
    }

    @Test
    @DisplayName("GET /users/pagination — возвращает 400 при некорректных параметрах")
    void getUsersWithPagination_ShouldReturn400WhenParametersInvalid() throws Exception {
        mockMvc.perform(get("/users/pagination")
                        .param("page", "-1")
                        .param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message")
                        .value("Введены некорректные данные"))
                .andExpect(jsonPath("$.details.page")
                        .value("Номер страницы не может быть меньше 0"))
                .andExpect(jsonPath("$.details.size")
                        .value("Количество пользователей должно быть больше 0"));
    }

    @Test
    @DisplayName("GET /users/pagination — 400 при некорректном поле сортировки")
    void getUsersWithPagination_ShouldReturn400WhenSortByInvalid() throws Exception {
        mockMvc.perform(get("/users/pagination")
                        .param("page", "0")
                        .param("size", "2")
                        .param("sortBy", "xxx"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.details.sortBy")
                        .value("Недопустимое поле сортировки"));
    }
}
