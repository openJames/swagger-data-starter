package com.hh.swagger.dubbo.web;

import com.hh.swagger.dubbo.config.DubboPropertyConfig;
import com.hh.swagger.dubbo.config.DubboServiceScanner;
import com.hh.swagger.dubbo.config.SwaggerDocCache;
import com.hh.swagger.dubbo.json.Json;
import com.hh.swagger.dubbo.reader.Reader;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.annotations.Api;
import io.swagger.config.SwaggerConfig;
import io.swagger.models.Swagger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

import static com.hh.swagger.dubbo.web.SwaggerInvokeController.NON_ARRAY_FLAG;

@Controller
@RequestMapping("swagger-service")
@CrossOrigin
public class SwaggerApiController {

    public static final String DEFAULT_URL = "/api-docs";
    private static final String HAL_MEDIA_TYPE = "application/hal+json";

    @Autowired
    private DubboServiceScanner dubboServiceScanner;

    @Autowired
    private DubboPropertyConfig dubboPropertyConfig;

    @Autowired
    private SwaggerDocCache swaggerDocCache;

    private String httpContext = NON_ARRAY_FLAG;

    private boolean enable = true;

    @RequestMapping(value = DEFAULT_URL,
            method = RequestMethod.GET,
            produces = {"application/json; charset=utf-8", HAL_MEDIA_TYPE})
    @ResponseBody
    public ResponseEntity<Json> getApiList() throws JsonProcessingException {

        if (!enable) {
            return new ResponseEntity<Json>(HttpStatus.NOT_FOUND);
        }

//        Swagger swagger = new Swagger();

        Swagger swagger = swaggerDocCache.getSwagger();
        if (null != swagger) {
            return new ResponseEntity<Json>(new Json(io.swagger.util.Json.mapper().writeValueAsString(swagger)), HttpStatus.OK);
        } else {
            swagger = new Swagger();
        }

        final SwaggerConfig configurator = dubboPropertyConfig;


        Map<Class<?>, Object> interfaceMapRef = dubboServiceScanner.interfaceMapRef();
        if (null != interfaceMapRef) {
            Reader.read(swagger, interfaceMapRef, httpContext);
        }

        if (configurator != null) {
            configurator.configure(swagger);
        }
        swaggerDocCache.setSwagger(swagger);
        return new ResponseEntity<Json>(new Json(io.swagger.util.Json.mapper().writeValueAsString(swagger)), HttpStatus.OK);
    }

}
