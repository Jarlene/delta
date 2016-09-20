package org.byteam.delta

import com.android.build.gradle.AppPlugin
import com.android.build.gradle.api.ApplicationVariant
import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.api.BaseVariantOutput
import com.android.build.gradle.internal.pipeline.TransformTask
import com.android.build.gradle.internal.transforms.DexTransform
import com.android.build.gradle.internal.transforms.ProGuardTransform
import com.android.builder.Version
import com.android.utils.XmlUtils
import com.google.common.base.Joiner
import org.apache.commons.io.Charsets
import org.apache.commons.io.FileUtils
import org.byteam.delta.bean.Patch
import org.byteam.delta.extension.DeltaExtension
import org.byteam.delta.extension.ExtConsts
import org.byteam.delta.task.BackupTask
import org.byteam.delta.task.PatchTask
import org.byteam.delta.task.PrePatchTask
import org.byteam.delta.task.TaskConsts
import org.byteam.delta.util.ReflectionUtils
import org.byteam.delta.util.TaskUtils
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.tooling.BuildException
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList

import javax.xml.transform.Transformer
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

import static com.android.SdkConstants.*
/**
 * @Author: chenenyu
 * @Created: 16/8/17 11:03.
 */
class DeltaPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {

        def extension = project.extensions.create(ExtConsts.EXTENSION_NAME, DeltaExtension)

        dependencyDelta(project, extension)

        def isAndroidApp = project.plugins.hasPlugin(AppPlugin)
        if (!isAndroidApp) {
            println("'com.android.application' plugin required.")
            return
        }

        project.afterEvaluate {

            if (!extension.enable) {
                return
            }

            project.android.applicationVariants.all { ApplicationVariant variant ->

                variant.outputs.each { BaseVariantOutput output ->
                    output.processManifest.doLast {
                        handleManifest(project, variant, output.processManifest.manifestOutputFile)
                    }
                }

                // Fetch version code.
                int versionCode = extension.versionCode > 0 ? extension.versionCode : variant.versionCode
                Patch patch = new Patch(project.projectDir.getAbsolutePath(), String.valueOf(versionCode),
                        variant.flavorName, variant.buildType.name)

                // dex task
                String transformClassesWithDexForVariant = TaskUtils.getTransformDex(variant)
                def dexTask = project.tasks.findByName(transformClassesWithDexForVariant) as TransformTask
                if (dexTask) {
                    setMaxNumberOfIdxPerDex(dexTask.transform as DexTransform, extension)
                } else {
                    println("Task:${transformClassesWithDexForVariant} not found.")
                }

                Task prePatchTask
                if (extension.mapping) { // If custom mapping exists.
                    File customMappingFile = new File(extension.mapping)
                    if (customMappingFile.exists() && customMappingFile.isFile()) {
                        String transformClassesAndResourcesWithProguardForVariant =
                                TaskUtils.getTransformProguard(variant)
                        def proguardTask = project.tasks.findByName(
                                transformClassesAndResourcesWithProguardForVariant) as TransformTask
                        if (proguardTask) {
                            def proguard = proguardTask.transform as ProGuardTransform
                            proguard.applyTestedMapping(customMappingFile)
                        } else {
                            println("Task: ${transformClassesAndResourcesWithProguardForVariant} not found.")
                        }
                    }
                } else { // Add pre-patch task to apply previous mapping.
                    Task cleanTask = project.tasks.findByName("clean")
                    if (cleanTask) {
                        prePatchTask = project.task(type: PrePatchTask, overwrite: true,
                                TaskConsts.PRE_PATCH.concat(variant.name.capitalize())) { PrePatchTask task ->
                            task.group = TaskConsts.GROUP
                            task.description = "Clean project and apply mapping for ${variant.name}"
                            task.mPatch = patch
                            task.mVariant = variant
                            task.dependsOn cleanTask
                        }
                    } else {
                        println("Can not find task: clean")
                    }
                }

                String assembleVariant = TaskUtils.getAssemble(variant)
                Task assembleTask = project.tasks.findByName(assembleVariant)
                if (assembleTask) {
                    project.task(type: BackupTask, overwrite: true,
                            TaskConsts.BACKUP.concat(variant.name.capitalize())) { BackupTask task ->
                        task.group = TaskConsts.GROUP
                        task.description = "Backup all required files for ${variant.name}"
                        task.mPatch = patch
                        task.mVariant = variant
                        task.dependsOn assembleTask
                    }

                    project.task(type: PatchTask, overwrite: true,
                            TaskConsts.PATCH.concat(variant.name.capitalize())) { PatchTask task ->
                        task.group = TaskConsts.GROUP
                        task.description = "Generates patchs for ${variant.name}"
                        task.mPatch = patch
                        task.mVariant = variant
                        task.pushPatchToDevice = extension.autoPushPatchToDevice
                        task.dependsOn assembleTask
                        if (prePatchTask) {
                            task.dependsOn prePatchTask
                            assembleTask.mustRunAfter prePatchTask
                        }
                    }
                } else {
                    println("Can not find task: ${assembleVariant}")
                }

            }
        }
    }

    private void handleManifest(Project project, BaseVariant variant, File manifestFile) {
        if (manifestFile.exists()) {
            try {
                Document document = XmlUtils.parseUtfXmlFile(manifestFile, true);
                Element root = document.getDocumentElement();
                if (root != null) {
                    String applicationId = root.getAttribute(ATTR_PACKAGE);
                    String applicationClass = null;
                    NodeList children = root.getChildNodes();
                    for (int i = 0; i < children.getLength(); i++) {
                        Node node = children.item(i);
                        if (node.getNodeType() == Node.ELEMENT_NODE &&
                                node.getNodeName().equals(TAG_APPLICATION)) {
                            Element application = (Element) node;
                            String applicationName = ANDROID_NS_NAME_PREFIX + ATTR_NAME;
                            if (application.hasAttribute(applicationName)) {
                                String name = application.getAttribute(applicationName);
                                assert !name.startsWith("."): name;
                                if (!name.isEmpty()) {
                                    applicationClass = name;
                                }
                                // Inject the bootstrapping application
                                application.setAttribute(applicationName, "org.byteam.delta.BootstrapApplication");
                                // Save AndroidManifest.xml
                                Transformer transformer = TransformerFactory.newInstance().newTransformer();
                                DOMSource source = new DOMSource(document);
                                StreamResult result = new StreamResult(manifestFile);
                                transformer.transform(source, result);
                            }
                            break
                        }
                    }
                    if (applicationId && applicationClass) {
                        writeAppInfoClass(project, variant, applicationId, applicationClass)
                    } else {
                        throw new NullPointerException("Null applicationId or applicationClass")
                    }
                }
            } catch (Exception e) {
                throw new BuildException("Failed to inject bootstrapping application", e)
            }
        }
    }

    private void writeAppInfoClass(Project project, BaseVariant variant, String applicationId, String applicationClass) {
        // Add generated code to compile source.
        variant.javaCompiler.source(new File("${project.buildDir}/generated/source/delta"))

        // Generate AppInfo.java
        File appInfoFile = project.file("${project.buildDir.absolutePath}/generated/source/delta/org/byteam/delta/AppInfo.java")
        def template = new AppInfoTemplate()
        template.applicationId = applicationId
        template.applicationClass = applicationClass
        FileUtils.writeStringToFile(appInfoFile, template.getTemplate(), Charsets.UTF_8)
    }

    /**
     * Compile delta lib.
     */
    private void dependencyDelta(Project project, DeltaExtension extension) {
        Project delta = project.rootProject.findProject("delta")
        if (delta) {
            project.dependencies {
                compile delta
            }
        } else {
            if (extension.deltaVersion) {
                project.dependencies.add('compile', "org.byteam.delta:delta:${extension.deltaVersion}")
            } else {
                Configuration configuration = project.rootProject.buildscript.configurations
                        .getByName('classpath')
                configuration.allDependencies.all { Dependency dependency ->
                    if (dependency.group == "org.byteam.delta") {
                        project.dependencies.add('compile', "org.byteam.delta:delta:${dependency.version}")
                    }
                }
            }
        }
    }

    /**
     * 指定每个dex的最大方法数。
     */
    private void setMaxNumberOfIdxPerDex(DexTransform dexTransform, DeltaExtension extension) {
        if (isMajor2Minor2OrAboveVersion()) { // 2.2.x or above
            // DefaultDexOptions
            def dexOptions = ReflectionUtils.getField(dexTransform, dexTransform.class, "dexOptions")
            List<String> additionalParameters = dexOptions.additionalParameters
            if (additionalParameters == null) {
                additionalParameters = []
            }
            String arg = '--set-max-idx-number='
            if (extension.maxNumberOfIdxPerDex > 0 && extension.maxNumberOfIdxPerDex < 0xFFFF) {
                arg = arg.concat("${extension.maxNumberOfIdxPerDex}")
            } else {
                arg = arg.concat(String.valueOf(ExtConsts.DEFAULT_MAX_NUMBER_OF_IDX_PER_DEX))
            }
            additionalParameters << arg
            dexOptions.additionalParameters = additionalParameters
            ReflectionUtils.setField(dexTransform, dexTransform.class, "dexOptions", dexOptions)
        }
    }

    /**
     * @return True if AndroidGradlePluginVersion >= 2.2.x.
     */
    static boolean isMajor2Minor2OrAboveVersion() {
        String[] version = Version.ANDROID_GRADLE_PLUGIN_VERSION.split("\\.")
        if (version.length > 2) {
            if (Integer.valueOf(version[0]) > 1 && Integer.valueOf(version[1]) > 1) {
                true
            }
        }
        false
    }

    /**
     * 获取生成dex的目录。
     */
    static File getDexFolder(Project project, Patch patch) {
        List<String> dir = []
        dir.add(project.buildDir.absolutePath)
        dir.add('intermediates')
        dir.add('transforms')
        dir.add('dex')
        if (patch.flavorName)
            dir.add(patch.flavorName)
        dir.add(patch.buildTypeName)
        dir.add('folders')
        dir.add('1000')
        dir.add('1f')
        dir.add('main')

        return new File(Joiner.on(File.separator).join(dir))
    }

}