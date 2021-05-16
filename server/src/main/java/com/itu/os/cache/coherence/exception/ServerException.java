package com.itu.os.cache.coherence.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;

@ResponseStatus(code = HttpStatus.INTERNAL_SERVER_ERROR, reason = "error in processing the request")
public class ServerException extends ResponseStatusException {

    public ServerException(HttpStatus status) {
        super(status);
    }

    public ServerException(HttpStatus status, String reason) {
        super(status, reason);
    }

    public ServerException(HttpStatus status, String reason, Throwable cause) {
        super(status, reason, cause);
    }
}
