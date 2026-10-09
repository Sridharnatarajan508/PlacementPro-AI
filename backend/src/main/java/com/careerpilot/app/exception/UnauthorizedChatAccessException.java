package com.careerpilot.app.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class UnauthorizedChatAccessException extends RuntimeException {
    public UnauthorizedChatAccessException(String message) {
        super(message);
    }
}
