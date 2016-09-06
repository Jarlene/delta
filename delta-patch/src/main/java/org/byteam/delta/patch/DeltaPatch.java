package org.byteam.delta.patch;

/**
 * bspatch wrapper.
 *
 * @Author: chenenyu
 * @Created: 16/8/16 15:26.
 */
public class DeltaPatch {

    static {
        System.loadLibrary("delta");
    }

    private DeltaPatch() {
    }

//    public static native int diff(String oldPath, String newPath, String patchPath);

    public static native int patch(String oldPath, String newPath, String patchPath);

}
