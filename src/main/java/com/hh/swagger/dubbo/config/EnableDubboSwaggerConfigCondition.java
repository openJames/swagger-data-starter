package com.hh.swagger.dubbo.config;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

/**
 * desc:
 *
 * @author James
 * @since 2022-03-08 18:08
 */

public class EnableDubboSwaggerConfigCondition implements Condition {
    @Override
    public boolean matches(ConditionContext conditionContext, AnnotatedTypeMetadata annotatedTypeMetadata) {
        String value = conditionContext.getEnvironment().getProperty("swagger.enable");
        return "true".equalsIgnoreCase(value) || StringUtils.isEmpty(value);
    }
}
