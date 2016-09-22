package org.byteam.delta.extension

/**
 * @Author: chenenyu
 * @Created: 16/8/18 18:06.
 */
class DeltaExtension {
    /**
     * 是否开启patch
     */
    boolean enable

    /**
     * 对应的版本号
     */
    int versionCode

    /**
     * 用户指定mapping文件的路径
     */
    String mapping

    /**
     * 生成patch后自动拷贝到手机rom:/data/local/tmp/package/delta/
     */
    boolean autoPushPatchToDevice = true
}