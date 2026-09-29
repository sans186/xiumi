package cn.tazumesans.xinmeiti.mapper;

import cn.tazumesans.xinmeiti.model.Group;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface GroupMapper extends BaseMapper<Group> {

    @Select("""
            SELECT g.*, l.nickname AS leader_name,
                   (SELECT COUNT(*) FROM sys_user u WHERE u.group_id = g.id AND u.status='ACTIVE') AS member_count,
                   COALESCE((SELECT SUM(r.score) FROM score_record r
                             JOIN sys_user u2 ON u2.id = r.user_id WHERE u2.group_id = g.id), 0) AS total_score
            FROM sys_group g LEFT JOIN sys_user l ON l.id = g.leader_id
            ORDER BY g.id ASC
            """)
    List<Group> selectWithStats();

    @Select("""
            SELECT g.id, g.name, g.leader_id, l.nickname AS leader_name,
                   COALESCE(m.cnt, 0) AS member_count, COALESCE(s.total, 0) AS score
            FROM sys_group g
            LEFT JOIN sys_user l ON l.id = g.leader_id
            LEFT JOIN (SELECT group_id, COUNT(*) cnt FROM sys_user WHERE status='ACTIVE' GROUP BY group_id) m
                   ON m.group_id = g.id
            LEFT JOIN (SELECT u.group_id, SUM(r.score) total FROM score_record r
                       JOIN sys_user u ON u.id = r.user_id GROUP BY u.group_id) s ON s.group_id = g.id
            ORDER BY score DESC, g.id ASC
            """)
    List<Map<String, Object>> groupRank();
}
