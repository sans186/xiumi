package cn.tazumesans.xinmeiti.system;

import cn.tazumesans.xinmeiti.mapper.MetaMapper;
import cn.tazumesans.xinmeiti.model.Meta;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/** sys_meta 键值配置（轮转游标、默认分值、开关等） */
@Service
public class MetaService extends ServiceImpl<MetaMapper, Meta> {

    public static final String K_CLAIM_SCORE = "default_claim_score";
    public static final String K_COMPLETE_SCORE = "default_complete_score";
    public static final String K_REVIEW_SCORE = "default_review_score";
    public static final String K_AUTO_CLAIM_SCORE = "auto_assign_give_claim_score";
    public static final String K_ASSIGN_CURSOR = "assign_cursor_user_id";
    public static final String K_REVIEW_CURSOR = "review_cursor_admin_id";

    public String get(String key, String def) {
        Meta m = getById(key);
        return m == null ? def : m.getV();
    }

    public int getInt(String key, int def) {
        String v = get(key, null);
        if (v == null || v.isBlank()) {
            return def;
        }
        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    public void set(String key, String value) {
        Meta m = new Meta();
        m.setK(key);
        m.setV(value);
        // SQLite 支持 upsert，直接用 XML 之外的通用方式：先查后写
        if (getById(key) == null) {
            save(m);
        } else {
            lambdaUpdate().eq(Meta::getK, key).set(Meta::getV, value).update();
        }
    }

    public void setInt(String key, int value) {
        set(key, String.valueOf(value));
    }
}
