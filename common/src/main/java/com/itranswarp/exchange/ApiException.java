package com.itranswarp.exchange;

/**
 * 业务异常，携带 {@link ApiError} 错误码，由全局异常处理器转换为 HTTP 响应。
 */
public class ApiException extends RuntimeException {

    public final ApiError error;
    public final String data;

    public ApiException(ApiError error) {
        this(error, null, error.name());
    }

    public ApiException(ApiError error, String message) {
        this(error, null, message);
    }

    public ApiException(ApiError error, String data, String message) {
        super(message);
        this.error = error;
        this.data = data;
    }

    public ApiErrorResponse getError() {
        return new ApiErrorResponse(this.error, this.data, getMessage());
    }
}
