package com.hh.swagger.dubbo.util;
import java.lang.reflect.Array;
import java.util.*;

/**
 * desc:
 *

 输出加收集


 *
 * @author James
 * @since 2022-11-07 20:14
 */
public class MapCamelUtil {

    public static Object key2Camel(Object v) {
        if (v instanceof Map) {
            return mapDeal(v);
        }else if (v instanceof List || v instanceof Array){
            return listOrArrayDeal(v);
        }
        return v;
    }


    // 判断可以优化 用等于数组或者 list来优化
    private static List<Object> listOrArrayDeal(Object v) {
        ArrayList<Object> objects = new ArrayList<>();
        HashSet keySet = new HashSet();
        List valueAll = new ArrayList();
        ((List<?>) v).forEach(
                v1 ->{
                    Object e = key2Camel(v1);
                    if (e instanceof Map) {
                        Set<?> keySet1 = ((Map<?, ?>) e).keySet();
                        Collection<?> values = ((Map<?, ?>) e).values();
                        keySet.addAll(keySet1);
                        valueAll.addAll(values);
                    }
                    objects.add(e);
                }
        );
        if (keySet.size() == 1) {
            return valueAll;
        }
        return objects;
    }

    // 最终都是来到这里
    private static Map mapDeal(Object v) {
        HashMap hashMap = new HashMap();
        ((Map) v).forEach((k1,v1) ->{
            hashMap.put(toCamelCase((CharSequence) k1,'_'), key2Camel(v1));
        });
        return hashMap;
    }

    private static String toCamelCase(CharSequence name, char symbol) {
        if (null == name) {
            return null;
        }

        final String name2 = name.toString();
        if (name2.contains(String.valueOf(symbol))) {
            final int length = name2.length();
            final StringBuilder sb = new StringBuilder(length);
            boolean upperCase = false;
            for (int i = 0; i < length; i++) {
                char c = name2.charAt(i);

                if (c == symbol) {
                    upperCase = true;
                } else if (upperCase) {
                    sb.append(Character.toUpperCase(c));
                    upperCase = false;
                } else {
                    sb.append(Character.toLowerCase(c));
                }
            }
            return sb.toString();
        } else {
            return name2;
        }
    }
}
