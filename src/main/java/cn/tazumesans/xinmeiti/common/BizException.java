package cn.tazumesans.xinmeiti.common;

/** 业务异常，携带错误码，会被 GlobalExceptionHandler 转成 Result */
public class BizException extends RuntimeException {

    public static final int FORCE_CHANGE_PASSWORD = 1001;
    public static final int NO_AUTH = 1002;
    public static final int NOT_LOGIN = 1003;

    private final int code;

    public BizException(String message) {
        super(message);
        this.code = -1;
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static BizException of(String message) {
        return new BizException(message);
    }
}
