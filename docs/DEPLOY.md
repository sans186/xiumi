# 部署与运维手册

面向 Ubuntu 22.04 / 24.04。整套东西只有一个 jar、一个 SQLite 文件和一个附件目录，运维成本基本等于零。

- [一、环境要求](#一环境要求)
- [二、首次部署](#二首次部署)
- [三、HTTPS](#三https)
- [四、上线后的初始配置](#四上线后的初始配置)
- [五、升级与回滚](#五升级与回滚)
- [六、备份与恢复](#六备份与恢复)
- [七、排障手册](#七排障手册)
- [八、常用命令速查](#八常用命令速查)

## 一、环境要求

| 项 | 要求 | 说明 |
| --- | --- | --- |
| 系统 | Ubuntu 22.04+ / 任意主流 Linux | 其他发行版把 apt 换成对应包管理器即可 |
| JDK | **21**（运行只要 JRE） | `apt-get install -y openjdk-21-jre-headless` |
| 内存 | 建议 ≥ 1GB 可用 | JVM 按 `-Xmx256m` 配置，实测常驻约 210MB |
| 磁盘 | ≥ 1GB | jar 约 40MB + 数据库 + 附件 |
| 反向代理 | nginx（可换 caddy） | 上传附件需要放开请求体大小 |
| 端口 | 应用监听 **127.0.0.1:18081** | 只对本机开放，外网一律走 nginx |

## 二、首次部署

```bash
# 1. 运行环境
sudo apt-get update
sudo apt-get install -y openjdk-21-jre-headless

# 2. 目录（用非 root 账号跑服务，这里示例账号 deploy）
sudo mkdir -p /opt/xinmeiti/data
sudo chown -R deploy:deploy /opt/xinmeiti

# 3. 上传产物
#    本地执行（Windows 上是 .\mvnw.cmd -DskipTests package）
./mvnw -DskipTests package
scp target/xinmeiti.jar        <host>:/opt/xinmeiti/
# 想保留已有数据再传这两个（新装可跳过）
scp data/xinmeiti.db           <host>:/opt/xinmeiti/data/
scp -r data/uploads            <host>:/opt/xinmeiti/data/
```

### systemd

`/etc/systemd/system/xinmeiti.service`：

```ini
[Unit]
Description=xinmeiti - 新媒体小组管理系统 (Spring Boot)
After=network.target

[Service]
Type=simple
User=deploy
Group=deploy
WorkingDirectory=/opt/xinmeiti

# 时区必须设置：接取时限 / DDL 是按本地时间字符串比较的
Environment=TZ=Asia/Shanghai
Environment=SERVER_PORT=18081
Environment=XMT_DB_PATH=/opt/xinmeiti/data/xinmeiti.db
Environment=XMT_UPLOAD_DIR=/opt/xinmeiti/data/uploads
# 邮件通知（可选，不配就只发站内消息）
# Environment=MAIL_HOST=smtp.exmail.qq.com
# Environment=MAIL_PORT=465
# Environment=MAIL_USERNAME=noreply@example.com
# Environment=MAIL_PASSWORD=********
# Environment=XMT_SITE_URL=https://xinmeiti.example.com

ExecStart=/usr/bin/java -Xmx256m -Xss512k -XX:MaxMetaspaceSize=160m -XX:+UseSerialGC \
          -XX:+ExitOnOutOfMemoryError -Dfile.encoding=UTF-8 -jar /opt/xinmeiti/xinmeiti.jar

Restart=always
RestartSec=10
SuccessExitStatus=143

[Install]
WantedBy=multi-user.target
```

```bash
sudo systemctl daemon-reload
sudo systemctl enable --now xinmeiti
systemctl status xinmeiti --no-pager
sudo journalctl -u xinmeiti -f
```

### nginx

`/etc/nginx/sites-available/xinmeiti`：

```nginx
server {
    listen 80;
    listen [::]:80;
    server_name xinmeiti.example.com;

    # 附件上限 50MB，这里给到 60M 留余量；不加这行上传会 413
    client_max_body_size 60M;

    location / {
        client_body_buffer_size 10M;
        proxy_request_buffering off;
        proxy_pass http://127.0.0.1:18081;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_read_timeout 300s;
        proxy_send_timeout 300s;
    }
}
```

```bash
sudo ln -sf /etc/nginx/sites-available/xinmeiti /etc/nginx/sites-enabled/xinmeiti
sudo nginx -t && sudo systemctl reload nginx
```

> 多站点并存时注意：**443 上如果没有匹配的 server_name，nginx 会拿默认站点的证书来应答**，浏览器会报"证书名称不匹配"。所以子域名解析一生效就立刻签证书（见下一节），别只挂 80。

## 三、HTTPS

```bash
sudo apt-get install -y certbot python3-certbot-nginx
sudo certbot --nginx -d xinmeiti.example.com --redirect
```

certbot 会自动：签发证书 → 改写 nginx 配置（补 443 段、加 80→443 跳转）→ 注册续期定时任务。

```bash
sudo certbot renew --dry-run     # 验证续期逻辑
systemctl list-timers | grep certbot
```

## 四、上线后的初始配置

1. 浏览器登录默认管理员 `admin / admin123`，**系统会强制要求改密码**（这是设计好的，账号被重置过也一样）。
2. 「成员管理」建账号 → 把初始密码发给本人 → 对方首次登录同样会被要求改密。
3. 想改默认分值和"自动分配是否也给接取分"，去「系统设置」。
4. 要发邮件通知就配上 `MAIL_*` 环境变量后重启；不配只发站内消息，不影响使用。

## 五、升级与回滚

```bash
# 本地
./mvnw -DskipTests package
scp target/xinmeiti.jar <host>:/opt/xinmeiti/xinmeiti.jar.new

# 服务器：先备份，再原子替换
cd /opt/xinmeiti
cp xinmeiti.jar xinmeiti.jar.bak
mv xinmeiti.jar.new xinmeiti.jar
sudo systemctl restart xinmeiti
sudo journalctl -u xinmeiti -n 50 --no-pager
```

- **前端**没有单独部署步骤：`npm run build` 的产物直接进 `src/main/resources/static`，跟着 jar 一起发。
- **数据库结构变更**由启动时的自动补列处理（对比 `PRAGMA table_info` 后 `ALTER TABLE ADD COLUMN`），升级不用手工改表。
- **回滚**：把 `xinmeiti.jar.bak` 换回来重启即可；新版本如果只是加了列，老版本忽略这些列也能跑。

## 六、备份与恢复

数据只有两处：SQLite 文件 + 附件目录。

```bash
# 在线备份（SQLite 开了 WAL，直接 cp 主文件可能丢最近的写入）
sqlite3 /opt/xinmeiti/data/xinmeiti.db ".backup '/opt/xinmeiti/backup/xinmeiti-$(date +%F).db'"

# 或者停服务后连同 -wal / -shm 一起拷
sudo systemctl stop xinmeiti
cp -a /opt/xinmeiti/data/xinmeiti.db* /opt/xinmeiti/backup/
sudo systemctl start xinmeiti

# 附件
tar czf /opt/xinmeiti/backup/uploads-$(date +%F).tar.gz -C /opt/xinmeiti/data uploads
```

恢复就是把文件放回 `/opt/xinmeiti/data/` 再重启服务。顺手可以加个 crontab 每天备份、只留最近 14 天。

## 七、排障手册

| 现象 | 排查 |
| --- | --- |
| 服务起不来 | `journalctl -u xinmeiti -n 100 --no-pager`；最常见是端口被占（`ss -lntp \| grep 18081`）或 JDK 版本低于 21 |
| 502 Bad Gateway | nginx 连不上应用：确认 `systemctl is-active xinmeiti`、`ss -lntp \| grep 18081`，以及 nginx 里 `proxy_pass` 端口是否一致 |
| 上传附件报 **413** | nginx `client_max_body_size` 没放开（默认 1M），改成 `60M` 后 reload |
| 上传报"文件太大" | 超过 50MB 的业务上限，属于预期行为；要放宽同时改 `spring.servlet.multipart.max-file-size` 和 `AttachmentService.MAX_SIZE` |
| 超时轮派 / 逾期判断不准 | 检查 `TZ`：容器或 systemd 里必须显式设 `Asia/Shanghai`，否则时间字符串会对不上 |
| 页面样式是旧的 / 按钮点了没反应 | 构建后 `index.html` 引用的 hash 资源已变。本项目已给 `index.html` 加 `Cache-Control: no-store`，遇到异常先 `Ctrl+F5` 硬刷 |
| 内存吃紧 / 被 OOM Killer 杀 | `ps -o rss,args -C java` 看常驻；必要时把 `-Xmx256m` 降到 192m，或加 swap（`fallocate` + `mkswap`） |
| `disk full` | 附件目录 `du -sh /opt/xinmeiti/data/uploads`，作废任务会自动清理，异常残留可以按数据库引用清理 |
| 忘记管理员密码 | 用另一个管理员重置；一个在职管理员都没有时，**重启服务会自动恢复最早的管理员为在职**，再用 `XMT_ADMIN_PASSWORD` 对不上时可直接改库（口令是 PBKDF2-HMAC-SHA256，120000 次迭代，`pbkdf2$迭代$salt$hash`） |

日志：应用日志进 journal（`journalctl -u xinmeiti`），nginx 在 `/var/log/nginx/`。应用自身的日志级别在 `application.yml` 的 `logging.level` 调。

## 八、常用命令速查

```bash
sudo systemctl restart xinmeiti          # 重启
sudo systemctl status xinmeiti           # 状态
sudo journalctl -u xinmeiti -f           # 实时日志
sudo journalctl -u xinmeiti --since "10 min ago"
sudo nginx -t && sudo systemctl reload nginx
curl -I http://127.0.0.1:18081/          # 直连应用自检（绕过 nginx）
curl -I -H 'Host: xinmeiti.example.com' http://127.0.0.1/   # 经 nginx 自检
systemctl is-enabled xinmeiti            # 是否开机自启
```
