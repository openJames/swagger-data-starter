package com.hh.swagger.dubbo.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

/**
 * desc:
 *
 * @author James
 * @since 2022-03-02 7:49
 */
@Conditional(EnableDubboSwaggerConfigCondition.class)
@ComponentScan(basePackages = {"com.hh.swagger.dubbo.config", "com.hh.swagger.dubbo.web"})
@Slf4j
public class EnableDubboSwaggerConfig {

    public EnableDubboSwaggerConfig() {
       log.info("init swagger configuration start ^^_^^_^^_^^_^ smile ^_^^_^^_^^_^^_");
    }
}
