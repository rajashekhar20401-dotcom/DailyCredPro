package com.unqiuehire.kashflow.dto.responsedto;
import com.unqiuehire.kashflow.constant.ApiStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApiResponse<T> {

    private ApiStatus status;
    private String message;
    private T data;

    public ApiResponse() {
    }

    public ApiResponse(ApiStatus status, String message, T data) {
        this.status = status;
        this.message = message;
        this.data = data;
    }
}