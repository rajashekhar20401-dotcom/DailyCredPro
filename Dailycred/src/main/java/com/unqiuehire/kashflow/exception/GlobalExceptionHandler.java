package com.unqiuehire.kashflow.exception;
import com.unqiuehire.kashflow.constant.ApiStatus;
import com.unqiuehire.kashflow.dto.responsedto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Handle Resource Not Found
    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Object> handleResourceNotFound(ResourceNotFoundException ex) {

        return new ApiResponse<>(
                ApiStatus.FAILURE,
                ex.getMessage(),
                null
        );
    }

    // Handle Illegal Argument (Bad Request)
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Object> handleIllegalArgument(IllegalArgumentException ex) {

        return new ApiResponse<>(
                ApiStatus.FAILURE,
                ex.getMessage(),
                null
        );
    }

    // Handle All Other Exceptions
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Object> handleGenericException(Exception ex) {

        return new ApiResponse<>(
                ApiStatus.FAILURE,
                "Something went wrong: " + ex.getMessage(),
                null
        );
    }
}