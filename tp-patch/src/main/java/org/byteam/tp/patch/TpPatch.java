package org.byteam.tp.patch;

/**
 * @Author: chenenyu
 * @Created: 16/8/16 15:26.
 */
public class TpPatch {

    public static native int patch(String oldPath, String newPath, String patchPath);

}
