package cn.tazumesans.xinmeiti.security;

import cn.dev33.satoken.stp.StpUtil;
import cn.tazumesans.xinmeiti.common.BizException;
import cn.tazumesans.xinmeiti.mapper.UserMapper;
import cn.tazumesans.xinmeiti.model.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

/**
 * 登录校验（基于 Sa-Token）+ 首次登录强制改密
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final UserMapper userMapper;

    /** 必须先改密码时，仍然允许访问的接口 */
    private static final String[] OPEN_WHEN_MUST_CHANGE = {"/api/auth/", "/api/me"};

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String uri = request.getRequestURI();
        if (!uri.startsWith("/api/")) {
            return true;
        }
        if (!StpUtil.isLogin()) {
            writeJson(response, BizException.NOT_LOGIN, "登录已失效，请重新登录");
            return false;
        }
        User user = userMapper.selectById(StpUtil.getLoginIdAsLong());
        if (user == null || !"ACTIVE".equals(user.getStatus())) {
            StpUtil.logout();
            writeJson(response, BizException.NO_AUTH, "账号已被停用，请联系管理员");
            return false;
        }
        if (Integer.valueOf(1).equals(user.getMustChange()) && !isWhitelisted(uri)) {
            writeJson(response, BizException.FORCE_CHANGE_PASSWORD, "请先修改初始密码");
            return false;
        }
        return true;
    }

    private boolean isWhitelisted(String uri) {
        for (String prefix : OPEN_WHEN_MUST_CHANGE) {
            if (uri.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private void writeJson(HttpServletResponse response, int code, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"code\":" + code + ",\"message\":\"" + escape(message) + "\",\"data\":null}");
    }

    private String escape(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }
}
