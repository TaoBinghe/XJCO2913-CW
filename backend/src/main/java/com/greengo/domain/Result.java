package com.greengo.domain;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class Result<T> {
    // Business status code: 0-success, 1-failure
    private Integer code;

    // Message
    private String message;

    // Response data
    private T data;

    // Quick success response with data
    public static <E> Result<E> success(E data) {
        return new Result<>(0, "Success", data);
    }

    // Quick success response without data
    public static Result success() {
        return new Result(0, "Success", null);
    }

    public static Result error(String message) {
        return new Result(1, message, null);
    }
}

