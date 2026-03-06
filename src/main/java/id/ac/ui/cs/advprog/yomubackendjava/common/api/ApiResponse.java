package id.ac.ui.cs.advprog.yomubackendjava.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public final class ApiResponse<T> {
    private final boolean successful;
    private final String message;
    private final T data;

    private ApiResponse(boolean success, String message, T data) {
        this.successful = success;
        this.message = message;
        this.data = data;
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data);
    }

    public static ApiResponse<Void> success(String message) {
        return new ApiResponse<>(true, message, null);
    }

    public static ApiResponse<Void> error(String message) {
        return new ApiResponse<>(false, message, null);
    }

    public boolean isSuccess() {
        return successful;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }
}
