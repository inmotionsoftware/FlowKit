package com.inmotionsoftware.flowkit.compiler
import net.sourceforge.plantuml.FileFormat
import net.sourceforge.plantuml.FileFormatOption
import net.sourceforge.plantuml.ISourceFileReader
import net.sourceforge.plantuml.SourceFileReader
import net.sourceforge.plantuml.cucadiagram.GroupType
import net.sourceforge.plantuml.cucadiagram.LeafType
import net.sourceforge.plantuml.preproc.Defines
import net.sourceforge.plantuml.statediagram.StateDiagram
import java.io.*
import java.lang.RuntimeException

val defaultNameSpace = "com.inmotionsoftware.flowkit.generated"

enum class Export {
    SWIFT,
    KOTLIN,
    JAVA
}

data class State(val name: String, var type: String?)
data class Transition(val from: State, val to: State, val type: String?)

class StateMachineGenerator(val title: String, val states: Collection<State>, val transitions: Collection<Transition>) {
    val stateMachineName = "${title}StateMachine"
    val stateName = "${title}State"
    var namespace: String = "com.inmotionsoftware.flowkit"
}

class Enumeration(val name: String) {
    class Value(val name: String, val context: String? = null)
    var generics = mutableListOf<String>()
    var values = mutableListOf<Value>()
    var aliasess = mutableListOf<Pair<String,String>>()
    var nested: Collection<Enumeration> = listOf<Enumeration>()
}

class Handler(val state: String, val name: String, val enum: Enumeration)

fun convertName(name: String)
        = when (name) {
    "*start" -> "Begin"
    "*end" -> "End"
    "Begin" -> throw RuntimeException("Begin is a reserved state")
    "End" -> throw RuntimeException("End is a reserved state")
    else -> name
}


fun processPuml(namespace: String, file: File, imageDir: File?, exportFormat: ExportFormat, writer: Writer) {

    val states = mutableMapOf<String, State>()
    val transitions = mutableListOf<Transition>()

    var title: String = file.nameWithoutExtension

    val outdir = imageDir ?: file.parentFile
    outdir.mkdirs()
    val option = FileFormatOption(if (imageDir != null) FileFormat.PNG else FileFormat.XMI_STANDARD )

    val reader: ISourceFileReader = SourceFileReader(file, outdir, option)
    reader.setCheckMetadata(false)
    reader.blocks.forEach {
        it.diagram.warningOrError?.let {
            printErrLn("${file.absolutePath}: warning: ${it}")
        }
        val diagram = it.diagram as StateDiagram
        // override the title
        if (diagram.title != null) {
            val t = diagram.title
            if (t != null) {
                val display = t.display
                if (display != null && display.size() > 0) {
                    title = display.first().toString()
                }
            }
        }

//        diagram.title?.display?.firstOrNull()?.let { title = it.toString() }
        diagram.entityFactory.leafs().forEach {
            when (it.leafType) {
                LeafType.CIRCLE_START -> {
                    val type = it.bodier.rawBody.firstOrNull()
                    states[it.uid] = State(name = "Begin", type = type)
                }
                LeafType.CIRCLE_END -> {
                    val type = it.bodier.rawBody.firstOrNull()
                    states[it.uid] = State(name = "End", type = type)
                }
                LeafType.STATE -> {
                    when (it.parentContainer.groupType) {
                        GroupType.STATE -> {
//                            println("group: ${it.codeGetName}")
                        }
                        else -> {

                        }
                    }
                    val type = it.bodier.rawBody.firstOrNull()
                    states[it.uid] = State(name = it.codeGetName, type = type)
                }
                else -> {}
            }
        }
        diagram.entityFactory.groups().forEach {
            val group = it.groupType
            val type = it.bodier.rawBody.firstOrNull()
            states[it.uid] = State( name = it.codeGetName, type = type )
        }
        diagram.entityFactory.links.forEach {
            val id1 = it.entity1.uid
            val id2 = it.entity2.uid

            val from = states.get(id1)
            val to = states.get(id2)

            if (from == null) {
                return@forEach
            }

            if (to == null) {
                return@forEach
            }

            if (from.name == "Begin") {
                from.type = if (it.label.size() == 0) null else it.label?.firstOrNull()?.toString()
            }

            if (to.name == "End") {
                to.type = if (it.label.size() == 0) null else it.label?.firstOrNull()?.toString()
            }

            val type: String? = if (it.label.size() == 0) null else it.label?.firstOrNull()?.toString()
            transitions += Transition(
                from = from,
                to = to,
                type = type
            )
        }
    }

    title = title.
        replace("-", "")
        .replace(" ", "")
        .replace("_", "")
        .capitalize()

    val generator =
        StateMachineGenerator(
            title = title,
            states = states.values,
            transitions = transitions
        )
    generator.namespace = namespace
    when (exportFormat) {
        ExportFormat.JAVA -> { generator.toKotlin(writer) }
        ExportFormat.KOTLIN -> { generator.toKotlin(writer) }
        ExportFormat.SWIFT -> { generator.toSwift(writer) }
    }

    reader.generatedImages.forEach { println("image: ${it.pngFile}") }
}