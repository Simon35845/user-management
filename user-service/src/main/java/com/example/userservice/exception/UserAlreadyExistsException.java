package com.example.userservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
/**
 * @author Yushinova (TATYANA YUSHINOVA)
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class UserAlreadyExistsException extends UserServiceException{
    public UserAlreadyExistsException(String message) {
        super(message);
    }
}
