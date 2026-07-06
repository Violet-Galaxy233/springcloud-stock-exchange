package com.itranswarp.exchange;

/**
 * API 错误码。
 */
public enum ApiError {

    /**
     * 参数错误。
     */
    PARAMETER_INVALID,

    /**
     * 认证失败。
     */
    AUTH_SIGNIN_FAILED,

    /**
     * 无权限。
     */
    AUTH_FORBIDDEN,

    /**
     * 资产不足 (余额不足)。
     */
    NO_ENOUGH_ASSET,

    /**
     * 操作失败。
     */
    OPERATION_FAILED,

    /**
     * 找不到资源。
     */
    NOT_FOUND,

    /**
     * 内部错误。
     */
    INTERNAL_SERVER_ERROR;
}
