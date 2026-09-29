package cn.tazumesans.xinmeiti.system;

import cn.tazumesans.xinmeiti.common.Result;
import cn.tazumesans.xinmeiti.security.AuthKit;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final MetaService metaService;
    private final AuthKit authKit;

    @GetMapping
    public Result<Map<String, Object>> get() {
        authKit.requireAdmin();
        Map<String, Object> m = new HashMap<>();
        m.put("claimScore", metaService.getInt(MetaService.K_CLAIM_SCORE, 1));
        m.put("completeScore", metaService.getInt(MetaService.K_COMPLETE_SCORE, 1));
        m.put("reviewScore", metaService.getInt(MetaService.K_REVIEW_SCORE, 1));
        m.put("autoAssignGiveClaimScore", metaService.getInt(MetaService.K_AUTO_CLAIM_SCORE, 0));
        return Result.ok(m);
    }

    @PutMapping
    public Result<?> update(@RequestBody Map<String, Object> body) {
        authKit.requireAdmin();
        if (body.get("claimScore") != null) {
            metaService.setInt(MetaService.K_CLAIM_SCORE, toInt(body.get("claimScore"), 1));
        }
        if (body.get("completeScore") != null) {
            metaService.setInt(MetaService.K_COMPLETE_SCORE, toInt(body.get("completeScore"), 1));
        }
        if (body.get("reviewScore") != null) {
            metaService.setInt(MetaService.K_REVIEW_SCORE, toInt(body.get("reviewScore"), 1));
        }
        if (body.get("autoAssignGiveClaimScore") != null) {
            metaService.setInt(MetaService.K_AUTO_CLAIM_SCORE, toInt(body.get("autoAssignGiveClaimScore"), 0));
        }
        return Result.ok();
    }

    private int toInt(Object v, int def) {
        if (v instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(v));
        } catch (Exception e) {
            return def;
        }
    }
}
