package cn.tazumesans.xinmeiti.task;

import cn.tazumesans.xinmeiti.common.BizException;
import cn.tazumesans.xinmeiti.common.TimeUtil;
import cn.tazumesans.xinmeiti.mapper.TaskLogMapper;
import cn.tazumesans.xinmeiti.mapper.TaskMapper;
import cn.tazumesans.xinmeiti.model.Task;
import cn.tazumesans.xinmeiti.model.TaskLog;
import cn.tazumesans.xinmeiti.model.User;
import cn.tazumesans.xinmeiti.notify.NotifyService;
import cn.tazumesans.xinmeiti.score.ScoreService;
import cn.tazumesans.xinmeiti.system.MetaService;
import cn.tazumesans.xinmeiti.user.UserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TaskService extends ServiceImpl<TaskMapper, Task> {

    private final UserService userService;
    private final ScoreService scoreService;
    private final NotifyService notifyService;
    private final MetaService metaService;
    private final TaskLogMapper taskLogMapper;
    private final AttachmentService attachmentService;

    public TaskService(TaskMapper taskMapper, UserService userService, ScoreService scoreService,
                       NotifyService notifyService, MetaService metaService, TaskLogMapper taskLogMapper,
                       AttachmentService attachmentService) {
        this.baseMapper = taskMapper;
        this.userService = userService;
        this.scoreService = scoreService;
        this.notifyService = notifyService;
        this.metaService = metaService;
        this.taskLogMapper = taskLogMapper;
        this.attachmentService = attachmentService;
    }

    /* ------------------------------------------------------------------ 查询 */

    public Task getTask(Long id) {
        Task t = getById(id);
        if (t == null) {
            throw BizException.of("任务不存在");
        }
        decorate(List.of(t), null);
        return t;
    }

    /**
     * @param view open=待接取 / mine=我的 / review=待审核 / done=已完成 / all=全部
     */
    public List<Task> list(String view, Long meId) {
        LambdaQueryWrapper<Task> q = new LambdaQueryWrapper<Task>()
                .ne(Task::getStatus, "CANCELLED");
        switch (view == null ? "all" : view) {
            case "open" -> q.eq(Task::getStatus, "OPEN");
            case "mine" -> q.eq(Task::getAssigneeId, meId)
                    .in(Task::getStatus, List.of("IN_PROGRESS", "PENDING_REVIEW"));
            case "review" -> q.eq(Task::getStatus, "PENDING_REVIEW");
            case "done" -> q.eq(Task::getStatus, "COMPLETED");
            default -> {
            }
        }
        q.last("ORDER BY CASE status WHEN 'OPEN' THEN 0 WHEN 'IN_PROGRESS' THEN 1 " +
                "WHEN 'PENDING_REVIEW' THEN 2 ELSE 3 END, updated_at DESC");
        List<Task> tasks = list(q);
        decorate(tasks, meId);
        return tasks;
    }

    private void decorate(List<Task> tasks, Long meId) {
        if (tasks.isEmpty()) {
            return;
        }
        Set<Long> ids = new HashSet<>();
        for (Task t : tasks) {
            if (t.getCreatorId() != null) {
                ids.add(t.getCreatorId());
            }
            if (t.getAssigneeId() != null) {
                ids.add(t.getAssigneeId());
            }
            if (t.getReviewerId() != null) {
                ids.add(t.getReviewerId());
            }
        }
        Map<Long, String> names = userService.listByIds(ids).stream()
                .collect(Collectors.toMap(User::getId, User::getNickname, (a, b) -> a));
        String nextCandidate = null;
        for (Task t : tasks) {
            t.setCreatorName(names.get(t.getCreatorId()));
            t.setAssigneeName(names.get(t.getAssigneeId()));
            t.setReviewerName(names.get(t.getReviewerId()));
            boolean finished = "COMPLETED".equals(t.getStatus()) || "CANCELLED".equals(t.getStatus());
            t.setOverdue(!finished && TimeUtil.expired(t.getDdl()));
            t.setCanClaim("OPEN".equals(t.getStatus()) && !TimeUtil.expired(t.getClaimDeadline()));
            if ("OPEN".equals(t.getStatus())) {
                if (nextCandidate == null) {
                    nextCandidate = nextEmployeeName();
                }
                t.setNextCandidateName(nextCandidate);
            }
        }
    }

    public List<TaskLog> logs(Long taskId) {
        return taskLogMapper.selectByTask(taskId);
    }

    public Map<String, Object> stats(Long meId, boolean isAdmin) {
        Map<String, Object> m = new HashMap<>();
        m.put("open", countOf(new LambdaQueryWrapper<Task>().eq(Task::getStatus, "OPEN")));
        m.put("mine", countOf(new LambdaQueryWrapper<Task>()
                .eq(Task::getAssigneeId, meId)
                .in(Task::getStatus, List.of("IN_PROGRESS", "PENDING_REVIEW"))));
        if (isAdmin) {
            m.put("pendingReview", countOf(new LambdaQueryWrapper<Task>()
                    .eq(Task::getStatus, "PENDING_REVIEW")));
        }
        return m;
    }

    private long countOf(LambdaQueryWrapper<Task> q) {
        Long c = count(q);
        return c == null ? 0 : c;
    }

    /* ------------------------------------------------------------------ 发布 */

    public Long create(String title, String content, String claimDeadline, String ddl,
                       Integer claimScore, Integer completeScore, Integer reviewScore, Long creatorId) {
        if (title == null || title.isBlank()) {
            throw BizException.of("请填写任务标题");
        }
        if (claimDeadline == null || TimeUtil.parse(claimDeadline) == null) {
            throw BizException.of("请选择接取时限");
        }
        if (ddl == null || TimeUtil.parse(ddl) == null) {
            throw BizException.of("请选择完成时限（DDL）");
        }
        Task t = new Task();
        t.setTitle(title.trim());
        t.setContent(content);
        t.setCreatorId(creatorId);
        t.setClaimDeadline(claimDeadline);
        t.setDdl(ddl);
        t.setClaimScore(claimScore == null ? metaService.getInt(MetaService.K_CLAIM_SCORE, 1) : claimScore);
        t.setCompleteScore(completeScore == null ? metaService.getInt(MetaService.K_COMPLETE_SCORE, 1) : completeScore);
        t.setReviewScore(reviewScore == null ? metaService.getInt(MetaService.K_REVIEW_SCORE, 1) : reviewScore);
        t.setStatus("OPEN");
        t.setCreatedAt(TimeUtil.now());
        t.setUpdatedAt(TimeUtil.now());
        save(t);
        addLog(t.getId(), creatorId, "CREATE", "发布任务");
        notifyService.sendToUsers(activeUsers(), "NEW_TASK", "新任务：" + t.getTitle(),
                "《" + t.getTitle() + "》接取截止 " + claimDeadline + "，完成截止 " + ddl, t.getId());
        return t.getId();
    }

    private List<User> activeUsers() {
        return userService.listAll().stream().filter(u -> "ACTIVE".equals(u.getStatus())).toList();
    }

    /* ------------------------------------------------------------------ 接取 */

    public void claim(Long taskId, Long userId) {
        Task t = getTask(taskId);
        if (!"OPEN".equals(t.getStatus())) {
            throw BizException.of("该任务已被接取或已关闭");
        }
        // 超过接取时限后不再允许主动抢单，交由自动分配的定时任务按顺序派发
        if (TimeUtil.expired(t.getClaimDeadline())) {
            throw BizException.of("接取时限已过，该任务将由系统自动分配");
        }
        String now = TimeUtil.now();
        int rows = baseMapper.update(new Task(), new LambdaUpdateWrapper<Task>()
                .set(Task::getStatus, "IN_PROGRESS")
                .set(Task::getAssigneeId, userId)
                .set(Task::getAssignType, "CLAIM")
                .set(Task::getClaimedAt, now)
                .set(Task::getUpdatedAt, now)
                .eq(Task::getId, taskId)
                .eq(Task::getStatus, "OPEN"));
        if (rows == 0) {
            throw BizException.of("手慢了，任务刚被别人接走");
        }
        User u = userService.getById(userId);
        scoreService.add(userId, taskId, "CLAIM", t.getClaimScore(), "接取任务《" + t.getTitle() + "》", null);
        addLog(taskId, userId, "CLAIM", (u == null ? "" : u.getNickname()) + " 接取了任务");
    }

    /* -------------------------------------------------- 超时未接取：自动轮派 */

    /** 定时任务调用：到点仍无人接取的任务按顺序分配给员工 */
    public synchronized void autoAssignExpired() {
        List<Task> pending = list(new LambdaQueryWrapper<Task>()
                .eq(Task::getStatus, "OPEN")
                .le(Task::getClaimDeadline, TimeUtil.now())
                .orderByAsc(Task::getId));
        for (Task t : pending) {
            try {
                autoAssignOne(t);
            } catch (Exception e) {
                log.warn("自动分配任务 {} 失败：{}", t.getId(), e.getMessage());
            }
        }
    }

    private void autoAssignOne(Task t) {
        List<User> employees = userService.listActiveEmployees();
        if (employees.isEmpty()) {
            log.warn("没有可分配的员工，任务 {} 保持待接取", t.getId());
            return;
        }
        User next = pickNext(employees, metaService.getInt(MetaService.K_ASSIGN_CURSOR, 0));
        if (next == null) {
            return;
        }
        String now = TimeUtil.now();
        int rows = baseMapper.update(new Task(), new LambdaUpdateWrapper<Task>()
                .set(Task::getStatus, "IN_PROGRESS")
                .set(Task::getAssigneeId, next.getId())
                .set(Task::getAssignType, "AUTO")
                .set(Task::getClaimedAt, now)
                .set(Task::getUpdatedAt, now)
                .eq(Task::getId, t.getId())
                .eq(Task::getStatus, "OPEN"));
        if (rows == 0) {
            return;
        }
        metaService.setInt(MetaService.K_ASSIGN_CURSOR, next.getId().intValue());
        addLog(t.getId(), null, "AUTO_ASSIGN", "接取时限已过，系统自动分配给 " + next.getNickname());

        // 自动分配是否也给"接取分"，由管理员在系统设置里决定（默认不给）
        if (metaService.getInt(MetaService.K_AUTO_CLAIM_SCORE, 0) == 1) {
            scoreService.add(next.getId(), t.getId(), "CLAIM", t.getClaimScore(),
                    "系统自动分配《" + t.getTitle() + "》", null);
        }
        notifyService.send(next.getId(), next.getEmail(), "AUTO_ASSIGN",
                "任务已自动分配给你：" + t.getTitle(),
                "《" + t.getTitle() + "》超过接取时限无人接取，已按顺序分配给你，完成截止 " + t.getDdl(),
                t.getId());
    }

    private User pickNext(List<User> users, int cursor) {
        for (User u : users) {
            if (u.getId() != null && u.getId() > cursor) {
                return u;
            }
        }
        return users.isEmpty() ? null : users.get(0);
    }

    /** 下一个将被自动分配到的人（展示用，不移动游标） */
    public String nextEmployeeName() {
        List<User> employees = userService.listActiveEmployees();
        if (employees.isEmpty()) {
            return null;
        }
        User next = pickNext(employees, metaService.getInt(MetaService.K_ASSIGN_CURSOR, 0));
        return next == null ? null : next.getNickname();
    }

    /* ------------------------------------------------------------------ 提交 */

    public void submit(Long taskId, Long userId, String previewUrl, String note) {
        Task t = getTask(taskId);
        // 进行中可以提交；已经提交待审核的，允许改完再交（换新的秀米预览链接）
        boolean resubmit = "PENDING_REVIEW".equals(t.getStatus());
        if (!"IN_PROGRESS".equals(t.getStatus()) && !resubmit) {
            throw BizException.of("当前状态不能提交");
        }
        if (t.getAssigneeId() == null || !t.getAssigneeId().equals(userId)) {
            throw BizException.of("只有任务负责人可以提交");
        }
        if (previewUrl == null || previewUrl.isBlank()) {
            throw BizException.of("请填写秀米预览链接");
        }
        String url = previewUrl.trim();
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }
        String now = TimeUtil.now();
        // 重新提交仍交给原来的审核人（避免来回换人）；首次提交才按顺序轮询分配
        Long reviewerId = t.getReviewerId() != null ? t.getReviewerId() : nextReviewerId();
        int rows = baseMapper.update(new Task(), new LambdaUpdateWrapper<Task>()
                .set(Task::getStatus, "PENDING_REVIEW")
                .set(Task::getPreviewUrl, url)
                .set(Task::getSubmitNote, note)
                .set(Task::getSubmittedAt, now)
                .set(Task::getReviewerId, reviewerId)
                .set(Task::getReviewComment, (String) null)
                .set(Task::getReviewedAt, (String) null)
                .set(Task::getUpdatedAt, now)
                .eq(Task::getId, taskId)
                .in(Task::getStatus, List.of("IN_PROGRESS", "PENDING_REVIEW")));
        if (rows == 0) {
            throw BizException.of("提交失败，请刷新后重试");
        }

        // 按时完成加分（只加一次；被打回后重新提交不重复加）
        boolean onTime = !TimeUtil.expired(t.getDdl());
        User me = userService.getById(userId);
        String who = me == null ? "" : me.getNickname();
        int clawedBack = 0;
        if (onTime) {
            if (!scoreService.exists(taskId, userId, "COMPLETE")) {
                scoreService.add(userId, taskId, "COMPLETE", t.getCompleteScore(),
                        "按时完成任务《" + t.getTitle() + "》", null);
                addLog(taskId, userId, "COMPLETE", "在 DDL 内提交，按时完成 +" + t.getCompleteScore());
            } else {
                addLog(taskId, userId, "SUBMIT", resubmit ? "修改后重新提交（预览链接已更新）" : "重新提交");
            }
        } else {
            addLog(taskId, userId, "SUBMIT", "逾期提交，不获得完成分");
            // 超过 DDL 才完成：扣回此前已到手的接取分（已扣过的不再重复扣）
            int claimed = scoreService.sumOf(taskId, userId, "CLAIM");
            int alreadyBack = -scoreService.sumOf(taskId, userId, "CLAIM_BACK");
            clawedBack = Math.max(0, claimed - alreadyBack);
            if (clawedBack > 0) {
                scoreService.add(userId, taskId, "CLAIM_BACK", -clawedBack,
                        "《" + t.getTitle() + "》超过 DDL 提交，扣回接取分", null);
                addLog(taskId, userId, "CLAIM_BACK", "逾期提交，扣回接取分 -" + clawedBack);
            }
        }

        String title = (resubmit ? "重新提交待审核：" : "待审核：") + t.getTitle();
        String content = who + (resubmit ? " 修改了提交内容" : " 提交了") + "《" + t.getTitle() + "》\n秀米预览：" + url
                + (onTime ? "（按时）" : "（已逾期" + (clawedBack > 0 ? "，已扣回接取分 " + clawedBack : "") + "）");
        if (reviewerId != null) {
            User reviewer = userService.getById(reviewerId);
            notifyService.send(reviewerId, reviewer == null ? null : reviewer.getEmail(),
                    "REVIEW_REQUEST", title, content, taskId);
        } else {
            notifyService.sendToUsers(userService.listActiveAdmins(), "REVIEW_REQUEST", title, content, taskId);
        }
    }

    /* ------------------------------------------------------------------ 审核 */

    /**
     * 审核：评语可选（打回也不强制）。
     *
     * @param giveScore 通过时是否给"审核分"——质量好才给分，走个流程可以不给
     */
    public void review(Long taskId, Long adminId, boolean approve, String comment, boolean giveScore) {
        Task t = getTask(taskId);
        if (!"PENDING_REVIEW".equals(t.getStatus())) {
            throw BizException.of("该任务不在待审核状态");
        }
        String now = TimeUtil.now();
        String cmt = (comment == null || comment.isBlank()) ? null : comment.trim();
        String cmtText = cmt == null ? "" : "，评语：" + cmt;
        User assignee = t.getAssigneeId() == null ? null : userService.getById(t.getAssigneeId());
        if (approve) {
            baseMapper.update(new Task(), new LambdaUpdateWrapper<Task>()
                    .set(Task::getStatus, "COMPLETED")
                    .set(Task::getReviewerId, adminId)
                    .set(Task::getReviewComment, cmt)
                    .set(Task::getReviewedAt, now)
                    .set(Task::getUpdatedAt, now)
                    .eq(Task::getId, taskId)
                    .eq(Task::getStatus, "PENDING_REVIEW"));
            if (giveScore) {
                scoreService.add(t.getAssigneeId(), taskId, "REVIEW", t.getReviewScore(),
                        "《" + t.getTitle() + "》审核通过", adminId);
            }
            addLog(taskId, adminId, "APPROVE",
                    (giveScore ? "审核通过 +" + t.getReviewScore() : "审核通过（本次不给分）") + cmtText);
            notifyService.send(t.getAssigneeId(), assignee == null ? null : assignee.getEmail(),
                    "REVIEW_RESULT", "审核通过：" + t.getTitle(),
                    "《" + t.getTitle() + "》审核通过"
                            + (giveScore ? "，+" + t.getReviewScore() + " 分" : "（本次不额外计分）")
                            + (cmt == null ? "" : "\n评语：" + cmt), taskId);
        } else {
            baseMapper.update(new Task(), new LambdaUpdateWrapper<Task>()
                    .set(Task::getStatus, "IN_PROGRESS")
                    .set(Task::getReviewerId, adminId)
                    .set(Task::getReviewComment, cmt)
                    .set(Task::getSubmittedAt, (String) null)
                    .set(Task::getUpdatedAt, now)
                    .eq(Task::getId, taskId)
                    .eq(Task::getStatus, "PENDING_REVIEW"));
            addLog(taskId, adminId, "REJECT", "被审核打回" + (cmt == null ? "（未填写评语）" : "：" + cmt));
            notifyService.send(t.getAssigneeId(), assignee == null ? null : assignee.getEmail(),
                    "REVIEW_RESULT", "需要修改：" + t.getTitle(),
                    "《" + t.getTitle() + "》被打回，可以修改后重新提交（换新的秀米预览链接）。"
                            + (cmt == null ? "" : "\n意见：" + cmt), taskId);
        }
    }

    /* ------------------------------------------------------- 管理员手动指派/作废 */

    public void manualAssign(Long taskId, Long userId) {
        Task t = getTask(taskId);
        if (!"OPEN".equals(t.getStatus()) && !"IN_PROGRESS".equals(t.getStatus())) {
            throw BizException.of("当前状态不能指派");
        }
        User target = userService.getById(userId);
        if (target == null) {
            throw BizException.of("用户不存在");
        }
        String now = TimeUtil.now();
        baseMapper.update(new Task(), new LambdaUpdateWrapper<Task>()
                .set(Task::getStatus, "IN_PROGRESS")
                .set(Task::getAssigneeId, userId)
                .set(Task::getAssignType, "MANUAL")
                .set(Task::getClaimedAt, t.getClaimedAt() == null ? now : t.getClaimedAt())
                .set(Task::getUpdatedAt, now)
                .eq(Task::getId, taskId));
        addLog(taskId, null, "MANUAL_ASSIGN", "管理员指派给 " + target.getNickname());
        notifyService.send(userId, target.getEmail(), "AUTO_ASSIGN", "任务指派给你：" + t.getTitle(),
                "《" + t.getTitle() + "》由管理员指派给你，完成截止 " + t.getDdl(), taskId);
    }

    public void cancel(Long taskId, Long adminId) {
        Task t = getTask(taskId);
        if ("COMPLETED".equals(t.getStatus())) {
            throw BizException.of("已完成的任务不能作废");
        }
        lambdaUpdate().eq(Task::getId, taskId)
                .set(Task::getStatus, "CANCELLED")
                .set(Task::getUpdatedAt, TimeUtil.now())
                .update();
        clearAttachment(t, adminId);
        addLog(taskId, adminId, "CANCEL", "任务被作废");
    }

    /* ---------------------------------------------------------------- 任务附件 */

    /**
     * 给任务挂一个附件（压缩包或单个 docx）。发布者或管理员可调用，重复上传会覆盖旧文件。
     */
    public void saveAttachment(Long taskId, Long operatorId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw BizException.of("请选择要上传的附件");
        }
        Task t = getTask(taskId);
        String old = t.getAttachPath();
        String relative = attachmentService.save(taskId, file);
        String original = file.getOriginalFilename();
        lambdaUpdate().eq(Task::getId, taskId)
                .set(Task::getAttachName, original == null || original.isBlank() ? "attachment" : original)
                .set(Task::getAttachPath, relative)
                .set(Task::getAttachSize, file.getSize())
                .set(Task::getUpdatedAt, TimeUtil.now())
                .update();
        if (old != null && !old.equals(relative)) {
            attachmentService.delete(old);
        }
        addLog(taskId, operatorId, "ATTACH", "上传任务附件：" + original);
    }

    /** 下载任务附件：返回磁盘真实路径 */
    public Path attachmentFile(Long taskId) {
        Task t = getTask(taskId);
        if (t.getAttachPath() == null) {
            throw BizException.of("该任务没有附件");
        }
        Path p = attachmentService.resolve(t.getAttachPath());
        if (p == null || !Files.exists(p)) {
            throw BizException.of("附件已不存在，可能已被清理");
        }
        return p;
    }

    /** 任务作废时清掉附件，释放磁盘空间 */
    private void clearAttachment(Task t, Long actorId) {
        if (t.getAttachPath() == null) {
            return;
        }
        attachmentService.delete(t.getAttachPath());
        lambdaUpdate().eq(Task::getId, t.getId())
                .set(Task::getAttachName, (String) null)
                .set(Task::getAttachPath, (String) null)
                .set(Task::getAttachSize, (Long) null)
                .update();
        addLog(t.getId(), actorId, "ATTACH", "任务作废，已清理附件");
    }

    /* ------------------------------------------------------------------ 内部 */

    /** 审核人轮询：在管理员之间按顺序轮流 */
    private Long nextReviewerId() {
        List<User> admins = userService.listActiveAdmins();
        if (admins.isEmpty()) {
            return null;
        }
        User next = pickNext(admins, metaService.getInt(MetaService.K_REVIEW_CURSOR, 0));
        if (next == null) {
            return null;
        }
        metaService.setInt(MetaService.K_REVIEW_CURSOR, next.getId().intValue());
        return next.getId();
    }

    private void addLog(Long taskId, Long actorId, String action, String detail) {
        TaskLog tl = new TaskLog();
        tl.setTaskId(taskId);
        tl.setActorId(actorId);
        tl.setAction(action);
        tl.setDetail(detail);
        tl.setCreatedAt(TimeUtil.now());
        taskLogMapper.insert(tl);
    }
}
