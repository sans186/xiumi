package cn.tazumesans.xinmeiti.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.List;

@Data
@TableName("sys_group")
public class Group {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private Long leaderId;
    private String description;
    private String createdAt;

    /* ============ 关联/统计字段 ============ */
    @TableField(exist = false)
    private String leaderName;
    @TableField(exist = false)
    private Integer memberCount;
    @TableField(exist = false)
    private Integer totalScore;
    @TableField(exist = false)
    private List<User> members;
}
