package cn.tazumesans.xinmeiti.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("score_record")
public class ScoreRecord {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long taskId;
    /** CLAIM / COMPLETE / REVIEW / MANUAL */
    private String type;
    private Integer score;
    private String remark;
    private Long operatorId;
    private String createdAt;

    /* ============ 关联字段 ============ */
    @TableField(exist = false)
    private String userName;
    @TableField(exist = false)
    private String taskTitle;
    @TableField(exist = false)
    private String operatorName;
}
