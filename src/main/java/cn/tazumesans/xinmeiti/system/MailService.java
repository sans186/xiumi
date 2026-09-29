package cn.tazumesans.xinmeiti.system;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * 邮件通知。未配置 SMTP 或总开关关闭时，只记录日志，不影响主流程。
 */
@Slf4j
@Service
public class MailService {

    @Value("${xmt.mail-enabled:true}")
    private boolean mailEnabled;

    @Value("${spring.mail.username:}")
    private String from;

    @Value("${xmt.site-url:}")
    private String siteUrl;

    @Autowired(required = false)
    private JavaMailSender mailSender;

    public boolean isEnabled() {
        return mailEnabled && mailSender != null;
    }

    public void sendAsync(String to, String subject, String text) {
        if (!isEnabled() || to == null || to.isBlank()) {
            return;
        }
        String finalTo = to.trim();
        String body = text + (siteUrl == null || siteUrl.isBlank() ? "" : "\n\n—— " + siteUrl);
        CompletableFuture.runAsync(() -> {
            try {
                SimpleMailMessage msg = new SimpleMailMessage();
                msg.setTo(finalTo);
                msg.setSubject(subject);
                msg.setText(body);
                if (from != null && !from.isBlank()) {
                    msg.setFrom(from);
                }
                mailSender.send(msg);
            } catch (Exception e) {
                log.warn("邮件发送失败 to={} : {}", finalTo, e.getMessage());
            }
        });
    }
}
