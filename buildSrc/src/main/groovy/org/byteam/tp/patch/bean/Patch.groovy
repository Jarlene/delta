package org.byteam.tp.patch.bean

import java.text.SimpleDateFormat

/**
 * @Author: chenenyu
 * @Created: 16/8/17 11:25.
 */
class Patch {
    static final String ROOT_NAME = "patch";
    static final String PATCH_FILE_NAME = "patch";
    static final String MAPPING_FILE_NAME = "mapping.txt";
    static final String MAIN_DEX_LIST_FILE_NAME = "maindexlist.txt";
    static final String ORIGINAL_FILE_NAME = "original";

    String projectDir; // project根目录
    String version; // 版本号
    String flavorName; // flavor
    String buildTypeName; // buildType (debug or release ...)


    Patch(String projectDir, String version, String flavorName, String buildTypeName) {
        this.projectDir = projectDir
        this.version = version
        this.flavorName = flavorName
        this.buildTypeName = buildTypeName
    }

    /**
     * @return app/patch
     */
    private String getRootPath() {
        return projectDir + File.separator + ROOT_NAME;
    }

    /**
     * @return version_1
     */
    private String getVersionDirName() {
        return "version_" + version;
    }

    /**
     * @return app/patch/version_1/flavor/debug{release}
     */
    private String getVariantPath() {
        return getRootPath() + File.separator + getVersionDirName() + File.separator +
                (flavorName ? flavorName : "default") + File.separator + buildTypeName;
    }

    /**
     * @return app/patch/version_1/flavor/debug{release}/original
     */
    public String getOriginalPath() {
        return getVariantPath() + File.separator + ORIGINAL_FILE_NAME;
    }

    /**
     * @return app/patch/version_1/flavor/debug{release}/original/dex
     */
    public String getOriginalDexPath() {
        return getOriginalPath() + File.separator + 'dex';
    }

    /**
     * @return app/patch/version_1/flavor/debug{release}/patch+时间戳
     */
    private String getPatchPath() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss")
        return getVariantPath() + File.separator + PATCH_FILE_NAME + "_" + sdf.format(new Date());
    }

    public File getPatchDir() {
        return new File(getPatchPath());
    }

    public File getMappingFile() {
        File file = new File(getOriginalPath() + File.separator + MAPPING_FILE_NAME);
        return file;
    }

    public File getMainDexListFile() {
        File file = new File(getMainDexListPath());
        return file;
    }

    public String getMainDexListPath() {
        return getOriginalPath() + File.separator + MAIN_DEX_LIST_FILE_NAME;
    }

}