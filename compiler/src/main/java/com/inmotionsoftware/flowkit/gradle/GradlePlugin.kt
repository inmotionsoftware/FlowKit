package com.inmotionsoftware.flowkit.gradle

import com.inmotionsoftware.flowkit.compiler.ExportFormat
import com.inmotionsoftware.flowkit.compiler.defaultNameSpace
import com.inmotionsoftware.flowkit.compiler.processPuml
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.FileTree
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.internal.file.collections.PatternFilterableFileTree
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.specs.Spec
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.util.PatternFilterable
import org.gradle.tooling.model.SourceDirectory
import org.jetbrains.kotlin.gradle.plugin.android.AndroidGradleWrapper
import java.io.File
import java.io.FileWriter
import com.android.build.gradle.BaseExtension

open class FlowKitPluginExtension {
    var namespace: String? = null
    var export: String? = null
}

fun getGeneratedPath(project: Project, format: ExportFormat): File = File("${project.buildDir}/generated/source/flowkit")

class FlowKitPlugin: Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("flowKit", FlowKitPluginExtension::class.java)

        // TODO: pick the format more intelligently
        val format = ExportFormat.valueOf(extension.export?.toUpperCase() ?: "KOTLIN")
        val outDir = getGeneratedPath(project, format)

        // android
        (project.extensions.findByName("android") as? BaseExtension)
            ?.sourceSets
            ?.filter { it.name == SourceSet.MAIN_SOURCE_SET_NAME }
            ?.forEach { it.java.srcDirs(outDir) }

        // non android
        project.convention.findPlugin(JavaPluginConvention::class.java)
            ?.sourceSets
            ?.filter{ it.name == SourceSet.MAIN_SOURCE_SET_NAME}
            ?.forEach { it.java.srcDirs(outDir) }

        val sourceSet = project.fileTree(outDir)
        val task = project.tasks.create("compileFlow", FlowKitGeneratorTask::class.java)
//        project.tasks.findByName("build")?.dependsOn(task)

        val compileKotlinTasks = project.tasks.withType(KotlinCompile::class.java)
        compileKotlinTasks.configureEach { it.dependsOn(task) }

        val compileJavaTasks = project.tasks.withType(JavaCompile::class.java)
        compileJavaTasks.configureEach { it.dependsOn(task) }
    }
}

open class FlowKitGeneratorTask : DefaultTask() {
    private fun compile(root: File, file: File, namespace: String?, format: ExportFormat, outDir: File): File {

        val name = file.nameWithoutExtension
        if (!outDir.exists()) project.mkdir(outDir)

        val out = File(outDir, "${name}.${format.extension}")

//        println("compiling: ${file.absolutePath} to ${out}")
        val writer = FileWriter(out)

        var ns = namespace ?: defaultNameSpace

        // generate our namespace package
        // 1) use the extension override first
        // 2) Use the folder name
        // 3) If all else fails use the default
        if (namespace == null) {
            var path = file.relativeTo(root).parentFile.toPath()
            path.firstOrNull()?.let {
                val str = it.toString()
                if (str == "kotlin" || str == "java") {
                    path = path.subpath(1, path.nameCount)
                }
            }
            val str = path.joinToString(separator=".")
            if (str.isNotEmpty()) ns = str
        }

        processPuml(
            namespace = ns,
            file = file,
            imageDir = outDir,
            exportFormat = format,
            writer = writer
        )
        writer.close()

        return out
    }

    @TaskAction
    fun generate() {
        val extension = project.extensions.findByType(FlowKitPluginExtension::class.java) ?: FlowKitPluginExtension()
        val format = ExportFormat.valueOf(extension.export?.toUpperCase() ?: "KOTLIN")

        val outDir = getGeneratedPath( project, format )
        val root = File("${project.projectDir}/src/${SourceSet.MAIN_SOURCE_SET_NAME}")
        val sourceSet = project.fileTree(root).filter(Spec { it.extension == "puml" })

        val files = mutableSetOf<File>()
//        println("source set from ${root}")
        sourceSet.forEach {
            val out = compile(root=root,file=it, namespace=extension.namespace, format=format, outDir=outDir)
            files.add(out)
        }

//        if (!sourceSet.isEmpty) {
//            val compileKotlinTasks = project.tasks.withType(KotlinCompile::class.java)
//            compileKotlinTasks.configureEach {
//                val name = it.name
//                val compile = it
////                val collection = project.files(files.toTypedArray())
////                compile.source = compile.source.plus(collection).asFileTree
//                println("Task: ${name}")
////                it.classpath = it.classpath.plus(collection)
//                compile.source.files.forEach { println("compile kotlin: ${it}") }
//            }
//        }
    }
}