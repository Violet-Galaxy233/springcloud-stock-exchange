package com.itranswarp.exchange;

/**
 * 返回给客户端的错误响应体。
 *
 * @param error   错误码
 * @param data    出错的字段 (可为空)
 * @param message 错误描述
 */
public record ApiErrorResponse(ApiError error, String data, String message) {
}
