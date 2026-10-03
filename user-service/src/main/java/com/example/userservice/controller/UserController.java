package com.example.userservice.controller;

import com.example.userservice.dto.PaginationRequest;
import com.example.userservice.dto.UserRequest;
import com.example.userservice.dto.UserResponse;
import com.example.userservice.hateoas.UserModelAssembler;
import com.example.userservice.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST-контроллер для управления пользователями.
 */
@RestController
@RequestMapping("/users")
@Tag(name = "User API", description = "Операции с пользователями")
public class UserController {

    private final UserService userService;
    private final UserModelAssembler userModelAssembler;
    private final PagedResourcesAssembler<UserResponse> pagedResourcesAssembler;

    public UserController(
            UserService userService,
            UserModelAssembler userModelAssembler,
            PagedResourcesAssembler<UserResponse> pagedResourcesAssembler
    ) {
        this.userService = userService;
        this.userModelAssembler = userModelAssembler;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
    }

    @PostMapping
    @Operation(summary = "Создать пользователя", description = "Создаёт нового пользователя")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Пользователь создан"),
            @ApiResponse(responseCode = "400", description = "Некорректные данные"),
            @ApiResponse(responseCode = "409", description = "Email уже занят")
    })
    public ResponseEntity<EntityModel<UserResponse>> createUser(@Valid @RequestBody UserRequest request) {
        UserResponse response = userService.createUser(request);
        EntityModel<UserResponse> entityModel = userModelAssembler.toExtendedModel(response);
        return ResponseEntity.status(HttpStatus.CREATED).body(entityModel);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить пользователя по ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Пользователь найден"),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден")
    })
    public ResponseEntity<EntityModel<UserResponse>> getUserById(@PathVariable Integer id) {
        UserResponse response = userService.getUserById(id);
        EntityModel<UserResponse> entityModel = userModelAssembler.toExtendedModel(response);
        return ResponseEntity.ok(entityModel);
    }

    @GetMapping
    @Operation(summary = "Получить всех пользователей")
    @ApiResponse(responseCode = "200", description = "Список пользователей")
    public ResponseEntity<CollectionModel<EntityModel<UserResponse>>> getAllUsers() {
        List<UserResponse> response = userService.getAll();
        CollectionModel<EntityModel<UserResponse>> collectionModel = userModelAssembler.toCollectionModel(response);
        return ResponseEntity.ok(collectionModel);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить пользователя по ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Пользователь обновлён"),
            @ApiResponse(responseCode = "400", description = "Некорректные данные"),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден"),
            @ApiResponse(responseCode = "409", description = "Email уже занят")
    })
    public ResponseEntity<EntityModel<UserResponse>> updateUser(
            @PathVariable Integer id,
            @Valid @RequestBody UserRequest request) {
        UserResponse response = userService.updateUser(id, request);
        EntityModel<UserResponse> entityModel = userModelAssembler.toExtendedModel(response);
        return ResponseEntity.ok(entityModel);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить пользователя по ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Пользователь удалён"),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден")
    })
    public ResponseEntity<Void> deleteUser(@PathVariable Integer id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/pagination")
    @Operation(
            summary = "Получить пользователей с пагинацией",
            description = "Возвращает страницу пользователей с возможностью сортировки"
    )
    @ApiResponses(
            value = {
                    @ApiResponse(responseCode = "200", description = "Пользователи найдены"),
                    @ApiResponse(responseCode = "400", description = "Некорректные параметры пагинации")
            }
    )
    public ResponseEntity<PagedModel<EntityModel<UserResponse>>> getUsersWithPagination(
            @Valid @ModelAttribute PaginationRequest request
    ) {
        Page<UserResponse> page = userService.getAllWithPagination(request);
        PagedModel<EntityModel<UserResponse>> pagedModel = pagedResourcesAssembler.toModel(page, userModelAssembler);
        return ResponseEntity.ok(pagedModel);
    }
}