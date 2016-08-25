package org.byteam.tp.util;

import java.lang.reflect.Array;

import dalvik.system.DexClassLoader;
import dalvik.system.PathClassLoader;

/**
 * Referred from <a href="https://github.com/jasonross/Nuwa/blob/master/nuwa/src/main/java/cn/jiajixin/nuwa/util/DexUtils.java">Nuwa</a>.
 *
 * @Author: chenenyu
 * @Created: 16/8/16 16:06.
 */
public class DexUtils {

    public static void injectDexAtFirst(String dexPath, String defaultDexOptPath) throws NoSuchFieldException, IllegalAccessException, ClassNotFoundException {
        DexClassLoader dexClassLoader = new DexClassLoader(dexPath, defaultDexOptPath, dexPath, getPathClassLoader());
        Object oldDexElements = getDexElements(getPathList(getPathClassLoader()));
        Object newDexElements = getDexElements(getPathList(dexClassLoader));
        Object allDexElements = combineDexes(newDexElements, oldDexElements);
        Object pathList = getPathList(getPathClassLoader());
        ReflectionUtils.setField(pathList, pathList.getClass(), "dexElements", allDexElements);
    }

    /**
     * @return Current system class loader.
     */
    private static PathClassLoader getPathClassLoader() {
        PathClassLoader pathClassLoader = (PathClassLoader) DexUtils.class.getClassLoader();
        return pathClassLoader;
    }

    /**
     * @param baseDexClassLoader {@link dalvik.system.BaseDexClassLoader}
     * @return <strong>pathList</strong> in class loader.
     * @throws IllegalArgumentException
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     * @throws ClassNotFoundException
     */
    private static Object getPathList(Object baseDexClassLoader)
            throws IllegalArgumentException, NoSuchFieldException, IllegalAccessException, ClassNotFoundException {
        return ReflectionUtils.getField(baseDexClassLoader, Class.forName("dalvik.system.BaseDexClassLoader"), "pathList");
    }

    /**
     * @param pathList DexPathList
     * @return <strong>dexElements</strong> in DexPathList
     * @throws IllegalArgumentException
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    private static Object getDexElements(Object pathList)
            throws IllegalArgumentException, NoSuchFieldException, IllegalAccessException {
        return ReflectionUtils.getField(pathList, pathList.getClass(), "dexElements");
    }

    /**
     * Combines patch dex with original dex(s).
     *
     * @param newDexes patch dex
     * @param oldDexes original dex(s)
     * @return new dexs
     */
    private static Object combineDexes(Object newDexes, Object oldDexes) {
        Class<?> localClass = newDexes.getClass().getComponentType();
        int newDexesLength = Array.getLength(newDexes);
        int allLength = newDexesLength + Array.getLength(oldDexes);
        Object result = Array.newInstance(localClass, allLength);
        for (int i = 0; i < allLength; ++i) {
            if (i < newDexesLength) {
                Array.set(result, i, Array.get(newDexes, i));
            } else {
                Array.set(result, i, Array.get(oldDexes, i - newDexesLength));
            }
        }
        return result;
    }

}
