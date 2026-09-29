package cn.tazumesans.xinmeiti.notify;

import cn.tazumesans.xinmeiti.common.Result;
import cn.tazumesans.xinmeiti.model.Notification;
import cn.tazumesans.xinmeiti.security.AuthKit;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotifyController {

    private final NotifyService notifyService;
    private final AuthKit authKit;

    @GetMapping
    public Result<List<Notification>> list(@RequestParam(defaultValue = "false") boolean unreadOnly,
                                           @RequestParam(defaultValue = "50") int limit) {
        return Result.ok(notifyService.listMine(authKit.id(), unreadOnly, limit));
    }

    @GetMapping("/unread-count")
    public Result<Map<String, Integer>> unreadCount() {
        return Result.ok(Map.of("count", notifyService.unreadCount(authKit.id())));
    }

    @PutMapping("/{id}/read")
    public Result<?> read(@PathVariable Long id) {
        notifyService.markRead(id, authKit.id());
        return Result.ok();
    }

    @PutMapping("/read-all")
    public Result<?> readAll() {
        notifyService.markAllRead(authKit.id());
        return Result.ok();
    }
}
