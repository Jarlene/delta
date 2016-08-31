package org.byteam.tp.patch.extension
/**
 * @Author: chenenyu
 * @Created: 16/8/18 18:06.
 */
class TpExtension {
    /**
     * 是否开启patch
     */
    boolean enable

    /**
     * 对应的版本号
     */
    int versionCode

    /**
     * 每个dex包含的方法数上限.
     */
    int maxNumberOfIdxPerDex = ExtConsts.DEFAULT_MAX_NUMBER_OF_IDX_PER_DEX

    /**
     * 生成patch后自动拷贝到assets目录下
     */
    boolean autoPushPatchToAssets
}