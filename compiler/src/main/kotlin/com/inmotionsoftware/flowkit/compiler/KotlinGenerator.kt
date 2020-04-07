package com.inmotionsoftware.flowkit.compiler

import java.io.Writer

private fun convertType(name: String?): String
        = when (name) {
    null -> "Unit"
    else -> name
}

fun Enumeration.toKotlin(builder: Writer) {
    builder.appendln("sealed class ${name} {")
    values.forEach {
        builder.appendln("\tclass ${it.name}(val context: ${convertType(
            it.context
        )}): ${name}()")
    }
    nested.forEach { it.toSwift(builder) }
    builder.appendln("}");
    builder.appendln()
}

fun Handler.toKotlin(builder: Writer) {
    builder.appendln("private fun ${enum.name}.handle(stateMachine: ${state}): Promise<State> =")
    builder.appendln("\twhen (this) {")
    enum.values.forEach {
        if (enum.name == "Fail" && it.name == "Terminate") {
            builder.appendln("\t\tis ${enum.name}.${it.name} -> Promise(error=context)")
        } else {
            builder.appendln("\t\tis ${enum.name}.${it.name} -> Promise.value(State.${it.name}(context))")
        }
    }
    builder.appendln("\t}")
    builder.appendln()
}

fun StateMachineGenerator.toKotlin(builder: Writer) {

    val stateEnum =
        Enumeration("State")
    val enums = mutableMapOf<String, Enumeration>()

//    val back = Enumeration.Value("Back")
//    val cancel = Enumeration.Value("Cancel")
    val fail = Enumeration.Value(
        "Fail",
        "Throwable"
    )

    transitions.forEach {
        if (it.from.name == "Begin") {
            it.from.type = it.type
        }

        if (it.to.name == "End") {
            it.to.type = it.type
        }
    }

    states.forEach {
        val name = it.name
        stateEnum.values.add(
            Enumeration.Value(
                name,
                it.type
            )
        )
    }

    var endType: String?
    states.first { it.name == "End" }.let {
        endType = it.type
    }
    val output = endType ?: "Unit"

    var beginType: String?
    states.first { it.name == "Begin" }.let {
        beginType = it.type
    }
    val input = beginType ?: "Unit"

//    stateEnum.values.add(back)
//    stateEnum.values.add(cancel)
    stateEnum.values.add(fail)

    stateEnum.values.forEach {
        val name = it.name

        val enum =
            Enumeration(name = "${name}")
//        enum.values.add( if (name != "Back") back else Enumeration.Value("Terminate", "Result<${output}>"))
//        enum.values.add( if (name != "Cancel") cancel else Enumeration.Value("Terminate", "Result<${output}>"))
        enum.values.add( if (name != "Fail") fail else Enumeration.Value(
            "Terminate",
            "Throwable"
        )
        )

        if (name == "End") {
            enum.values.add(
                Enumeration.Value(
                    "Terminate",
                    "Result<${output}>"
                )
            )
        }
        enums[name] = enum
    }

    transitions.forEach {
        val name = it.from.name
        val enum = enums[name]!!
        enum.values.add(
            Enumeration.Value(
                it.to.name,
                it.to.type
            )
        )
    }

    var defaultInitialState: String? = null
    transitions.find { it.from.name == "Begin" }?.let {
        defaultInitialState = it.to.name
    }

//    builder.appendln("@file:JvmName(\"${stateMachineName}\")")
    if (namespace.isNotEmpty()) builder.appendln("package ${namespace}")
    builder.appendln("import com.inmotionsoftware.promisekt.Promise")
    builder.appendln("import com.inmotionsoftware.promisekt.thenMap")
    builder.appendln("import com.inmotionsoftware.promisekt.recover")
    builder.appendln("import com.inmotionsoftware.flowkit.*")

    builder.appendln()
    builder.appendln()
    builder.appendln("typealias Result<T> = FlowResult<T>")
    builder.appendln()
    builder.appendln("interface ${stateMachineName}: Flow<${input}, ${output}> {")

    stateEnum.values.add(
        Enumeration.Value(
            "Terminate",
            "Result<${output}>"
        )
    )
    stateEnum.toKotlin(builder)
    enums.forEach {
        it.value.toKotlin(builder)
        Handler(
            stateMachineName,
            stateEnum.name,
            it.value
        ).toKotlin(builder)
    }

    builder.appendln("fun onStateDidChange(prev: State, curr: State) {}")
    builder.appendln("override fun start(context: ${input}): Promise<Result<${convertType(
        output
    )}>> = next(State.Begin(context), State.Begin(context))")
    builder.appendln("private fun next(prev: State, next: State): Promise<Result<${output}>> {")
    builder.appendln("try { onStateDidChange(prev=prev, curr=next) } catch (e: Throwable) {}")
    builder.appendln("\treturn when(next) {")
    stateEnum.values.forEach {
        if (it.name == "Terminate") {
            builder.appendln("\t\tis State.${it.name} -> Promise.value(next.context)")
        } else {
            builder.append("\t\tis State.${it.name} -> on${it.name}(prev, next.context).thenMap{ it.handle(this) }.thenMap{ next(next, it) }")
            if (it.name != "Fail") {
                builder.append(".recover { next(next, State.Fail(context=it)) }")
            }
            builder.appendln()
        }
    }
    builder.appendln("\t}")
    builder.appendln("}")

    stateEnum.values.forEach {
        val type =
            convertType(it.context)
        builder.appendln(when (it.name) {
//            "Begin" -> "fun on${it.name}(state: State, context: ${type}): Promise<${it.name}> = Promise.value(${it.name}.${defaultInitialState!!}(context))"
//            "Back" -> "fun on${it.name}(state: State, context: ${type}): Promise<${it.name}> = Promise.value(${it.name}.Terminate(Result.Back(Unit)))"
//            "Cancel" -> "fun on${it.name}(state: State, context: ${type}): Promise<${it.name}> = Promise.value(${it.name}.Terminate(Result.Cancel(Unit)))"
            "Fail" -> "fun on${it.name}(state: State, context: ${type}): Promise<${it.name}> = Promise(error=context)"
            "End" -> "fun on${it.name}(state: State, context: ${type}): Promise<${it.name}> = Promise.value(${it.name}.Terminate(Result.Success(context)))"
            "Terminate" -> ""
            else -> "fun on${it.name}(state: State, context: ${type}): Promise<${it.name}>"
        })
    }
    builder.appendln("}")
    builder.appendln()
}