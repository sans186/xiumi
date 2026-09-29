package cn.tazumesans.xinmeiti.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("task")
public class Task {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String content;
    private Long creatorId;
    /** 接取时限，超时自动分配 */
    private String claimDeadline;
    /** 完成时限 */
    private String ddl;
    private Integer claimScore;
    private Integer completeScore;
    private Integer reviewScore;
    private Long assigneeId;
    /** CLAIM 抢单 / AUTO 超时自动 / MANUAL 管理员指派 */
    private String assignType;
    /** OPEN / IN_PROGRESS / PENDING_REVIEW / COMPLETED / CANCELLED */
    private String status;
    private String claimedAt;
    private String submittedAt;
    /** 秀米预览链接 */
    private String previewUrl;
    private String submitNote;
    /** 任务压缩包：原始文件名 / 存储相对路径 / 字节大小 */
    private String attachName;
    private String attachPath;
    private Long attachSize;
    /** 轮询到的审核管理员 */
    private Long reviewerId;
    private String reviewComment;
    private String reviewedAt;
    private String createdAt;
    private String updatedAt;

    /* ============ 关联/计算字段 ============ */
    @TableField(exist = false)
    private String creatorName;
    @TableField(exist = false)
    private String assigneeName;
    @TableField(exist = false)
    private String reviewerName;
    /** ddl 已过且未完成 */
    @TableField(exist = false)
    private Boolean overdue;
    /** 当前登录人能否接取 */
    @TableField(exist = false)
    private Boolean canClaim;
    /** 若超时自动分配，下一个轮到谁 */
    @TableField(exist = false)
    private String nextCandidateName;
}
