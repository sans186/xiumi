package cn.tazumesans.xinmeiti.task;

import cn.tazumesans.xinmeiti.common.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;

/**
 * 任务附件存储（压缩包 or 单个 docx）。
 * 文件落在 data/uploads/tasks/{taskId}/ 下；重复上传覆盖旧文件，任务作废时删除以释放空间。
 * 已完成任务的附件会保留，便于回看素材。
 */
@Slf4j
@Service
public class AttachmentService {

    /** 允许的附件后缀：压缩包 + Word 文档 */
    private static final String[] ALLOWED_EXT = {".zip", ".rar", ".7z", ".docx"};
    /** 单个附件上限 50MB，与 application.yml 里的 multipart 限制配套 */
    private static final long MAX_SIZE = 50L * 1024 * 1024;

    private final Path root;

    public AttachmentService(@Value("${xmt.upload-dir:./data/uploads}") String dir) {
        this.root = Paths.get(dir).toAbsolutePath().normalize();
    }

    /** 保存附件，返回存储相对路径（相对上传根目录） */
    public String save(Long taskId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        String original = safeName(file.getOriginalFilename());
        String ext = extOf(original);
        if (ext == null) {
            throw BizException.of("附件只支持 zip / rar / 7z 压缩包或 docx 文档");
        }
        if (file.getSize() > MAX_SIZE) {
            throw BizException.of("压缩包不能超过 50MB");
        }
        try {
            Path dir = root.resolve("tasks").resolve(String.valueOf(taskId));
            Files.createDirectories(dir);
            String stored = UUID.randomUUID().toString().replace("-", "").substring(0, 12) + ext;
            Path target = dir.resolve(stored);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return root.relativize(target).toString().replace("\\", "/");
        } catch (IOException e) {
            log.error("保存任务附件失败，taskId={}", taskId, e);
            throw BizException.of("附件保存失败，请重试");
        }
    }

    /** 取回附件真实路径（带目录穿越防护） */
    public Path resolve(String relative) {
        if (relative == null || relative.isBlank()) {
            return null;
        }
        Path p = root.resolve(relative).normalize();
        if (!p.startsWith(root)) {
            throw BizException.of("附件路径非法");
        }
        return p;
    }

    /** 删除附件及其所在目录，不存在则忽略 */
    public void delete(String relative) {
        Path p = resolve(relative);
        if (p == null) {
            return;
        }
        try {
            Files.deleteIfExists(p);
            Path parent = p.getParent();
            if (parent != null && !parent.equals(root) && Files.isDirectory(parent)) {
                try (var list = Files.list(parent)) {
                    if (list.findAny().isEmpty()) {
                        Files.deleteIfExists(parent);
                    }
                }
            }
        } catch (IOException e) {
            log.warn("删除任务附件失败：{}", relative, e);
        }
    }

    private String safeName(String name) {
        if (name == null) {
            return "attachment";
        }
        return name.replace("\\", "/").replaceAll(".*/", "");
    }

    private String extOf(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        for (String ext : ALLOWED_EXT) {
            if (lower.endsWith(ext)) {
                return ext;
            }
        }
        return null;
    }
}
