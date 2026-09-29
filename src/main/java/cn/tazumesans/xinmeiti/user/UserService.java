package cn.tazumesans.xinmeiti.user;

import cn.tazumesans.xinmeiti.common.BizException;
import cn.tazumesans.xinmeiti.common.PasswordHasher;
import cn.tazumesans.xinmeiti.common.TimeUtil;
import cn.tazumesans.xinmeiti.mapper.UserMapper;
import cn.tazumesans.xinmeiti.model.User;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class UserService extends ServiceImpl<UserMapper, User> {

    private static final Pattern USERNAME = Pattern.compile("^[A-Za-z0-9_]{3,20}$");

    public User findByUsername(String username) {
        return getOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username), false);
    }

    /** 带小组名的完整列表 */
    public List<User> listAll() {
        return baseMapper.selectWithGroup();
    }

    /** 参与自动轮派的员工（按 id 升序） */
    public List<User> listActiveEmployees() {
        return baseMapper.selectActiveEmployees();
    }

    public List<User> listActiveAdmins() {
        return baseMapper.selectActiveAdmins();
    }

    /** 管理员建号：账号不由用户自己注册 */
    public User create(String username, String nickname, String role, String email,
                       Long groupId, String initialPassword) {
        if (username == null || !USERNAME.matcher(username).matches()) {
            throw BizException.of("登录名需为 3-20 位字母、数字或下划线");
        }
        if (findByUsername(username) != null) {
            throw BizException.of("登录名已存在");
        }
        if (nickname == null || nickname.isBlank()) {
            throw BizException.of("请填写姓名/昵称");
        }
        String pwd = (initialPassword == null || initialPassword.isBlank())
                ? PasswordHasher.randomPassword() : initialPassword;
        if (pwd.length() < 6) {
            throw BizException.of("初始密码至少 6 位");
        }
        User u = new User();
        u.setUsername(username.trim());
        u.setNickname(nickname.trim());
        u.setPasswordHash(PasswordHasher.hash(pwd));
        u.setRole("ADMIN".equals(role) ? "ADMIN" : "EMPLOYEE");
        u.setEmail(blankToNull(email));
        u.setGroupId(groupId);
        u.setMustChange(1);
        u.setStatus("ACTIVE");
        u.setCreatedAt(TimeUtil.now());
        u.setUpdatedAt(TimeUtil.now());
        save(u);
        return u;
    }

    /**
     * 管理员编辑成员。为防止误操作把自己或唯一的在职管理员停用/降级（会导致无人能登录管理后台），
     * 这里做两道保护：不能动自己、系统至少保留一位在职管理员。
     */
    public void updateUser(Long id, String nickname, String role, String email, Long groupId,
                           String status, Long operatorId) {
        User target = getById(id);
        if (target == null) {
            throw BizException.of("用户不存在");
        }
        boolean self = operatorId != null && operatorId.equals(id);
        if (self && "DISABLED".equals(status)) {
            throw BizException.of("不能停用自己的账号");
        }
        if (self && role != null && !"ADMIN".equals(role) && "ADMIN".equals(target.getRole())) {
            throw BizException.of("不能取消自己的管理员身份");
        }
        boolean demoteAdmin = "ADMIN".equals(target.getRole())
                && ("DISABLED".equals(status) || (role != null && !"ADMIN".equals(role)));
        if (demoteAdmin) {
            long activeAdmins = count(new LambdaQueryWrapper<User>()
                    .eq(User::getRole, "ADMIN")
                    .eq(User::getStatus, "ACTIVE"));
            if (activeAdmins <= 1) {
                throw BizException.of("系统必须保留至少一位在职管理员");
            }
        }
        lambdaUpdate().eq(User::getId, id)
                .set(nickname != null, User::getNickname, nickname)
                .set(role != null, User::getRole, role)
                .set(User::getEmail, blankToNull(email))
                .set(User::getGroupId, groupId)
                .set(status != null, User::getStatus, status)
                .set(User::getUpdatedAt, TimeUtil.now())
                .update();
    }

    /** 重置密码后用户下次登录必须改密码，返回实际密码 */
    public String resetPassword(Long id, String newPassword) {
        String pwd = (newPassword == null || newPassword.isBlank())
                ? PasswordHasher.randomPassword() : newPassword;
        if (pwd.length() < 6) {
            throw BizException.of("密码至少 6 位");
        }
        lambdaUpdate().eq(User::getId, id)
                .set(User::getPasswordHash, PasswordHasher.hash(pwd))
                .set(User::getMustChange, 1)
                .set(User::getUpdatedAt, TimeUtil.now())
                .update();
        return pwd;
    }

    public void changePassword(Long id, String oldPassword, String newPassword) {
        User u = getById(id);
        if (u == null) {
            throw BizException.of("用户不存在");
        }
        if (!PasswordHasher.matches(oldPassword, u.getPasswordHash())) {
            throw BizException.of("原密码不正确");
        }
        if (newPassword == null || newPassword.length() < 6) {
            throw BizException.of("新密码至少 6 位");
        }
        if (newPassword.equals(oldPassword)) {
            throw BizException.of("新密码不能与原密码相同");
        }
        lambdaUpdate().eq(User::getId, id)
                .set(User::getPasswordHash, PasswordHasher.hash(newPassword))
                .set(User::getMustChange, 0)
                .set(User::getUpdatedAt, TimeUtil.now())
                .update();
    }

    public void touchLogin(Long id) {
        lambdaUpdate().eq(User::getId, id).set(User::getLastLoginAt, TimeUtil.now()).update();
    }

    public Map<Long, String> emailMap(List<Long> ids) {
        Map<Long, String> map = new HashMap<>();
        if (ids == null || ids.isEmpty()) {
            return map;
        }
        List<User> users = listByIds(ids);
        users.forEach(u -> map.put(u.getId(), u.getEmail()));
        return map;
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
