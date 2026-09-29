package cn.tazumesans.xinmeiti.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("task_log")
public class TaskLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private Long actorId;
    private String action;
    private String detail;
    private String createdAt;

    @TableField(exist = false)
    private String actorName;
}
