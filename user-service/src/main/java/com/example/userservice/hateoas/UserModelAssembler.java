package com.example.userservice.hateoas;

import com.example.userservice.controller.UserController;
import com.example.userservice.dto.UserResponse;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Component
public class UserModelAssembler implements RepresentationModelAssembler<UserResponse, EntityModel<UserResponse>> {

    @Override
    public EntityModel<UserResponse> toModel(UserResponse user) {
        return EntityModel.of(
                user,
                linkTo(methodOn(UserController.class).getUserById(user.id())).withSelfRel()
        );
    }

    public EntityModel<UserResponse> toExtendedModel(UserResponse user) {
        EntityModel<UserResponse> entityModel = toModel(user);
        entityModel.add(
                linkTo(methodOn(UserController.class).getAllUsers()).withRel("users"),
                linkTo(UserController.class).slash("pagination").withRel("pagination")
        );
        return entityModel;
    }

    @Override
    public CollectionModel<EntityModel<UserResponse>> toCollectionModel(Iterable<? extends UserResponse> users) {
        CollectionModel<EntityModel<UserResponse>> collectionModel =
                RepresentationModelAssembler.super.toCollectionModel(users);
        collectionModel.add(
                linkTo(methodOn(UserController.class).getAllUsers()).withSelfRel(),
                linkTo(UserController.class).slash("pagination").withRel("pagination")
        );
        return collectionModel;
    }
}
