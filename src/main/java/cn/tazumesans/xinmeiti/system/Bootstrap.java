package cn.tazumesans.xinmeiti.system;

import cn.tazumesans.xinmeiti.common.PasswordHasher;
import cn.tazumesans.xinmeiti.common.TimeUtil;
import cn.tazumesans.xinmeiti.model.User;
import cn.tazumesans.xinmeiti.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** 启动引导：建表 → 建管理员 → 初始化默认配置 */
@Slf4j
@Component
@RequiredArgsConstructor
public class Bootstrap {

    private final JdbcTemplate jdbc;
    private final MetaService metaService;
    private final UserService userService;

    @Value("${xmt.bootstrap.admin-username:admin}")
    private String adminUsername;

    @Value("${xmt.bootstrap.admin-password:admin123}")
    private String adminPassword;

    @Value("${xmt.bootstrap.admin-nickname:系统管理员}")
    private String adminNickname;

    @Value("${xmt.default-claim-score:1}")
    private int defaultClaimScore;

    @Value("${xmt.default-complete-score:1}")
    private int defaultCompleteScore;

    @Value("${xmt.default-review-score:1}")
    private int defaultReviewScore;

    @Bean
    public ApplicationRunner bootstrapRunner() {
        return args -> {
            initSchema();
            migrate();
            initAdmin();
            ensureActiveAdmin();
            initMeta();
            log.info("启动完成，默认管理员账号：{}", adminUsername);
        };
    }

    private void initSchema() {
        try (InputStream in = new ClassPathResource("schema.sql").getInputStream()) {
            String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            StringBuilder sb = new StringBuilder();
            for (String line : sql.split("\n")) {
                String l = line.trim();
                if (l.startsWith("--") || l.isEmpty()) {
                    continue;
                }
                sb.append(l).append("\n");
            }
            for (String stmt : sb.toString().split(";")) {
                if (!stmt.isBlank()) {
                    jdbc.execute(stmt);
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("初始化数据库失败", e);
        }
    }

    /**
     * 轻量迁移：老库的建表语句带 IF NOT EXISTS，新增字段不会自动补上，
     * 这里按需 ALTER TABLE 补列（SQLite 不支持 ADD COLUMN IF NOT EXISTS，先查再改）。
     */
    private void migrate() {
        addColumnIfMissing("task", "attach_name", "TEXT");
        addColumnIfMissing("task", "attach_path", "TEXT");
        addColumnIfMissing("task", "attach_size", "INTEGER");
    }

    private void addColumnIfMissing(String table, String column, String type) {
        boolean exists = jdbc.queryForList("PRAGMA table_info(" + table + ")").stream()
                .anyMatch(row -> column.equalsIgnoreCase(String.valueOf(row.get("name"))));
        if (exists) {
            return;
        }
        jdbc.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + type);
        log.info("数据库迁移：已为 {} 表补充字段 {}", table, column);
    }

    private void initAdmin() {
        if (userService.findByUsername(adminUsername) != null) {
            return;
        }
        User u = new User();
        u.setUsername(adminUsername);
        u.setNickname(adminNickname);
        u.setPasswordHash(PasswordHasher.hash(adminPassword));
        u.setRole("ADMIN");
        u.setMustChange(1);
        u.setStatus("ACTIVE");
        u.setCreatedAt(TimeUtil.now());
        u.setUpdatedAt(TimeUtil.now());
        userService.save(u);
        log.info("已创建默认管理员 {}，初始密码取自 XMT_ADMIN_PASSWORD 配置（首次登录必须修改）", adminUsername);
    }

    /**
     * 兜底保护：万一库里一个在职管理员都不剩（例如误操作停用了自己），
     * 启动时自动把最早创建的管理员恢复为在职，避免整个后台没人能登录。
     */
    private void ensureActiveAdmin() {
        if (!userService.listActiveAdmins().isEmpty()) {
            return;
        }
        User target = userService.lambdaQuery()
                .eq(User::getRole, "ADMIN")
                .orderByAsc(User::getId)
                .last("limit 1")
                .one();
        if (target == null) {
            return;
        }
        userService.lambdaUpdate()
                .eq(User::getId, target.getId())
                .set(User::getStatus, "ACTIVE")
                .set(User::getUpdatedAt, TimeUtil.now())
                .update();
        log.warn("检测到系统没有在职管理员，已自动恢复「{}」（账号 {}）为在职，请登录后检查账号状态",
                target.getNickname(), target.getUsername());
    }

    private void initMeta() {
        if (metaService.get(MetaService.K_CLAIM_SCORE, null) == null) {
            metaService.setInt(MetaService.K_CLAIM_SCORE, defaultClaimScore);
        }
        if (metaService.get(MetaService.K_COMPLETE_SCORE, null) == null) {
            metaService.setInt(MetaService.K_COMPLETE_SCORE, defaultCompleteScore);
        }
        if (metaService.get(MetaService.K_REVIEW_SCORE, null) == null) {
            metaService.setInt(MetaService.K_REVIEW_SCORE, defaultReviewScore);
        }
        if (metaService.get(MetaService.K_AUTO_CLAIM_SCORE, null) == null) {
            metaService.setInt(MetaService.K_AUTO_CLAIM_SCORE, 0);
        }
        if (metaService.get(MetaService.K_ASSIGN_CURSOR, null) == null) {
            metaService.setInt(MetaService.K_ASSIGN_CURSOR, 0);
        }
        if (metaService.get(MetaService.K_REVIEW_CURSOR, null) == null) {
            metaService.setInt(MetaService.K_REVIEW_CURSOR, 0);
        }
    }
}
