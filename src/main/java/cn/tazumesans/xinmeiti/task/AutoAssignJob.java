package cn.tazumesans.xinmeiti.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 接取时限到点仍无人接取的任务 → 按顺序自动分配给员工 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AutoAssignJob {

    private final TaskService taskService;

    @Scheduled(fixedDelayString = "${xmt.auto-assign-fixed-delay:30000}", initialDelay = 15000)
    public void run() {
        taskService.autoAssignExpired();
    }
}
