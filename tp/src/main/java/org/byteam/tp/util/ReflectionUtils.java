package org.byteam.tp.util;

import java.lang.reflect.Field;

/**
 * 反射工具类
 *
 * @Author: chenenyu
 * @Created: 16/8/15 16:46.
 */
public class ReflectionUtils {

    public static Object getField(Object obj, Class<?> cl, String field)
            throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        Field localField = cl.getDeclaredField(field);
        if (!localField.isAccessible()) {
            localField.setAccessible(true);
        }
        return localField.get(obj);
    }

    public static void setField(Object obj, Class<?> cl, String field, Object value)
            throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        Field localField = cl.getDeclaredField(field);
        localField.setAccessible(true);
        localField.set(obj, value);
    }
}
