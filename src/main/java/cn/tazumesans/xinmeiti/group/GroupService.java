package cn.tazumesans.xinmeiti.group;

import cn.tazumesans.xinmeiti.common.BizException;
import cn.tazumesans.xinmeiti.common.TimeUtil;
import cn.tazumesans.xinmeiti.mapper.GroupMapper;
import cn.tazumesans.xinmeiti.model.Group;
import cn.tazumesans.xinmeiti.model.User;
import cn.tazumesans.xinmeiti.user.UserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class GroupService extends ServiceImpl<GroupMapper, Group> {

    private final UserService userService;

    public GroupService(GroupMapper groupMapper, UserService userService) {
        this.baseMapper = groupMapper;
        this.userService = userService;
    }

    public List<Group> listWithMembers() {
        List<Group> groups = baseMapper.selectWithStats();
        groups.forEach(g -> g.setMembers(members(g.getId())));
        return groups;
    }

    public List<User> members(Long groupId) {
        return userService.getBaseMapper().selectMembers(groupId);
    }

    public Long create(String name, Long leaderId, String description) {
        if (name == null || name.isBlank()) {
            throw BizException.of("请填写组名");
        }
        Long dup = count(new LambdaQueryWrapper<Group>().eq(Group::getName, name.trim()));
        if (dup != null && dup > 0) {
            throw BizException.of("组名已存在");
        }
        Group g = new Group();
        g.setName(name.trim());
        g.setLeaderId(leaderId);
        g.setDescription(description);
        g.setCreatedAt(TimeUtil.now());
        save(g);
        return g.getId();
    }

    public void updateGroup(Long id, String name, Long leaderId, String description) {
        if (name == null || name.isBlank()) {
            throw BizException.of("请填写组名");
        }
        lambdaUpdate().eq(Group::getId, id)
                .set(Group::getName, name.trim())
                .set(Group::getLeaderId, leaderId)
                .set(Group::getDescription, description)
                .update();
    }

    public void deleteGroup(Long id) {
        userService.lambdaUpdate().eq(User::getGroupId, id).set(User::getGroupId, null).update();
        removeById(id);
    }

    /** 全量覆盖组成员 */
    public void setMembers(Long groupId, List<Long> userIds) {
        userService.lambdaUpdate().eq(User::getGroupId, groupId).set(User::getGroupId, null).update();
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        for (Long uid : userIds) {
            userService.lambdaUpdate().eq(User::getId, uid).set(User::getGroupId, groupId).update();
        }
    }

    public List<Map<String, Object>> groupRank() {
        return baseMapper.groupRank();
    }
}
