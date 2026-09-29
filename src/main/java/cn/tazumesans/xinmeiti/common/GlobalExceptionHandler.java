package cn.tazumesans.xinmeiti.common;

import cn.dev33.satoken.exception.NotLoginException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public Result<?> handleBiz(BizException e) {
        return Result.fail(e.getCode(), e.getMessage());
    }

    /** Sa-Token 未登录 */
    @ExceptionHandler(NotLoginException.class)
    public Result<?> handleNotLogin(NotLoginException e) {
        return Result.fail(BizException.NOT_LOGIN, "登录已失效，请重新登录");
    }

    /** 上传文件超过 multipart 限制 */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<?> handleTooLarge(MaxUploadSizeExceededException e) {
        return Result.fail(400, "文件太大了，附件请控制在 50MB 以内");
    }

    @ExceptionHandler(Exception.class)
    public Result<?> handleOther(Exception e) {
        log.error("服务器内部错误", e);
        String msg = e.getMessage() == null ? "服务器开小差了" : e.getMessage();
        if (msg.length() > 200) {
            msg = msg.substring(0, 200);
        }
        return Result.fail(500, msg);
    }
}
