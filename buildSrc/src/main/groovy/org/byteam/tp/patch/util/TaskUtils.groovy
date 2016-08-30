package org.byteam.tp.patch.util

import com.android.build.gradle.api.BaseVariant

/**
 * @Author: chenenyu
 * @Created: 16/8/29 19:03.
 */
class TaskUtils {

    static final String getAssemble(BaseVariant variant) {
        "assemble${variant.name.capitalize()}"
    }

    static final String getTransformDex(BaseVariant variant) {
        "transformClassesWithDexFor${variant.name.capitalize()}"
    }

    static final String getTransformProguard(BaseVariant variant) {
        "transformClassesAndResourcesWithProguardFor${variant.name.capitalize()}"
    }

}