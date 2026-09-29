package cn.tazumesans.xinmeiti.score;

import cn.tazumesans.xinmeiti.common.TimeUtil;
import cn.tazumesans.xinmeiti.mapper.ScoreMapper;
import cn.tazumesans.xinmeiti.model.ScoreRecord;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ScoreService extends ServiceImpl<ScoreMapper, ScoreRecord> {

    /** 记一笔积分 */
    public void add(Long userId, Long taskId, String type, int score, String remark, Long operatorId) {
        ScoreRecord r = new ScoreRecord();
        r.setUserId(userId);
        r.setTaskId(taskId);
        r.setType(type);
        r.setScore(score);
        r.setRemark(remark);
        r.setOperatorId(operatorId);
        r.setCreatedAt(TimeUtil.now());
        save(r);
    }

    public int totalOf(Long userId) {
        List<ScoreRecord> list = list(new LambdaQueryWrapper<ScoreRecord>()
                .eq(ScoreRecord::getUserId, userId));
        return list.stream().mapToInt(r -> r.getScore() == null ? 0 : r.getScore()).sum();
    }

    /** 某个任务下某类积分的累计值（可能为负），没有记录返回 0 */
    public int sumOf(Long taskId, Long userId, String type) {
        List<ScoreRecord> list = list(new LambdaQueryWrapper<ScoreRecord>()
                .eq(ScoreRecord::getTaskId, taskId)
                .eq(ScoreRecord::getUserId, userId)
                .eq(ScoreRecord::getType, type));
        return list.stream().mapToInt(r -> r.getScore() == null ? 0 : r.getScore()).sum();
    }

    public boolean exists(Long taskId, Long userId, String type) {
        Long c = count(new LambdaQueryWrapper<ScoreRecord>()
                .eq(ScoreRecord::getTaskId, taskId)
                .eq(ScoreRecord::getUserId, userId)
                .eq(ScoreRecord::getType, type));
        return c != null && c > 0;
    }

    public List<ScoreRecord> listByUser(Long userId, int limit) {
        return baseMapper.selectWithNames(userId, limit);
    }

    public List<ScoreRecord> recent(int limit) {
        return baseMapper.selectWithNames(null, limit);
    }

    /** 个人榜：period = all | month */
    public List<Map<String, Object>> rank(String period, Long groupId) {
        return baseMapper.rank(period, groupId, TimeUtil.monthStart());
    }

    /** 我的积分 / 排名 / 任务数 */
    public Map<String, Object> mySummary(Long userId) {
        Map<String, Object> res = new HashMap<>();
        res.put("score", totalOf(userId));
        List<Map<String, Object>> all = rank("all", null);
        int rankNo = 0;
        for (int i = 0; i < all.size(); i++) {
            Object id = all.get(i).get("id");
            if (id != null && ((Number) id).longValue() == userId) {
                rankNo = i + 1;
                break;
            }
        }
        res.put("rank", rankNo);
        res.put("totalMembers", all.size());
        res.put("recent", listByUser(userId, 5));
        return res;
    }
}
