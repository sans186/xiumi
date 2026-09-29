package cn.tazumesans.xinmeiti.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sys_user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String nickname;
    private String passwordHash;
    /** ADMIN / EMPLOYEE */
    private String role;
    private String email;
    private Long groupId;
    /** 1=登录后必须先改密码 */
    private Integer mustChange;
    /** ACTIVE / DISABLED */
    private String status;
    private String lastLoginAt;
    private String createdAt;
    private String updatedAt;

    /* ============ 以下为关联/统计字段（非数据库列） ============ */
    @TableField(exist = false)
    private String groupName;
    @TableField(exist = false)
    private Integer score;
    @TableField(exist = false)
    private Integer completedCount;
    @TableField(exist = false)
    private Integer pendingCount;

    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }
}
