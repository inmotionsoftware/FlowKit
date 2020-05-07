package com.inmotionsoftware.flowkit.compiler
import net.sourceforge.plantuml.*
import net.sourceforge.plantuml.classdiagram.ClassDiagram
import net.sourceforge.plantuml.cucadiagram.IEntity
import net.sourceforge.plantuml.cucadiagram.IGroup
import net.sourceforge.plantuml.cucadiagram.LeafType
import net.sourceforge.plantuml.objectdiagram.ObjectDiagram
import net.sourceforge.plantuml.skin.VisibilityModifier
import net.sourceforge.plantuml.statediagram.StateDiagram
import java.io.*
import java.lang.IllegalArgumentException

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

class StateMachine(val name: String) {
    val states = mutableMapOf<String, State>()
    val transitions = mutableListOf<Transition>()
}

fun checkReservedKeyword(name: String): Boolean {
    return name == "Begin" || name == "End" || name == "Terminate" || name == "Fail"
}

fun tabln(tabs: Int, msg: String) {
//    for (i in 0..tabs) print('\t')
//    println(msg)
}

fun doGroup(name: String, state: StateDiagram, group: IGroup, depth: Int = 0): List<StateMachine> {
//    println("---------------------------------------")

    val sm = StateMachine(name=name.toJavaCase())
    val list = mutableListOf<StateMachine>(sm)
    group.leafsDirect.forEach {
        when (it.leafType) {
            LeafType.CIRCLE_START -> {
                tabln(depth, "Begin")

                val one = try {
                    group.bodier?.rawBody?.firstOrNull()
                } catch (t: Throwable) {
                    null
                }
                val two = it?.bodier?.rawBody?.firstOrNull()
                
                var type = one
                if (type == null) type = two

                sm.states[it.uid] = State(name="Begin", type=type)
            }
            LeafType.CIRCLE_END -> {
                tabln(depth, "End")
                val type = it.bodier.rawBody.firstOrNull()
                sm.states[it.uid] = State(name="End", type=type)

            }
            LeafType.STATE -> {
                val type = it.bodier.rawBody.firstOrNull()
                val name = it.codeGetName.toJavaCase()
                if (checkReservedKeyword(name)) {
                    IllegalArgumentException("${name} is a reserved state")
                }
                tabln(depth, name)
                sm.states[it.uid] = State(name=name, type = type)
            }

            // ignore
            LeafType.NOTE -> {}
            LeafType.ANNOTATION -> {}

            else -> {
                throw IllegalArgumentException("Unsupported leaf type: ${it.leafType}")
            }
        }
    }

    group.children.forEach {
        val name = it.codeGetName.toJavaCase()
        val sub = doGroup(name=name, state=state, group=it, depth=depth+1)
        list.addAll(sub)

        val type = it.bodier?.rawBody?.firstOrNull()
        sm.states[it.uid] = State(name=name, type=type) // TODO: get the correct type...
    }

    return list
}

fun process(stateDiagram: StateDiagram, title: String): List<StateMachine> {
    val states = doGroup(name=title, state=stateDiagram, group=stateDiagram.rootGroup)
    
    val checkType = fun (entity: IEntity): Boolean =
        when (entity.leafType) {
            LeafType.STATE -> true
            LeafType.CIRCLE_START -> true
            LeafType.CIRCLE_END -> true
            null -> true
            else -> {
                println("$entity type: ${entity.leafType}")
                false
            }
        }

    stateDiagram
        .links
        .filter{ checkType(it.entity1) && checkType(it.entity2) }
        .forEach {
            val id1 = it.entity1.uid
            val id2 = it.entity2.uid

            val sm = states.find { it.states.containsKey(id1) }
            checkNotNull(sm)

            val from = sm.states.get(id1)
            val to = sm.states.get(id2)

//            println("from: ${it.entity1.codeGetName} to: ${it.entity2.codeGetName}")

            checkNotNull(from)
            checkNotNull(to)

            if (from.name == "Begin" && from.type == null) {
                // get the type from the transition
                from.type = if (it.label.size() == 0) null else it.label?.firstOrNull()?.toString()
            }

            if (to.name == "End" && to.type == null) {
                // get the type from the transition
                to.type = if (it.label.size() == 0) null else it.label?.firstOrNull()?.toString()
            }

            val type: String? = if (it.label.size() == 0) null else it.label?.firstOrNull()?.toString()
            sm.transitions += Transition(from=from, to=to, type=type)
        }
    return states
}

enum class Visibility {
    PUBLIC,
    PROTECTED,
    PRIVATE
}

class UmlField(val visibility: Visibility, val name: String, val type: String, val def: String?)
class UmlMethod(val visibility: Visibility, val name: String, args: List<String>, result: String?)
class UmlClass(val name: String) {
    val fields = mutableListOf<UmlField>()
}

fun process(classDiagram: ClassDiagram, title: String): List<UmlClass> {
    val classes = mutableListOf<UmlClass>()

    classDiagram.entityFactory.leafs2()
        .filter { it.leafType == LeafType.CLASS }
        .forEach {

        val clazz = UmlClass(it.codeGetName.toString().toJavaCase())

        val modifier = it.visibilityModifier
        val type = it.leafType
        val symbol = it.uSymbol
        val generic = it.generic
        it.bodier.fieldsToDisplay.forEach {

            val str = it.getDisplay(false)
            val strs = str.split(":")
            val name = strs.first()
            var type = strs.last()

            var def: String? = null
            if (type.contains('=')) {
                val vals = type.split("=")
                type = vals.first()
                def = vals.last()
            }

            val vis = when (it.visibilityModifier ?: VisibilityModifier.PUBLIC_FIELD) {
                VisibilityModifier.PRIVATE_FIELD -> Visibility.PRIVATE
                VisibilityModifier.PUBLIC_FIELD -> Visibility.PUBLIC
                VisibilityModifier.PROTECTED_FIELD -> Visibility.PROTECTED
                else -> throw IllegalArgumentException("Unsupported visibility ${it.visibilityModifier.toString()}")
            }
            clazz.fields.add(UmlField(visibility=vis, name=name.trim(), type=type.trim(), def=def?.trim()))
        }

        if (it.bodier.methodsToDisplay.size > 0) {
            throw IllegalStateException("Methods are unsupported")
        }
        classes.add(clazz)
    }
    return classes
}


fun process(objectDiagram: ObjectDiagram, title: String) {
    TODO("Object diagrams are unsupported")
}

fun String.toJavaCase(): String {
    return this
        .replace("-", "")
        .replace(" ", "")
        .replace("_", "")
        .capitalize()
}

fun processPuml(namespace: String, inputFile: File, imageDir: File?, exportFormat: ExportFormat, writer: Writer): List<File> {
    var title: String = inputFile.nameWithoutExtension

    val files = mutableListOf<File>()

    val outdir = imageDir ?: inputFile.parentFile
    outdir.mkdirs()
    val option = FileFormatOption(if (imageDir != null) FileFormat.PNG else FileFormat.XMI_STANDARD )

    val reader: ISourceFileReader = SourceFileReader(inputFile, outdir, option)
    reader.setCheckMetadata(false)

    when (exportFormat) {
        ExportFormat.JAVA -> { writeKotlinHeader(namespace=namespace, writer=writer) }
        ExportFormat.KOTLIN -> { writeKotlinHeader(namespace=namespace, writer=writer) }
        ExportFormat.SWIFT -> { writeSwiftHeader(writer=writer) }
    }

    reader.blocks.forEach {

        try {
            // UML diagrams
            val diagram = it.diagram
            if (diagram is TitledDiagram) {
                diagram.title?.let {
                    val display = it.display
                    if (display != null && display.size() > 0) {
                        title = display.first().toString()
                    }
                }
            }

            when (diagram) {
                is StateDiagram -> {
                    val list = process(stateDiagram = diagram, title=title)
                    list.forEach {
                        val generator = StateMachineGenerator(title=it.name, states=it.states.values, transitions=it.transitions)
                        generator.namespace = namespace
                        when (exportFormat) {
                            ExportFormat.JAVA -> { generator.toKotlin(writer) }
                            ExportFormat.KOTLIN -> { generator.toKotlin(writer) }
                            ExportFormat.SWIFT -> { generator.toSwift(writer) }
                        }
                    }
                }
                is ClassDiagram -> {
                    val classes = process(classDiagram = diagram, title = title)
                    when (exportFormat) {
                        ExportFormat.JAVA -> { classes.forEach { it.toKotlin(writer) } }
                        ExportFormat.KOTLIN -> { classes.forEach { it.toKotlin(writer) } }
                        ExportFormat.SWIFT -> { classes.forEach { it.toSwift(writer) } }
                    }
                }
                is ObjectDiagram -> process(objectDiagram = diagram, title = title)
                else -> throw IllegalArgumentException("Unsupported uml diagram type")
            }
        } catch (t: Throwable) {
            printErrLn("${inputFile.absolutePath}: error: ${t.localizedMessage ?: t.toString()}")
        }

        imageDir?.let {
            reader.generatedImages.forEach { files += it.pngFile }
        }
    }

    return files
}