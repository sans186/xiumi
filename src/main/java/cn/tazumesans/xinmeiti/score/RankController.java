package cn.tazumesans.xinmeiti.score;

import cn.tazumesans.xinmeiti.common.Result;
import cn.tazumesans.xinmeiti.group.GroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rank")
@RequiredArgsConstructor
public class RankController {

    private final ScoreService scoreService;
    private final GroupService groupService;

    /** 个人榜：period = all | month，groupId 为空为总榜 */
    @GetMapping
    public Result<List<Map<String, Object>>> rank(@RequestParam(defaultValue = "all") String period,
                                                  @RequestParam(required = false) Long groupId) {
        return Result.ok(scoreService.rank(period, groupId));
    }

    /** 小组榜 */
    @GetMapping("/groups")
    public Result<List<Map<String, Object>>> groupRank() {
        return Result.ok(groupService.groupRank());
    }
}
