package com.inmotionsoftware.flowkit.gradle

import com.inmotionsoftware.flowkit.compiler.ExportFormat
import com.inmotionsoftware.flowkit.compiler.defaultNameSpace
import com.inmotionsoftware.flowkit.compiler.processPuml
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.io.FileWriter

open class FlowKitPluginExtension {
    var namespace: String? = null
    var export: String? = null
}

fun getGeneratedPath(project: Project, format: ExportFormat): File = File("${project.buildDir}/generated/flowkit/sources/${format}/main")

class FlowKitPlugin: Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("flowKit", FlowKitPluginExtension::class.java)

        // TODO: pick the format more intelligently
        val format = ExportFormat.valueOf(extension.export?.toUpperCase() ?: "KOTLIN")

        val task = project.tasks.create("compileFlow", FlowKitGeneratorTask::class.java)
        project.convention.findPlugin(JavaPluginConvention::class.java)
            ?.sourceSets
            ?.find { it.name == SourceSet.MAIN_SOURCE_SET_NAME }
            ?.let { it.java.srcDirs(getGeneratedPath(project, format) ) }
    }
}

open class FlowKitGeneratorTask : DefaultTask() {
    @TaskAction
    fun generate() {
        var extension = project.extensions.findByType(FlowKitPluginExtension::class.java)
            ?: FlowKitPluginExtension()

        val format = ExportFormat.valueOf(extension.export?.toUpperCase() ?: "KOTLIN")
        val outDir = getGeneratedPath( project, format )

        project.mkdir(outDir)
        val root = File("${project.projectDir}/src/${SourceSet.MAIN_SOURCE_SET_NAME}")

        root
            .walk()
            .filter { it.extension == "puml" }
            .forEach {
                val name = it.nameWithoutExtension
                val out = File(outDir, "${name}.${format.extension}")
                val writer = FileWriter(out)

                // generate our namespace package
                // 1) use the extension override first
                // 2) Use the folder name
                // 3) If all else fails use the default
                var namespace = extension.namespace
                if (extension.namespace == null) {
                    var path = it.relativeTo(root).parentFile.toPath()
                    path.firstOrNull()?.let {
                        if (it.toString().equals(format.name, ignoreCase=true)) {
                            path = path.subpath(1, path.nameCount)
                        }
                    }
                    val ns = path.joinToString(".")
                    if (ns.isNotEmpty()) namespace = ns
                }

                val ns = namespace ?: defaultNameSpace
                processPuml(
                    ns,
                    it,
                    format,
                    writer
                )
                writer.close()
            }
    }
}