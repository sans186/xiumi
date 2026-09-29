package cn.tazumesans.xinmeiti.security;

import cn.dev33.satoken.stp.StpUtil;
import cn.tazumesans.xinmeiti.common.BizException;
import cn.tazumesans.xinmeiti.mapper.GroupMapper;
import cn.tazumesans.xinmeiti.mapper.UserMapper;
import cn.tazumesans.xinmeiti.model.User;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 基于 Sa-Token 的当前登录人工具 */
@Component
@RequiredArgsConstructor
public class AuthKit {

    private final UserMapper userMapper;
    private final GroupMapper groupMapper;

    public Long id() {
        return StpUtil.getLoginIdAsLong();
    }

    public User user() {
        User u = userMapper.selectById(id());
        if (u == null) {
            throw new BizException(BizException.NOT_LOGIN, "登录已失效，请重新登录");
        }
        return u;
    }

    public boolean isAdmin() {
        return "ADMIN".equals(user().getRole());
    }

    /** 员工也可以当组长，组长同样可以发布任务 */
    public boolean isGroupLeader(Long userId) {
        Long c = groupMapper.selectCount(new LambdaQueryWrapper<cn.tazumesans.xinmeiti.model.Group>()
                .eq(cn.tazumesans.xinmeiti.model.Group::getLeaderId, userId));
        return c != null && c > 0;
    }

    public boolean canPublish() {
        Long uid = id();
        return isAdmin() || isGroupLeader(uid);
    }

    public void requireAdmin() {
        if (!isAdmin()) {
            throw new BizException(BizException.NO_AUTH, "只有管理员可以操作");
        }
    }

    public void requireCanPublish() {
        if (!canPublish()) {
            throw new BizException(BizException.NO_AUTH, "只有管理员或组长可以发布任务");
        }
    }
}
