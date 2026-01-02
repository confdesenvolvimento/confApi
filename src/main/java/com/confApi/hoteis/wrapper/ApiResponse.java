package com.confApi.hoteis.wrapper;

import lombok.Data;

@Data
public class ApiResponse<T> {
    private boolean success;
    private String traceId;
    private String message;
    private T data;

    public static <T> ApiResponse<T> ok(T data) {
        ApiResponse<T> r = new ApiResponse<>();
        r.success = true;
        r.data = data;
        return r;
    }
}