package com.hh.swagger.dubbo.web;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hh.swagger.dubbo.http.HttpMatch;
import com.hh.swagger.dubbo.http.ReferenceManager;
import com.hh.swagger.dubbo.reader.NameDiscover;
import io.swagger.util.Json;
import io.swagger.util.PrimitiveType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

@Controller
public class SwaggerInvokeController {

    private static Logger logger = LoggerFactory.getLogger(SwaggerInvokeController.class);

    public static final String ARRAY_FLAG = "/swagger-invokes";
    public static final String NON_ARRAY_FLAG = "/swagger-invoke";

    private boolean enable = true;

    @Value("${app.name}")
    private String appName;

    @Value("${app.group}")
    private String appGroup;

    @Value("${app.version}")
    private String appVersion;

    @RequestMapping(value = "simple")
    @ResponseBody
    public void invokeDubboSimple() throws Exception {
        System.out.println("appName = " + appName);
        System.out.println("appGroup = " + appGroup);
        System.out.println("appVersion = " + appVersion);
    }


    @PostMapping(value = NON_ARRAY_FLAG + "/{interfaceClass}/{methodName}", produces = "application/json; charset=utf-8")
    @ResponseBody
    public ResponseEntity<String> invokeDubboJson(@PathVariable("interfaceClass") String interfaceClass,
                                              @PathVariable("methodName") String methodName,
                                              @RequestBody(required = false) Map map,
                                              HttpServletRequest request,
                                              HttpServletResponse response) throws Exception {
        String arrayOrJson = null;
        if (!CollectionUtils.isEmpty(map)) {

//            arrayOrJson = JSON.json(map);
            ObjectMapper objectMapper = new ObjectMapper();
            arrayOrJson = objectMapper.writeValueAsString(map);
        }
        return invokeDubbo(interfaceClass, methodName, arrayOrJson, request, response);
    }

    @PostMapping(value = ARRAY_FLAG + "/{interfaceClass}/{methodName}", produces = "application/json; charset=utf-8")
    @ResponseBody
    public ResponseEntity<String> invokeDubboArray(@PathVariable("interfaceClass") String interfaceClass,
                                              @PathVariable("methodName") String methodName,
                                              @RequestBody(required = false) List<Object> objects,
                                              HttpServletRequest request,
                                              HttpServletResponse response) throws Exception {
        String arrayOrJson = null;
        if (objects != null) {
//            arrayOrJson = JSON.json(objects);
            ObjectMapper objectMapper = new ObjectMapper();
            arrayOrJson = objectMapper.writeValueAsString(objects);
        }
        return invokeDubbo(interfaceClass, methodName, arrayOrJson, request, response);
    }


    public ResponseEntity<String> invokeDubbo(String interfaceClass, String methodName, String arrayOrJson, HttpServletRequest request,
                                              HttpServletResponse response) throws Exception {
        Object ref;
        Method method = null;
        Object result;

        logger.info("arrayOrJson:{}, request:{}", arrayOrJson, request.getParameterMap());

        Entry<Class<?>, Object> entry = ReferenceManager.getInstance().getRef(interfaceClass);

        if (null == entry) {
            logger.info("No Ref Service FOUND.");
            return new ResponseEntity<String>(HttpStatus.NOT_FOUND);
        }

        ref = ReferenceManager.getInstance().getProxy(interfaceClass);
        HttpMatch httpMatch = new HttpMatch(entry.getKey(),  entry.getValue().getClass());
        Method[] interfaceMethods = httpMatch.findInterfaceMethods(methodName);

        if (null != interfaceMethods && interfaceMethods.length > 0) {
            Method[] refMethods = httpMatch.findRefMethods(interfaceMethods, null, request.getMethod());
            method = httpMatch.matchRefMethod(refMethods, methodName, request.getParameterMap().keySet());
        }
        if (null == method) {
            logger.info("No Service Method FOUND.");
            return new ResponseEntity<String>(HttpStatus.NOT_FOUND);
        }


        String[] parameterNames = NameDiscover.parameterNameDiscover.getParameterNames(method);
        if (parameterNames == null || parameterNames.length == 0) {
            logger.info("[swagger-dubbo] Invoke service with no parameter");
            try {
                result = method.invoke(ref, new Object[0]);
            } catch (Exception e) {
                String s = ((InvocationTargetException) e).getTargetException().toString();
                return ResponseEntity.ok(Json.mapper().writeValueAsString(s));
            }
        } else {
            Object[] args = new Object[parameterNames.length];
            Type[] parameterTypes = method.getGenericParameterTypes();
            Class<?>[] parameterClazz = method.getParameterTypes();

            for (int i = 0; i < parameterNames.length; i++) {
                String parameter = request.getParameter(parameterNames[i]);
                if (StringUtils.isEmpty(parameter)) {
                    parameter = arrayOrJson;
                }
                Object suggestPrameterValue = suggestPrameterValue(parameterTypes[i],
                        parameterClazz[i], parameter);
                args[i] = suggestPrameterValue;
            }
            logger.info("[swagger-dubbo] Invoke service with parameter: {}", Arrays.toString(args));
            try {
                result = method.invoke(ref, args);
            } catch (Exception e) {
                String s = ((InvocationTargetException) e).getTargetException().toString();
                return ResponseEntity.ok(Json.mapper().writeValueAsString(s));
            }
        }
        return ResponseEntity.ok(Json.mapper().writeValueAsString(result));
    }

    private Object suggestPrameterValue(Type type, Class<?> cls, String parameter) throws JsonParseException, JsonMappingException, IOException {

        PrimitiveType fromType = PrimitiveType.fromType(type);
        if (null != fromType) {
            DefaultConversionService service = new DefaultConversionService();
            boolean actual = service.canConvert(String.class, cls);
            if (actual) {
                return service.convert(parameter, cls);
            }
        } else {
            if (null == parameter) {
                return null;
            }
            try {
                JavaType javaType = new ObjectMapper().constructType(type);
                ObjectMapper mapper = Json.mapper();
                return mapper.readValue(parameter,  javaType);
            } catch (Exception e) {
                throw new IllegalArgumentException("The parameter value [" + parameter + "] should be json of [" + cls.getName() + "] Type.", e);
            }
        }
        try {
            return Class.forName(cls.getName()).newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
