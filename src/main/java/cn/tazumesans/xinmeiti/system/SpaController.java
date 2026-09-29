package cn.tazumesans.xinmeiti.system;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/** 前端单页应用路由回退：未匹配到的非静态资源路径一律返回 index.html */
@Controller
public class SpaController {

    @RequestMapping(value = {"/",
            "/{p1:[^\\.]*}",
            "/{p1:[^\\.]*}/{p2:[^\\.]*}",
            "/{p1:[^\\.]*}/{p2:[^\\.]*}/{p3:[^\\.]*}"})
    public String index() {
        return "forward:/index.html";
    }
}
