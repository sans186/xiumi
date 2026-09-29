package cn.tazumesans.xinmeiti.user;

import cn.dev33.satoken.stp.StpUtil;
import cn.tazumesans.xinmeiti.common.BizException;
import cn.tazumesans.xinmeiti.common.PasswordHasher;
import cn.tazumesans.xinmeiti.common.Result;
import cn.tazumesans.xinmeiti.model.User;
import cn.tazumesans.xinmeiti.security.AuthKit;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final AuthKit authKit;

    public record LoginRequest(String username, String password) {
    }

    public record ChangePwdRequest(String oldPassword, String newPassword) {
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody LoginRequest req) {
        if (req.username() == null || req.password() == null) {
            throw BizException.of("请输入账号和密码");
        }
        User u = userService.findByUsername(req.username().trim());
        if (u == null || !PasswordHasher.matches(req.password(), u.getPasswordHash())) {
            throw BizException.of("账号或密码错误");
        }
        if (!"ACTIVE".equals(u.getStatus())) {
            throw BizException.of("账号已被停用，请联系管理员");
        }
        userService.touchLogin(u.getId());
        StpUtil.login(u.getId());

        Map<String, Object> data = new HashMap<>();
        data.put("token", StpUtil.getTokenValue());
        data.put("mustChangePassword", Integer.valueOf(1).equals(u.getMustChange()));
        Map<String, Object> user = new HashMap<>();
        user.put("id", u.getId());
        user.put("username", u.getUsername());
        user.put("nickname", u.getNickname());
        user.put("role", u.getRole());
        user.put("email", u.getEmail());
        user.put("groupId", u.getGroupId());
        data.put("user", user);
        return Result.ok(data);
    }

    @PostMapping("/logout")
    public Result<?> logout() {
        StpUtil.logout();
        return Result.ok();
    }

    @PostMapping("/change-password")
    public Result<?> changePassword(@RequestBody ChangePwdRequest req) {
        userService.changePassword(authKit.id(), req.oldPassword(), req.newPassword());
        return Result.ok();
    }
}
