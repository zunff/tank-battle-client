package com.zunf.tankbattleclient.exception;

/**
 * 自定义错误码
 *
 * @author ZunF
 */
public enum ErrorCode {

    // 0: success
    OK(0, "OK"),

    // 1xxx: common
    INTERNAL_ERROR(1000, "Internal error"),
    UNKNOWN_ERROR(1001, "Unknown error"),
    NOT_IMPLEMENTED(1002, "Not implemented"),

    // 2xxx: protocol / params
    BAD_REQUEST(2000, "Bad request"),
    INVALID_ARGUMENT(2001, "Invalid argument"),
    MISSING_ARGUMENT(2002, "Missing argument"),
    BAD_PROTOCOL_VERSION(2003, "Bad protocol version"),
    UNSUPPORTED_COMMAND(2004, "Unsupported command"),
    PAYLOAD_TOO_LARGE(2005, "Payload too large"),

    // 3xxx: auth / permission
    UNAUTHORIZED(3001, "Unauthorized"),
    TOKEN_EXPIRED(3002, "Token expired"),
    FORBIDDEN(3003, "Forbidden"),

    // 4xxx: resource
    NOT_FOUND(4001, "Not found"),
    ALREADY_EXISTS(4002, "Already exists"),
    CONFLICT(4003, "Conflict"),

    // 5xxx: retryable / transient
    TIMEOUT(5001, "Timeout"),
    RATE_LIMITED(5002, "Rate limited"),
    SERVICE_UNAVAILABLE(5003, "Service unavailable");


    /**
     * 状态码
     */
    private final int code;

    /**
     * 信息
     */
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public static ErrorCode of(int code) {
        for (ErrorCode value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        return UNKNOWN_ERROR;
    }
}