package cn.tazumesans.xinmeiti.user;

import cn.tazumesans.xinmeiti.common.Result;
import cn.tazumesans.xinmeiti.model.User;
import cn.tazumesans.xinmeiti.notify.NotifyService;
import cn.tazumesans.xinmeiti.score.ScoreService;
import cn.tazumesans.xinmeiti.security.AuthKit;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class MeController {

    private final UserService userService;
    private final ScoreService scoreService;
    private final NotifyService notifyService;
    private final AuthKit authKit;

    @GetMapping
    public Result<Map<String, Object>> me() {
        Long id = authKit.id();
        User u = userService.getById(id);
        Map<String, Object> user = new HashMap<>();
        user.put("id", u.getId());
        user.put("username", u.getUsername());
        user.put("nickname", u.getNickname());
        user.put("role", u.getRole());
        user.put("email", u.getEmail());
        user.put("groupId", u.getGroupId());
        user.put("mustChange", u.getMustChange());

        Map<String, Object> perm = new HashMap<>();
        perm.put("canReview", authKit.isAdmin());
        perm.put("canManage", authKit.isAdmin());
        perm.put("canPublish", authKit.canPublish());
        perm.put("isLeader", authKit.isGroupLeader(id));

        Map<String, Object> data = new HashMap<>();
        data.put("user", user);
        data.put("permissions", perm);
        data.put("summary", scoreService.mySummary(id));
        data.put("unread", notifyService.unreadCount(id));
        return Result.ok(data);
    }

    @GetMapping("/scores")
    public Result<?> myScores() {
        return Result.ok(scoreService.listByUser(authKit.id(), 100));
    }

    /** 自己改邮箱（用于接收邮件通知） */
    @PutMapping("/email")
    public Result<?> updateEmail(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        userService.lambdaUpdate()
                .eq(User::getId, authKit.id())
                .set(User::getEmail, email == null || email.isBlank() ? null : email.trim())
                .update();
        return Result.ok();
    }
}
