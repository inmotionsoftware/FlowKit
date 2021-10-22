// MIT License
//
// Permission is hereby granted, free of charge, to any person obtaining a 
// copy of this software and associated documentation files (the "Software"), 
// to deal in the Software without restriction, including without limitation 
// the rights to use, copy, modify, merge, publish, distribute, sublicense, 
// and/or sell copies of the Software, and to permit persons to whom the 
// Software is furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER 
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING 
// FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
// IN THE SOFTWARE.
//
//  GradlePlugin.kt
//  FlowKit
//
//  Created by Brian Howard
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//

package com.inmotionsoftware.flowkit.gradle

import com.inmotionsoftware.flowkit.compiler.ExportFormat
import com.inmotionsoftware.flowkit.compiler.defaultNameSpace
import com.inmotionsoftware.flowkit.compiler.processPuml
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.specs.Spec
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.compile.JavaCompile
import java.io.File
import java.io.FileWriter
import com.android.build.gradle.BaseExtension

open class FlowKitPluginExtension {
    var namespace: String? = null
    var export: String? = null
    var generatedDir: File? = null
    var sourceDir: File? = null
}

fun FlowKitPluginExtension.getGeneratedPath(project: Project, format: ExportFormat): File {
    val dir = generatedDir
    return if (dir != null) {
        if (dir.isRooted) {
            dir
        } else {
            val root = project.buildDir
            File(root, dir.toString())
        }
    } else {
        File(project.buildDir, "/generated/source/flowkit")
    }
}

class FlowKitPlugin: Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("flowKit", FlowKitPluginExtension::class.java)

        // TODO: pick the format more intelligently
        val format = ExportFormat.valueOf(extension.export?.toUpperCase() ?: "KOTLIN")
        val outDir = extension.getGeneratedPath(project, format)

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

//        val sourceSet = project.fileTree(outDir)
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
        FileWriter(out).use {
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

            val list = processPuml(
                namespace = ns,
                inputFile = file,
                imageDir = null,
                exportFormat = format,
                writer = it
            )
            list.forEach { println(it) }
        }
        return out
    }

    @TaskAction
    fun generate() {
        val extension = project.extensions.findByType(FlowKitPluginExtension::class.java) ?: FlowKitPluginExtension()
        val format = ExportFormat.valueOf(extension.export?.toUpperCase() ?: "KOTLIN")

        val outDir = extension.getGeneratedPath( project, format )

        val file = extension.sourceDir ?: File("${project.projectDir}/src/${SourceSet.MAIN_SOURCE_SET_NAME}")
        val source = project.fileTree(file)
        val sourceSet = source.filter(Spec { it.extension == "puml" })
        val root = file
        val files = mutableSetOf<File>()
        sourceSet.forEach {
            val out = compile(root=root,file=it, namespace=extension.namespace, format=format, outDir=outDir)
            files.add(out)
            println(out)
        }
    }
}