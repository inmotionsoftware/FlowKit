@file:JvmName("Main")
package com.inmotionsoftware.flowkit.compiler

import gnu.getopt.LongOpt;
import gnu.getopt.Getopt;
import java.io.*

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
    val args = arrayOf("--export", "kotlin", "../test/src/main/example.puml")

    print("args: ")
    args.forEach { println(it) }
    println()

    val longopts = arrayOf<LongOpt>(
        LongOpt("export", LongOpt.REQUIRED_ARGUMENT, null, 'e'.toInt()),
        LongOpt("format", LongOpt.REQUIRED_ARGUMENT, null, 'f'.toInt()),
        LongOpt("output", LongOpt.REQUIRED_ARGUMENT, null, 'o'.toInt()),
        LongOpt("help", LongOpt.NO_ARGUMENT, null, 'h'.toInt())
    )

    var output: Writer? = null

    val getopt = Getopt(appName, args, "e:f:i:o:h", longopts)

    var format =
        ExportFormat.KOTLIN

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
        val name = file.nameWithoutExtension
        val title = name.capitalize()
        val out: Writer = output ?: OutputStreamWriter(System.out)
        processPuml(
            namespace = defaultNameSpace,
            file = file,
            exportFormat = format,
            writer = out
        )
        out.close()
    }
}