package cn.tazumesans.xinmeiti.mapper;

import cn.tazumesans.xinmeiti.model.TaskLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TaskLogMapper extends BaseMapper<TaskLog> {

    @Select("""
            SELECT l.*, u.nickname AS actor_name
            FROM task_log l LEFT JOIN sys_user u ON u.id = l.actor_id
            WHERE l.task_id = #{taskId}
            ORDER BY l.id ASC
            """)
    List<TaskLog> selectByTask(@Param("taskId") Long taskId);
}
