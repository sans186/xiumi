package cn.tazumesans.xinmeiti.group;

import cn.tazumesans.xinmeiti.common.Result;
import cn.tazumesans.xinmeiti.model.Group;
import cn.tazumesans.xinmeiti.security.AuthKit;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;
    private final AuthKit authKit;

    public record GroupRequest(String name, Long leaderId, String description, List<Long> memberIds) {
    }

    @GetMapping
    public Result<List<Group>> list() {
        return Result.ok(groupService.listWithMembers());
    }

    @PostMapping
    public Result<Map<String, Long>> create(@RequestBody GroupRequest req) {
        authKit.requireAdmin();
        Long id = groupService.create(req.name(), req.leaderId(), req.description());
        groupService.setMembers(id, req.memberIds());
        return Result.ok(Map.of("id", id));
    }

    @PutMapping("/{id}")
    public Result<?> update(@PathVariable Long id, @RequestBody GroupRequest req) {
        authKit.requireAdmin();
        groupService.updateGroup(id, req.name(), req.leaderId(), req.description());
        if (req.memberIds() != null) {
            groupService.setMembers(id, req.memberIds());
        }
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        authKit.requireAdmin();
        groupService.deleteGroup(id);
        return Result.ok();
    }
}
