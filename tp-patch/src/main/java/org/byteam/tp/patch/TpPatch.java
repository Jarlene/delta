package org.byteam.tp.patch;

/**
 * Bsdiff wrapper.
 *
 * @Author: chenenyu
 * @Created: 16/8/16 15:26.
 */
public class TpPatch {

    public static native int diff(String oldPath, String newPath, String patchPath);

    public static native int patch(String oldPath, String newPath, String patchPath);

}
