package com.example.userservice.controller;

import com.example.userservice.dto.PaginationRequest;
import com.example.userservice.dto.UserRequest;
import com.example.userservice.dto.UserResponse;
import com.example.userservice.exception.UserAlreadyExistsException;
import com.example.userservice.exception.UserNotFoundException;
import com.example.userservice.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;


    @Test
    @DisplayName("POST /users — создаёт пользователя и возвращает 201")
    void createUser_ShouldReturn201_WhenDataIsValid() throws Exception {

        UserRequest request = new UserRequest("Иван", "ivan@mail.ru", 25);
        UserResponse response = new UserResponse(1, "Иван", "ivan@mail.ru", 25);
        when(userService.createUser(any(UserRequest.class))).thenReturn(response);

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Иван"))
                .andExpect(jsonPath("$.email").value("ivan@mail.ru"))
                .andExpect(jsonPath("$.age").value(25));

        verify(userService, times(1)).createUser(any(UserRequest.class));
    }

    @Test
    @DisplayName("POST /users — возвращает 400, если email невалидный")
    void createUser_ShouldReturn400_WhenEmailIsInvalid() throws Exception {

        UserRequest request = new UserRequest("Иван", "invalid-email", 25);

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(userService, never()).createUser(any(UserRequest.class));
    }

    @Test
    @DisplayName("POST /users — возвращает 409, если email уже занят")
    void createUser_ShouldReturn409_WhenEmailExists() throws Exception {

        UserRequest request = new UserRequest("Иван", "ivan@mail.ru", 25);
        when(userService.createUser(any(UserRequest.class)))
                .thenThrow(new UserAlreadyExistsException("Email занят"));

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("GET /users/{id} — возвращает 200 и пользователя")
    void getUserById_ShouldReturn200_WhenUserExists() throws Exception {

        UserResponse response = new UserResponse(1, "Иван", "ivan@mail.ru", 25);
        when(userService.getUserById(1)).thenReturn(response);

        mockMvc.perform(get("/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Иван"))
                .andExpect(jsonPath("$.email").value("ivan@mail.ru"));
    }

    @Test
    @DisplayName("GET /users/{id} — возвращает 404, если пользователь не найден")
    void getUserById_ShouldReturn404_WhenUserNotFound() throws Exception {

        when(userService.getUserById(999))
                .thenThrow(new UserNotFoundException("Пользователь не найден"));

        mockMvc.perform(get("/users/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /users — возвращает 200 и список пользователей")
    void getAllUsers_ShouldReturn200AndList() throws Exception {

        List<UserResponse> users = List.of(
                new UserResponse(1, "Иван", "ivan@mail.ru", 25),
                new UserResponse(2, "Пётр", "petr@mail.ru", 30)
        );
        when(userService.getAll()).thenReturn(users);

        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(2))
                .andExpect(jsonPath("$[0].name").value("Иван"))
                .andExpect(jsonPath("$[1].name").value("Пётр"));
    }

    @Test
    @DisplayName("GET /users — возвращает 200 и пустой список")
    void getAllUsers_ShouldReturn200AndEmptyList() throws Exception {

        when(userService.getAll()).thenReturn(List.of());

        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(0));
    }

    @Test
    @DisplayName("PUT /users/{id} — возвращает 200 и обновлённого пользователя")
    void updateUser_ShouldReturn200_WhenDataIsValid() throws Exception {

        UserRequest request = new UserRequest("Иван Сидоров", "ivan_new@mail.ru", 35);
        UserResponse response = new UserResponse(1, "Иван Сидоров", "ivan_new@mail.ru", 35);
        when(userService.updateUser(eq(1), any(UserRequest.class))).thenReturn(response);

        mockMvc.perform(put("/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Иван Сидоров"))
                .andExpect(jsonPath("$.email").value("ivan_new@mail.ru"))
                .andExpect(jsonPath("$.age").value(35));

        verify(userService, times(1)).updateUser(eq(1), any(UserRequest.class));
    }

    @Test
    @DisplayName("PUT /users/{id} — возвращает 404, если пользователь не найден")
    void updateUser_ShouldReturn404_WhenUserNotFound() throws Exception {

        UserRequest request = new UserRequest("Иван", "ivan@mail.ru", 25);
        when(userService.updateUser(eq(999), any(UserRequest.class)))
                .thenThrow(new UserNotFoundException("Пользователь не найден"));

        mockMvc.perform(put("/users/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /users/{id} — возвращает 400, если данные невалидны")
    void updateUser_ShouldReturn400_WhenDataIsInvalid() throws Exception {

        UserRequest request = new UserRequest("", "invalid-email", -5);

        mockMvc.perform(put("/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE /users/{id} — возвращает 204")
    void deleteUser_ShouldReturn204_WhenUserExists() throws Exception {

        doNothing().when(userService).delete(1);

        mockMvc.perform(delete("/users/1"))
                .andExpect(status().isNoContent());

        verify(userService, times(1)).delete(1);
    }

    @Test
    @DisplayName("DELETE /users/{id} — возвращает 404, если пользователь не найден")
    void deleteUser_ShouldReturn404_WhenUserNotFound() throws Exception {

        doThrow(new UserNotFoundException("Пользователь не найден"))
                .when(userService).delete(999);

        mockMvc.perform(delete("/users/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /users/pagination — возвращает 200 и список пользователей с пагинацией")
    void getUsersWithPagination_ShouldReturn200AndPage() throws Exception {
        List<UserResponse> users = List.of(
                new UserResponse(1, "Иван", "ivan@mail.ru", 25),
                new UserResponse(2, "Пётр", "petr@mail.ru", 30)
        );
        Page<UserResponse> page = new PageImpl<>(
                users,
                PageRequest.of(0, 2, Sort.by("id").ascending()),
                2
        );
        when(userService.getAllWithPagination(any(PaginationRequest.class)))
                .thenReturn(page);

        mockMvc.perform(get("/users/pagination")
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.size()").value(2))
                .andExpect(jsonPath("$.content[0].name").value("Иван"))
                .andExpect(jsonPath("$.content[1].name").value("Пётр"))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    @DisplayName("GET /users/pagination — возвращает 400 при некорректных параметрах")
    void getUsersWithPagination_ShouldReturn400WhenParametersInvalid() throws Exception {

        mockMvc.perform(get("/users/pagination")
                        .param("page", "-1")
                        .param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Введены некорректные данные"))
                .andExpect(jsonPath("$.details.page")
                        .value("Номер страницы не может быть меньше 0"))
                .andExpect(jsonPath("$.details.size")
                        .value("Количество пользователей должно быть больше 0"));
    }

    @Test
    @DisplayName("GET /users/pagination — без sortBy возвращает 200 и пробрасывает sortBy=null в сервис")
    void getUsersWithPagination_WithoutSortBy_ShouldReturn200AndPassNullSortBy() throws Exception {
        List<UserResponse> users = List.of(
                new UserResponse(1, "Иван", "ivan@mail.ru", 25),
                new UserResponse(2, "Пётр", "petr@mail.ru", 30)
        );
        Page<UserResponse> page = new PageImpl<>(
                users,
                PageRequest.of(0, 2, Sort.by("id").ascending()),
                2
        );
        when(userService.getAllWithPagination(any(PaginationRequest.class)))
                .thenReturn(page);

        mockMvc.perform(get("/users/pagination")
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.size()").value(2))
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Иван"))
                .andExpect(jsonPath("$.content[1].id").value(2))
                .andExpect(jsonPath("$.content[1].name").value("Пётр"))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1));

        ArgumentCaptor<PaginationRequest> captor = ArgumentCaptor.forClass(PaginationRequest.class);
        verify(userService).getAllWithPagination(captor.capture());
        PaginationRequest captured = captor.getValue();
        assertThat(captured.page()).isEqualTo(0);
        assertThat(captured.size()).isEqualTo(2);
        assertThat(captured.sortBy()).isNull();
    }

    @Test
    @DisplayName("GET /users/pagination — с sortBy возвращает 200 и пробрасывает sortBy в сервис")
    void getUsersWithPagination_WithSortBy_ShouldReturn200AndPassSortBy() throws Exception {
        List<UserResponse> users = List.of(
                new UserResponse(2, "Пётр", "petr@mail.ru", 30),
                new UserResponse(1, "Иван", "ivan@mail.ru", 25)
        );
        Page<UserResponse> page = new PageImpl<>(
                users,
                PageRequest.of(0, 2, Sort.by("age").ascending()),
                2
        );
        when(userService.getAllWithPagination(any(PaginationRequest.class)))
                .thenReturn(page);

        mockMvc.perform(get("/users/pagination")
                        .param("page", "0")
                        .param("size", "2")
                        .param("sortBy", "age"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.size()").value(2))
                .andExpect(jsonPath("$.content[0].age").value(30))
                .andExpect(jsonPath("$.content[1].age").value(25))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1));

        ArgumentCaptor<PaginationRequest> captor = ArgumentCaptor.forClass(PaginationRequest.class);
        verify(userService).getAllWithPagination(captor.capture());
        assertThat(captor.getValue().sortBy()).isEqualTo("age");
    }

    @Test
    @DisplayName("GET /users/pagination — 400 при некорректном поле сортировки")
    void getUsersWithPagination_ShouldReturn400WhenSortByInvalid() throws Exception {
        mockMvc.perform(get("/users/pagination")
                        .param("page", "0")
                        .param("size", "2")
                        .param("sortBy", "xxx"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.sortBy")
                        .value("Недопустимое поле сортировки"));

        verifyNoInteractions(userService);
    }

}