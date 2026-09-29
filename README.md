# 新媒体小组 · 任务与积分管理系统

面向 20 人以内的新媒体小组：管理员发布公众号（秀米）排版任务 → 成员抢单 / 超时自动轮派 → 提交秀米预览链接 → 管理员轮流审核 → 积分 + 排行榜。

## 技术栈

| 层 | 选型 | 说明 |
| --- | --- | --- |
| 后端 | Spring Boot 4.1.1 + JDK 21 | 单 jar 运行，`-Xmx256m` 即可 |
| 鉴权 | Sa-Token 1.46.0（Boot4 starter） | 登录态、7 天有效期，请求头 `satoken` 传 token |
| 持久层 | MyBatis-Plus 3.5.17 + SQLite | 单文件数据库 `./data/xinmeiti.db`，零安装零运维 |
| 前端 | Vue 3 + Vite + Tailwind CSS 4 | 移动端优先，PC 侧边栏 / 手机底部导航 |
| 通知 | 站内消息 + 邮件（SMTP 可选） | 不配 SMTP 就只走站内消息 |

## 快速开始

```bash
# 1. 前端构建（产物输出到 src/main/resources/static）
cd frontend && npm install && npm run build

# 2. 后端打包
cd .. && mvnw -DskipTests package      # Windows: .\mvnw.cmd

# 3. 运行（默认 8080，可用 SERVER_PORT 改）
java -Xmx256m -jar target/xinmeiti.jar
# 或：set SERVER_PORT=9090 && java -Xmx256m -jar target/xinmeiti.jar
```

打开 http://localhost:8080 ，用默认管理员登录：**admin / admin123**（首次登录强制改密码）。

开发模式（前后端分离热更新）：

```bash
java -jar target/xinmeiti.jar          # 后端 8080
cd frontend && npm run dev             # 前端 5173，已配 /api 代理
```

## 业务规则

1. **账号**：只能管理员创建，带初始密码；新账号登录后被强制改密码（不改不能用任何功能）。
2. **接取**：任务有「接取时限」（发布时默认 12 小时），时限内没人接 → 系统按成员顺序自动分配并通知本人；过了接取时限也不能再抢单。
   - 主动抢单得「接取分」；自动分配默认**不给**接取分（可在系统设置里打开），但按时完成、审核通过的分照给。
3. **完成**：DDL（默认 36 小时）前提交秀米预览链接 → 得「按时完成分」；
   - 逾期提交仍可提交，**不加完成分，并且扣回已经拿到的接取分**（只扣一次）。
4. **提交/重提**：交完还在「待审核」时，本人可以随时改完重新提交（换新的秀米预览链接），系统会通知原审核人；被打回后同样可以改完再交。
5. **审核**：提交后系统按管理员顺序**轮流**指派审核人（其他人也可以代审）。
   - **评语可选**（打回也不强制）；
   - 通过时管理员可以选**通过并给分**或**通过不给分**（质量好才给分）；
   - 完成分只记一次，重提不重复加。
6. **附件**：发布任务时可挂一个附件——`zip / rar / 7z` 压缩包或单个 `docx` 文档（≤50MB），详情页可下载；任务作废时自动清理，已完成任务保留。
7. **积分**：默认每项 1 分，发布任务时可单独设置；管理员可手动加减分。
8. **排名**：总榜 / 本月榜 / 分组榜，按积分降序，同分按完成任务数。
9. **小组**：可建组，组长可以是管理员也可以是普通成员，组长同样能发布任务。

## 配置项（application.yml 或环境变量）

| 配置 | 默认 | 说明 |
| --- | --- | --- |
| `SERVER_PORT` | 8080 | 端口 |
| `XMT_DB_PATH` | `./data/xinmeiti.db` | SQLite 文件路径 |
| `XMT_UPLOAD_DIR` | `./data/uploads` | 任务附件存放目录 |
| `XMT_ADMIN_USERNAME` / `XMT_ADMIN_PASSWORD` | admin / admin123 | 首次启动自动创建的管理员 |
| `MAIL_HOST` / `MAIL_PORT` / `MAIL_USERNAME` / `MAIL_PASSWORD` | 空 | 配置后启用邮件通知 |
| `XMT_MAIL_ENABLED` | true | 邮件总开关 |
| `XMT_SITE_URL` | http://localhost:8080 | 邮件里的跳转地址 |

## 目录结构

```
src/main/java/cn/tazumesans/xinmeiti/
├── common/     统一响应、异常、时间与口令哈希
├── config/     MyBatis-Plus、Web（拦截器 / CORS）
├── security/   AuthKit（当前登录人）+ 登录拦截器（强制改密）
├── model/      MP 实体
├── mapper/     MP Mapper（含连表查询）
├── user/       账号、登录、成员管理
├── group/      小组
├── task/       任务（发布 / 抢单 / 自动轮派 / 提交 / 审核）+ 定时任务
├── score/      积分与排行榜
├── notify/     站内消息
└── system/     启动引导、系统配置、邮件、SPA 路由回退
```

## 备份

数据全在 `data/xinmeiti.db` 一个文件里，直接拷贝即可备份（建议停服务后拷贝，或连同 `-wal`/`-shm` 一起拷）。
附件在 `data/uploads/`，一并备份即可。

## 线上部署（Ubuntu + systemd + nginx）

服务器环境：Ubuntu 22.04、`openjdk-21-jre-headless`、nginx。

```bash
# 1. 目录与 JDK
sudo apt-get install -y openjdk-21-jre-headless
sudo mkdir -p /opt/xinmeiti/data && sudo chown -R deploy:deploy /opt/xinmeiti

# 2. 上传产物（本地构建好的 jar + 可选的数据文件）
scp target/xinmeiti.jar            xiaoyu:/opt/xinmeiti/
scp data/xinmeiti.db               xiaoyu:/opt/xinmeiti/data/     # 想保留数据就传
scp -r data/uploads                xiaoyu:/opt/xinmeiti/data/     # 已有附件
```

`/etc/systemd/system/xinmeiti.service`：

```ini
[Unit]
Description=xinmeiti - 新媒体小组管理系统 (Spring Boot)
After=network.target

[Service]
Type=simple
User=deploy
WorkingDirectory=/opt/xinmeiti
Environment=TZ=Asia/Shanghai
Environment=SERVER_PORT=18081
Environment=XMT_DB_PATH=/opt/xinmeiti/data/xinmeiti.db
Environment=XMT_UPLOAD_DIR=/opt/xinmeiti/data/uploads
ExecStart=/usr/bin/java -Xmx256m -Xss512k -XX:MaxMetaspaceSize=160m -XX:+UseSerialGC -Dfile.encoding=UTF-8 -jar /opt/xinmeiti/xinmeiti.jar
Restart=always
RestartSec=10
SuccessExitStatus=143

[Install]
WantedBy=multi-user.target
```

```bash
sudo systemctl daemon-reload && sudo systemctl enable --now xinmeiti
sudo systemctl status xinmeiti          # 看状态
sudo journalctl -u xinmeiti -f          # 看日志
```

nginx 站点 `/etc/nginx/sites-available/xinmeiti`（`server_name xinmeiti.tazume-sans.cn`）：

```nginx
server {
    listen 80;
    listen [::]:80;
    server_name xinmeiti.tazume-sans.cn;
    client_max_body_size 60M;            # 附件最大 50MB，留余量
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
sudo certbot --nginx -d xinmeiti.tazume-sans.cn     # DNS 解析生效后签发 HTTPS
```

> 注意：`TZ=Asia/Shanghai` 必须设置——任务的接取时限 / DDL 都是按本地时间字符串比较的。
> 升级：本地重新 `mvnw package` → `scp` 覆盖 jar → `sudo systemctl restart xinmeiti`（数据库自动补列，不用手工改表）。
