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
//  Main.kt
//  FlowKit
//
//  Created by Brian Howard
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//

package com.inmotionsoftware.flowkit.compiler

import gnu.getopt.LongOpt;
import gnu.getopt.Getopt;
import java.io.*
import java.nio.file.Paths
import kotlin.system.exitProcess

val appName = "flowkit"

public enum class ExportFormat {
    SWIFT,
    KOTLIN,
    JAVA;

    fun parse(string: String): ExportFormat? =
        when (string) {
            "kotlin" -> KOTLIN
            "java" -> JAVA
            "swift" -> SWIFT
            else -> null
        }

    override fun toString(): String =
        when (this) {
            SWIFT -> "swift"
            KOTLIN -> "kotlin"
            JAVA -> "java"
        }

    val extension get() =
        when (this) {
            SWIFT -> "swift"
            KOTLIN -> "kt"
            JAVA -> "java"
        }
}

enum class Format {
    PUML
}

fun printErrLn(msg: String) {
    System.err.println(msg)
}

fun printErr(msg: String) {
    System.err.print(msg)
}

fun printHelp() {
    printErrLn("help")
    System.exit(1)
}

fun main(args: Array<String>) {

//    println("args: ")
//    args.forEach { println(it) }
//    println()

//    val file = Paths.get("/Users/bghoward/Projects/FlowKit/compiler/src/main/java/com/inmotionsoftware/flowkit/compiler/test.puml")
//    val file = Paths.get("/Users/bghoward/Projects/FlowKit/example/android/app/src/main/java/com/inmotionsoftware/example/flows/LoginFlow.puml")
//    val args = arrayOf<String>(
//        "--export", "kotlin",
//        "-i", "/Users/bghoward/Projects/FlowKit/example/android/app/build/generated/source/flowkit",
//        "--output", "./out.kt",
//        file.toAbsolutePath().toString()
//    )

    val longopts = arrayOf<LongOpt>(
        LongOpt("export", LongOpt.REQUIRED_ARGUMENT, null, 'e'.toInt()),
        LongOpt("format", LongOpt.REQUIRED_ARGUMENT, null, 'f'.toInt()),
        LongOpt("output", LongOpt.REQUIRED_ARGUMENT, null, 'o'.toInt()),
        LongOpt("image", LongOpt.REQUIRED_ARGUMENT, null, 'i'.toInt()),
        LongOpt("help", LongOpt.NO_ARGUMENT, null, 'h'.toInt())
    )

    var output: Writer? = null

    val getopt = Getopt(appName, args, "e:f:i:o:h", longopts)

    var format = ExportFormat.KOTLIN
    var imageDir: File? = null

    var outPath: String? = null

    while (true) {
        val c = getopt.getopt()
        if (c == -1) break
        when (c) {
            'f'.toInt() -> {
                if (getopt.optarg != "puml") {
                    printErrLn("invalid format")
                    System.exit(-1)
                }
            }
            'o'.toInt() -> {
                val path = getopt.optarg
                output = OutputStreamWriter(FileOutputStream(path))
                outPath = path
            }
            'h'.toInt() -> {
                printHelp()
                System.exit(0)
            }
            'i'.toInt() -> {
                imageDir = File(getopt.optarg)
            }
            'e'.toInt() -> {
                when (getopt.optarg) {
                    "swift" -> {
                        format =
                            ExportFormat.SWIFT
                    }
                    "kotlin" -> {
                        format =
                            ExportFormat.KOTLIN
                    }
                    "java" -> {
                        format =
                            ExportFormat.JAVA
                    }
                }
            }
            '?'.toInt() -> {
                printHelp()
            }
            else -> {
                printHelp()
            }
        }
    }

    val files = args.copyOfRange(getopt.optind, args.size)

    files.forEach {
        val file = File(it)
        try {

            val name = file.nameWithoutExtension
            val title = name.capitalize()

            val out: Writer = output ?: OutputStreamWriter(System.out)
            out.use {
                val list = processPuml(
                    namespace = defaultNameSpace,
                    inputFile = file,
                    imageDir = imageDir,
                    exportFormat = format,
                    writer = out
                )
                list.forEach { println(it) }
            }

        } catch (e: Throwable) {
//            e.printStackTrace()
            printErrLn("${file.absolutePath}: error: ${e.localizedMessage ?: e.toString()}")
            exitProcess(1)
        }
    }

    outPath?.let { println(it) }
}