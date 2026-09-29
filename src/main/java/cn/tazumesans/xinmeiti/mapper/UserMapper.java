package cn.tazumesans.xinmeiti.mapper;

import cn.tazumesans.xinmeiti.model.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select("""
            SELECT u.*, g.name AS group_name
            FROM sys_user u LEFT JOIN sys_group g ON g.id = u.group_id
            ORDER BY CASE u.role WHEN 'ADMIN' THEN 0 ELSE 1 END, u.id ASC
            """)
    List<User> selectWithGroup();

    /** 参与自动轮派的员工（按 id 升序，保证顺序稳定） */
    @Select("""
            SELECT u.*, g.name AS group_name
            FROM sys_user u LEFT JOIN sys_group g ON g.id = u.group_id
            WHERE u.status = 'ACTIVE' AND u.role = 'EMPLOYEE'
            ORDER BY u.id ASC
            """)
    List<User> selectActiveEmployees();

    @Select("""
            SELECT * FROM sys_user
            WHERE status = 'ACTIVE' AND role = 'ADMIN'
            ORDER BY id ASC
            """)
    List<User> selectActiveAdmins();

    @Select("""
            SELECT id, username, nickname, role, email,
                   COALESCE((SELECT SUM(score) FROM score_record r WHERE r.user_id = u.id), 0) AS score
            FROM sys_user u WHERE u.group_id = #{groupId} ORDER BY u.id ASC
            """)
    List<User> selectMembers(@org.apache.ibatis.annotations.Param("groupId") Long groupId);
}
