package cn.tazumesans.xinmeiti.mapper;

import cn.tazumesans.xinmeiti.model.ScoreRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface ScoreMapper extends BaseMapper<ScoreRecord> {

    @Select("""
            <script>
            SELECT r.*, u.nickname AS user_name, t.title AS task_title, o.nickname AS operator_name
            FROM score_record r
            LEFT JOIN sys_user u ON u.id = r.user_id
            LEFT JOIN task t ON t.id = r.task_id
            LEFT JOIN sys_user o ON o.id = r.operator_id
            <where>
                <if test="userId != null"> r.user_id = #{userId} </if>
            </where>
            ORDER BY r.id DESC
            LIMIT #{limit}
            </script>
            """)
    List<ScoreRecord> selectWithNames(@Param("userId") Long userId, @Param("limit") int limit);

    /** 个人排行（period=month 时只统计本月） */
    @Select("""
            <script>
            SELECT u.id, u.nickname, u.username, u.group_id, g.name AS group_name,
                   COALESCE(s.total, 0) AS score,
                   COALESCE(d.done, 0) AS completed_count,
                   COALESCE(p.pending, 0) AS pending_count
            FROM sys_user u
            LEFT JOIN sys_group g ON g.id = u.group_id
            LEFT JOIN (
                SELECT user_id, SUM(score) AS total FROM score_record
                <if test="period == 'month'"> WHERE created_at &gt;= #{monthStart} </if>
                GROUP BY user_id
            ) s ON s.user_id = u.id
            LEFT JOIN (
                SELECT assignee_id, COUNT(*) AS done FROM task WHERE status='COMPLETED' GROUP BY assignee_id
            ) d ON d.assignee_id = u.id
            LEFT JOIN (
                SELECT assignee_id, COUNT(*) AS pending FROM task
                WHERE status IN ('IN_PROGRESS','PENDING_REVIEW') GROUP BY assignee_id
            ) p ON p.assignee_id = u.id
            WHERE u.status = 'ACTIVE' AND u.role = 'EMPLOYEE'
            <if test="groupId != null"> AND u.group_id = #{groupId} </if>
            ORDER BY score DESC, completed_count DESC, u.id ASC
            </script>
            """)
    List<Map<String, Object>> rank(@Param("period") String period,
                                   @Param("groupId") Long groupId,
                                   @Param("monthStart") String monthStart);
}
