package com.example.userservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Класс используется для передачи сообщения об ошибке при возникновении исключений в UserService.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class UserServiceException extends RuntimeException{
    public UserServiceException(String message) {
        super(message);
    }
}
