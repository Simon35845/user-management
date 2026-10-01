package com.example.userservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
/**
 * @author Yushinova (TATYANA YUSHINOVA)
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class UserNotFoundException extends UserServiceException{
    public UserNotFoundException(String message) {
        super(message);
    }
}
