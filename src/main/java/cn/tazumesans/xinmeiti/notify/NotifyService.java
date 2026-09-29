package cn.tazumesans.xinmeiti.notify;

import cn.tazumesans.xinmeiti.common.TimeUtil;
import cn.tazumesans.xinmeiti.mapper.NotificationMapper;
import cn.tazumesans.xinmeiti.model.Notification;
import cn.tazumesans.xinmeiti.model.User;
import cn.tazumesans.xinmeiti.system.MailService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotifyService extends ServiceImpl<NotificationMapper, Notification> {

    private final MailService mailService;

    public NotifyService(NotificationMapper notificationMapper, MailService mailService) {
        this.baseMapper = notificationMapper;
        this.mailService = mailService;
    }

    /** 站内消息 + 邮件（用户填了邮箱才发） */
    public void send(Long userId, String email, String type, String title, String content, Long taskId) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setType(type);
        n.setTitle(title);
        n.setContent(content);
        n.setTaskId(taskId);
        n.setIsRead(0);
        n.setCreatedAt(TimeUtil.now());
        save(n);
        mailService.sendAsync(email, "【新媒体小组】" + title, content);
    }

    public void sendToUsers(List<User> users, String type, String title, String content, Long taskId) {
        for (User u : users) {
            send(u.getId(), u.getEmail(), type, title, content, taskId);
        }
    }

    public List<Notification> listMine(Long userId, boolean unreadOnly, int limit) {
        LambdaQueryWrapper<Notification> q = new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(unreadOnly, Notification::getIsRead, 0)
                .orderByDesc(Notification::getId)
                .last("LIMIT " + Math.max(1, Math.min(limit, 200)));
        return list(q);
    }

    public int unreadCount(Long userId) {
        Long c = count(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, 0));
        return c == null ? 0 : c.intValue();
    }

    public void markRead(Long id, Long userId) {
        lambdaUpdate().eq(Notification::getId, id)
                .eq(Notification::getUserId, userId)
                .set(Notification::getIsRead, 1)
                .update();
    }

    public void markAllRead(Long userId) {
        lambdaUpdate().eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, 0)
                .set(Notification::getIsRead, 1)
                .update();
    }
}
