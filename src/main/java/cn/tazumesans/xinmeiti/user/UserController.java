package cn.tazumesans.xinmeiti.user;

import cn.tazumesans.xinmeiti.common.Result;
import cn.tazumesans.xinmeiti.model.User;
import cn.tazumesans.xinmeiti.security.AuthKit;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final AuthKit authKit;
    private final cn.tazumesans.xinmeiti.score.ScoreService scoreService;

    public record CreateUserRequest(String username, String nickname, String role,
                                    String email, Long groupId, String initialPassword) {
    }

    public record UpdateUserRequest(String nickname, String role, String email,
                                    Long groupId, String status) {
    }

    @GetMapping
    public Result<List<User>> list() {
        authKit.requireAdmin();
        List<User> list = userService.listAll();
        list.forEach(u -> u.setPasswordHash(null));
        return Result.ok(list);
    }

    @GetMapping("/simple")
    public Result<List<User>> simple() {
        List<User> list = userService.listAll();
        list.forEach(u -> u.setPasswordHash(null));
        return Result.ok(list);
    }

    @PostMapping
    public Result<Map<String, Object>> create(@RequestBody CreateUserRequest req) {
        authKit.requireAdmin();
        User u = userService.create(req.username(), req.nickname(), req.role(),
                req.email(), req.groupId(), req.initialPassword());
        return Result.ok(Map.of("id", u.getId(), "nickname", u.getNickname()));
    }

    @PutMapping("/{id}")
    public Result<?> update(@PathVariable Long id, @RequestBody UpdateUserRequest req) {
        authKit.requireAdmin();
        userService.updateUser(id, req.nickname(), req.role(), req.email(), req.groupId(), req.status(),
                authKit.id());
        return Result.ok();
    }

    /** 重置密码：不传 password 时随机生成，返回实际密码给管理员 */
    @PostMapping("/{id}/reset-password")
    public Result<Map<String, String>> resetPassword(@PathVariable Long id,
                                                     @RequestBody(required = false) Map<String, String> body) {
        authKit.requireAdmin();
        String pwd = body == null ? null : body.get("password");
        return Result.ok(Map.of("password", userService.resetPassword(id, pwd)));
    }

    /** 管理员手动加减分（score 可为负） */
    @PostMapping("/{id}/adjust-score")
    public Result<?> adjustScore(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        authKit.requireAdmin();
        int score = body.get("score") == null ? 0 : ((Number) body.get("score")).intValue();
        Object remark = body.get("remark");
        if (score == 0) {
            throw cn.tazumesans.xinmeiti.common.BizException.of("分值不能为 0");
        }
        scoreService.add(id, null, "MANUAL", score,
                (remark == null || String.valueOf(remark).isBlank())
                        ? "管理员手动调整" : String.valueOf(remark),
                authKit.id());
        return Result.ok();
    }
}
