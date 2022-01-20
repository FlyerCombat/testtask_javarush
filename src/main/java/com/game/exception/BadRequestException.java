package com.game.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class BadRequestException extends RuntimeException{
    // this is Error - 400
    public BadRequestException() {
    }
    public BadRequestException(String message) {
        super(message);
    }
}
