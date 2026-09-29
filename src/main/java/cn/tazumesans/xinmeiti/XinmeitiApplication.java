package cn.tazumesans.xinmeiti;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@EnableScheduling
@SpringBootApplication
public class XinmeitiApplication {

    public static void main(String[] args) {
        ensureDbDir();
        SpringApplication.run(XinmeitiApplication.class, args);
    }

    /** SQLite 不会自动创建目录，启动前先把数据目录建好 */
    private static void ensureDbDir() {
        try {
            String path = System.getenv("XMT_DB_PATH");
            if (path == null || path.isBlank()) {
                path = System.getProperty("XMT_DB_PATH", "./data/xinmeiti.db");
            }
            Path parent = Path.of(path).toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (IOException e) {
            throw new IllegalStateException("无法创建数据库目录", e);
        }
    }
}
