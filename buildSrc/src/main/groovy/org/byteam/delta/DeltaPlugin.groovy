package org.byteam.delta

import com.android.build.gradle.AppPlugin
import com.android.build.gradle.api.ApplicationVariant
import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.api.BaseVariantOutput
import com.android.build.gradle.internal.pipeline.TransformTask
import com.android.build.gradle.internal.transforms.ProGuardTransform
import com.android.builder.Version
import com.android.utils.XmlUtils
import org.apache.commons.io.Charsets
import org.apache.commons.io.FileUtils
import org.byteam.delta.bean.Patch
import org.byteam.delta.extension.DeltaExtension
import org.byteam.delta.extension.ExtConsts
import org.byteam.delta.task.BackupTask
import org.byteam.delta.task.PatchTask
import org.byteam.delta.task.PrePatchTask
import org.byteam.delta.task.TaskConsts
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

        dependencyDelta(project)

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
                // Compile generated source.
                variant.javaCompiler.source(new File("${project.buildDir}/generated/source/delta"))

                variant.outputs.each { BaseVariantOutput output ->
                    output.processManifest.doLast {
                        handleManifest(project, variant, output.processManifest.manifestOutputFile)
                    }
                }

                // Fetch version code.
                int versionCode = extension.versionCode > 0 ? extension.versionCode : variant.versionCode
                Patch patch = new Patch(project.projectDir.getAbsolutePath(), String.valueOf(versionCode),
                        variant.flavorName, variant.buildType.name)

                Task prePatchTask
                if (extension.mapping) { // If custom mapping exists, do not create pre-patch task.
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
                            task.description = "Clean project and apply mapping for ${variant.name}"
                            task.mPatch = patch
                            task.mVariant = variant
                            task.dependsOn cleanTask
                        }
                    } else {
                        println("Can not find task: clean")
                    }
                }

                if (variant.getAssemble()) {
                    project.task(type: BackupTask, overwrite: true,
                            TaskConsts.BACKUP.concat(variant.name.capitalize())) { BackupTask task ->
                        task.group = TaskConsts.GROUP
                        task.description = "Backup all required files for ${variant.name}"
                        task.mPatch = patch
                        task.mVariant = variant
                        task.dependsOn variant.getAssemble()
                    }

                    project.task(type: PatchTask, overwrite: true,
                            TaskConsts.PATCH.concat(variant.name.capitalize())) { PatchTask task ->
                        task.group = TaskConsts.GROUP
                        task.description = "Generates patch for ${variant.name}"
                        task.mPatch = patch
                        task.mVariant = variant
                        task.pushPatchToDevice = extension.autoPushPatchToDevice
                        task.dependsOn variant.getAssemble()
                        if (prePatchTask) {
                            task.dependsOn prePatchTask
                            variant.getAssemble().mustRunAfter prePatchTask
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
    private void dependencyDelta(Project project) {
        Project delta = project.rootProject.findProject("delta")
        if (delta) {
            project.dependencies {
                compile delta
            }
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

}