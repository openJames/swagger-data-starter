package com.hh.swagger.dubbo.http;

import com.hh.swagger.dubbo.config.BeansUtil4SwaggerContext;
import com.hh.swagger.dubbo.util.AopTargetUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.SpringProxy;
import org.springframework.context.ApplicationContext;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ReferenceManager {

    private static Logger logger = LoggerFactory.getLogger(ReferenceManager.class);

    private static Map<Class<?>, Object> interfaceMapProxy = new ConcurrentHashMap<Class<?>, Object>();
    private static Map<Class<?>, Object> interfaceMapRef = new ConcurrentHashMap<Class<?>, Object>();
    private static ReferenceManager instance;
    private Set<String> serviceSize = new HashSet<>();

    private ReferenceManager() {
    }



    @SuppressWarnings({"rawtypes", "unchecked"})
    public synchronized static ReferenceManager getInstance() {
        if (null != instance) {
            return instance;
        }
        instance = new ReferenceManager();
        try {
            ApplicationContext context1 = BeansUtil4SwaggerContext.getApplicationContext();

            Class<?> aClass = Class.forName("com.hundsun.jrescloud.rpc.annotation.CloudService");
            Map<String, Object> beansWithAnnotation = context1.getBeansWithAnnotation((Class<? extends Annotation>) aClass);

            beansWithAnnotation.forEach((k, value) -> {
                // one value only find once
                boolean isFindOutImpl = false;

                Class<?> aClass1 = value.getClass();
                Class<?>[] interfaces = aClass1.getInterfaces();
                if (interfaces.length > 0) {
                    boolean isProxy = false;
                    Class<?> targetClass = null;
                    // normal impl or jdk/spring proxy
                    for (Class<?> anInterface : interfaces) {
                        if (anInterface.equals(Proxy.class) || anInterface.equals(SpringProxy.class)) {
                            isProxy = true;
                        }
                        if (anInterface.isAnnotationPresent((Class<? extends Annotation>) aClass)) {
                            targetClass = anInterface;
                            isFindOutImpl = true;
                        }
                    }
                    if (isFindOutImpl) {
                        if (isProxy) {
                            Object target = null;
                            try {
                                target = AopTargetUtils.getTarget(value);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            interfaceMapRef.putIfAbsent(targetClass, target);
                            interfaceMapProxy.put(targetClass, target);
                        } else {
                            interfaceMapRef.putIfAbsent(targetClass, value);
                            interfaceMapProxy.put(targetClass, value);
                        }
                    }
                }

                if (!isFindOutImpl) {
                    // cglib proxy
                    Class<?>[] interfaces1 = aClass1.getSuperclass().getInterfaces();
                    if (interfaces1.length > 0) {
                        for (Class<?> father : interfaces1) {
                            if (father.isAnnotationPresent((Class<? extends Annotation>) aClass)) {
                                Object value1 = null;
                                try {
                                    value1 = aClass1.getSuperclass().newInstance();
                                } catch (InstantiationException e) {
                                    e.printStackTrace();
                                } catch (IllegalAccessException e) {
                                    e.printStackTrace();
                                }
                                interfaceMapRef.putIfAbsent(father, value1);
                                interfaceMapProxy.put(father, value);
                            }
                        }
                    }
                }
            });

        } catch (Exception e) {
            logger.error("Get All Dubbo Service Error", e);
            return instance;
        }

        return instance;
    }

    public static Object getTarget(Object proxy) throws IllegalAccessException, NoSuchFieldException {
        Field field = proxy.getClass().getSuperclass().getDeclaredField("h");
        field.setAccessible(true);
        Object proxyObj = field.get(proxy);
        Field target = proxyObj.getClass().getDeclaredField("advised");
        target.setAccessible(true);
        return target.get(proxyObj);
    }


    public Object getProxy(String interfaceClass) {
        Set<Entry<Class<?>, Object>> entrySet = interfaceMapProxy.entrySet();
        for (Entry<Class<?>, Object> entry : entrySet) {
            if (entry.getKey().getName().equals(interfaceClass)) {
                return entry.getValue();
            }
        }
        return null;
    }

    public Entry<Class<?>, Object> getRef(String interfaceClass) {
        Set<Entry<Class<?>, Object>> entrySet = interfaceMapRef.entrySet();
        for (Entry<Class<?>, Object> entry : entrySet) {
            if (entry.getKey().getName().equals(interfaceClass)) {
                return entry;
            }
        }
        return null;
    }

    public Map<Class<?>, Object> getInterfaceMapRef() {
        return interfaceMapRef;
    }

    public void addAllService(List<String> tags) {
        serviceSize.addAll(tags);
    }

    public int getServiceSize() {
        return serviceSize.size();
    }
}
