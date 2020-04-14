package com.inmotionsoftware.flowkit.compiler

import gnu.getopt.LongOpt;
import gnu.getopt.Getopt;
import java.io.*
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

fun main(_args: Array<String>) {
//    println("args: ")
//    _args.forEach { println(it) }
//    println()

    val args = _args

//    val args = arrayOf<String>(
//        "--export", "swift",
//        "--output", "/Users/bghoward/Library/Developer/Xcode/DerivedData/ExampleFlow-eaanvgxflaclurgpuxwawntdfgtl/Build/Intermediates.noindex/ExampleFlow.build/Debug-iphonesimulator/ExampleFlow.build/DerivedSources/LoginFlow.swift",
//        "/Users/bghoward/Projects/FlowKit/example/ios/ExampleFlow/Flows/LoginFlow.puml"
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
                output = OutputStreamWriter(FileOutputStream(getopt.optarg))
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
            processPuml(
                namespace = defaultNameSpace,
                file = file,
                imageDir = imageDir,
                exportFormat = format,
                writer = out
            )
            out.close()
        } catch (e: Throwable) {
            e.printStackTrace()
            printErrLn("${file.absolutePath}: error: ${e.localizedMessage ?: e.toString()}")
            exitProcess(1)
        }
    }
}