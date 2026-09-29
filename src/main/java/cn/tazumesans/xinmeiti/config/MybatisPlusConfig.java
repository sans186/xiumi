package cn.tazumesans.xinmeiti.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("cn.tazumesans.xinmeiti.mapper")
public class MybatisPlusConfig {
}
