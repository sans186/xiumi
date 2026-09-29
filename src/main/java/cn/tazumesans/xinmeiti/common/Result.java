package cn.tazumesans.xinmeiti.common;

/**
 * 统一响应体：{code, message, data}
 * code = 0 表示成功，其余为业务错误码
 */
public record Result<T>(int code, String message, T data) {

    public static final int OK = 0;

    public static <T> Result<T> ok() {
        return new Result<>(OK, "ok", null);
    }

    public static <T> Result<T> ok(T data) {
        return new Result<>(OK, "ok", data);
    }

    public static <T> Result<T> fail(String message) {
        return new Result<>(-1, message, null);
    }

    public static <T> Result<T> fail(int code, String message) {
        return new Result<>(code, message, null);
    }
}
