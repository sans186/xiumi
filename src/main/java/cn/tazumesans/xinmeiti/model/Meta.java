package cn.tazumesans.xinmeiti.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sys_meta")
public class Meta {

    @TableId(value = "k", type = IdType.INPUT)
    private String k;
    private String v;
}
