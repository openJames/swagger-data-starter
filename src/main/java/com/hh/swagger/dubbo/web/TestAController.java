package com.hh.swagger.dubbo.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * desc:
 *
 * @author James
 * @since 2022-03-02 9:25
 */
@Controller
public class TestAController {

    @RequestMapping("test")
    @ResponseBody
    public String test() {
        return "AA";
    }
}
