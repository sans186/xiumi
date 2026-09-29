package cn.tazumesans.xinmeiti.task;

import cn.tazumesans.xinmeiti.common.Result;
import cn.tazumesans.xinmeiti.model.Task;
import cn.tazumesans.xinmeiti.model.TaskLog;
import cn.tazumesans.xinmeiti.security.AuthKit;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;
    private final AuthKit authKit;

    public record CreateTaskRequest(String title, String content, String claimDeadline, String ddl,
                                    Integer claimScore, Integer completeScore, Integer reviewScore) {
    }

    public record SubmitRequest(String previewUrl, String note) {
    }

    public record ReviewRequest(Boolean approve, String comment, Boolean giveScore) {
    }

    public record AssignRequest(Long userId) {
    }

    /** view: all / open / mine / review / done */
    @GetMapping
    public Result<List<Task>> list(@RequestParam(defaultValue = "all") String view) {
        return Result.ok(taskService.list(view, authKit.id()));
    }

    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        return Result.ok(taskService.stats(authKit.id(), authKit.isAdmin()));
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        Map<String, Object> data = new HashMap<>();
        data.put("task", taskService.getTask(id));
        List<TaskLog> logs = taskService.logs(id);
        data.put("logs", logs);
        return Result.ok(data);
    }

    @PostMapping
    public Result<Map<String, Long>> create(@RequestBody CreateTaskRequest req) {
        authKit.requireCanPublish();
        Long id = taskService.create(req.title(), req.content(), req.claimDeadline(), req.ddl(),
                req.claimScore(), req.completeScore(), req.reviewScore(), authKit.id());
        return Result.ok(Map.of("id", id));
    }

    /** 发布者/管理员上传任务附件（zip / rar / 7z / docx，≤50MB），重复上传覆盖 */
    @PostMapping("/{id}/attach")
    public Result<Map<String, Object>> uploadAttach(@PathVariable Long id,
                                                    @RequestParam("file") MultipartFile file) {
        authKit.requireCanPublish();
        taskService.saveAttachment(id, authKit.id(), file);
        Task t = taskService.getTask(id);
        Map<String, Object> data = new HashMap<>();
        data.put("attachName", t.getAttachName());
        data.put("attachSize", t.getAttachSize());
        return Result.ok(data);
    }

    /** 下载任务附件（需登录） */
    @GetMapping("/{id}/attach")
    public ResponseEntity<Resource> downloadAttach(@PathVariable Long id) {
        Task t = taskService.getTask(id);
        Path file = taskService.attachmentFile(id);
        String name = t.getAttachName() == null || t.getAttachName().isBlank()
                ? "attachment" : t.getAttachName();
        String encoded = URLEncoder.encode(name, StandardCharsets.UTF_8).replace("+", "%20");
        Resource body = new FileSystemResource(file);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + encoded + "\"; filename*=UTF-8''" + encoded)
                .contentType(mediaTypeOf(name))
                .body(body);
    }

    /** docx 给正确的 MIME，压缩包统一走 octet-stream */
    private MediaType mediaTypeOf(String name) {
        String lower = name == null ? "" : name.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".docx")) {
            return MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }

    /** 抢单 */
    @PostMapping("/{id}/claim")
    public Result<?> claim(@PathVariable Long id) {
        taskService.claim(id, authKit.id());
        return Result.ok();
    }

    /** 提交秀米预览链接 */
    @PostMapping("/{id}/submit")
    public Result<?> submit(@PathVariable Long id, @RequestBody SubmitRequest req) {
        taskService.submit(id, authKit.id(), req.previewUrl(), req.note());
        return Result.ok();
    }

    /** 管理员审核：评语可选；通过时可选择是否给"审核分"（不传默认给） */
    @PostMapping("/{id}/review")
    public Result<?> review(@PathVariable Long id, @RequestBody ReviewRequest req) {
        authKit.requireAdmin();
        boolean giveScore = req.giveScore() == null || Boolean.TRUE.equals(req.giveScore());
        taskService.review(id, authKit.id(), Boolean.TRUE.equals(req.approve()), req.comment(), giveScore);
        return Result.ok();
    }

    /** 管理员手动指派 */
    @PostMapping("/{id}/assign")
    public Result<?> assign(@PathVariable Long id, @RequestBody AssignRequest req) {
        authKit.requireAdmin();
        taskService.manualAssign(id, req.userId());
        return Result.ok();
    }

    @PostMapping("/{id}/cancel")
    public Result<?> cancel(@PathVariable Long id) {
        authKit.requireAdmin();
        taskService.cancel(id, authKit.id());
        return Result.ok();
    }
}
