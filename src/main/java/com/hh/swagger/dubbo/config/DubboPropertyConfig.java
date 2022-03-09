package com.hh.swagger.dubbo.config;

import com.hh.swagger.dubbo.http.ReferenceManager;
import io.swagger.config.SwaggerConfig;
import io.swagger.models.Info;
import io.swagger.models.Path;
import io.swagger.models.Scheme;
import io.swagger.models.Swagger;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.servlet.ServletContext;
import java.text.MessageFormat;
import java.util.Map;

@Component
public class DubboPropertyConfig implements SwaggerConfig {

    @Value("${app.name}")
    private String appName;

    @Value("${app.group}")
    private String appGroup;

    @Value("${app.version}")
    private String appVersion;

    @Autowired
    private ServletContext servletContext;

    private static String desc = "appGroup: {0}<br/>" + "appVersion: {1}<br/>" + "total: ";

    @Override
    public Swagger configure(Swagger swagger) {
        // 只是为了使用 swagger3 默认访问的是 https
//        swagger.addScheme(Scheme.WS);

        Info info = swagger.getInfo();

        if (info == null) {
            info = new Info();
            swagger.setInfo(info);

            Map<String, Path> paths = swagger.getPaths();

            int serviceSize = 0;
            int methodCount = 0;

            if (!CollectionUtils.isEmpty(paths)) {
                methodCount = paths.size();
                serviceSize = ReferenceManager.getInstance().getServiceSize();
            }
            String descAccordingSize = null;
            if (serviceSize > 1) {
                descAccordingSize = desc + "{2} services, ";
            } else {
                descAccordingSize = desc + "{2} service, ";
            }

            if (methodCount > 1) {
                descAccordingSize = descAccordingSize + "{3} methods";
            } else {
                descAccordingSize = descAccordingSize + "{3} method";
            }

            info.setDescription(MessageFormat.format(descAccordingSize, appGroup, appVersion, serviceSize, methodCount));
        }

        if (StringUtils.isNotBlank(appName)
                && StringUtils.isNotBlank(appGroup)
                && StringUtils.isNotBlank(appVersion)) {
            info.setTitle("app name: " + appName);
        }


        setBashPath(swagger);
        return swagger;
    }

    private void setBashPath(Swagger swagger) {
        if (StringUtils.isEmpty(swagger.getBasePath())) {
            swagger.setBasePath(StringUtils.isEmpty(servletContext.getContextPath()) ? "/" : servletContext.getContextPath());
        }
    }

    @Override
    public String getFilterClass() {
        return null;
    }

}
