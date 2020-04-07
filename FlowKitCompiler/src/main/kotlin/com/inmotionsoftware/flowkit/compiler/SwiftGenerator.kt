package com.inmotionsoftware.flowkit.compiler

import java.io.Writer

private fun convertType(name: String?): String
        = when (name) {
    null -> "Void"
    else -> name
}


fun Enumeration.toSwift(builder: Writer) {
    builder.append("public enum ${name}")

    if (generics.size > 0) {
        builder.append("<")

        val iter = generics.iterator()
        if (iter.hasNext()) {
            builder.append(iter.next())
            while (iter.hasNext()) {
                builder.append(',')
                builder.append(iter.next())
            }
        }
        builder.append(">")
    }

    builder.appendln("{")

    values.forEach {
        builder.appendln("\tcase ${it.name.decapitalize()}(_ context: ${convertType(
            it.context
        )})")
    }
    nested.forEach { it.toSwift(builder) }
    builder.appendln("}");
    builder.appendln()
}

fun StateMachineGenerator.toSwift(builder: Writer) {
    val stateEnum =
        Enumeration(stateName)
    stateEnum.generics.add("Input")
    stateEnum.generics.add("Output")
    val enums = mutableMapOf<String, Enumeration>()

    val back =
        Enumeration.Value("Back")
    val cancel =
        Enumeration.Value("Cancel")
    val fail = Enumeration.Value(
        "Fail",
        "Error"
    )

    transitions.forEach {
        if (it.from.name == "Begin") {
            it.from.type = "Input"
        }

        if (it.to.name == "End") {
            it.to.type = "Output"
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

    val output = "Output"
    val input = "Input"

//    var output: String? = null
//    states.first { it.name == "End" }.let {
//        output = it.type
//    }
//
//    var input: String? = null
//    states.first { it.name == "Begin" }.let {
//        input = it.type
//    }

    stateEnum.values.add(back)
    stateEnum.values.add(cancel)
    stateEnum.values.add(fail)

    stateEnum.values.forEach {
        val name = it.name

        val enum =
            Enumeration(name = "${name}")
        enum.values.add( if (name != "Back") back else Enumeration.Value(
            "Terminate",
            "Result<${output}>"
        )
        )
        enum.values.add( if (name != "Cancel") cancel else Enumeration.Value(
            "Terminate",
            "Result<${output}>"
        )
        )
        enum.values.add( if (name != "Fail") fail else Enumeration.Value(
            "Terminate",
            "Error"
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
        val enum = enums[it.from.name]!!
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

    builder.appendln("import PromiseKit")
    builder.appendln()
    builder.appendln()

    stateEnum.values.add(
        Enumeration.Value(
            "Terminate",
            "Result<${output}>"
        )
    )
    stateEnum.nested = enums.values
    stateEnum.toSwift(builder)

    enums.forEach {
        Handler(
            stateMachineName,
            stateEnum.name,
            it.value
        ).toSwift(builder)
    }

    builder.appendln()
    builder.appendln("public protocol ${stateMachineName} {")
    builder.appendln("associatedtype ${output}")
    builder.appendln("associatedtype ${input}")
    builder.appendln("typealias State = ${stateEnum.name}<${input}, ${output}>")

    builder.appendln("func onStateDidChange(prev: State, curr: State)")
    builder.appendln("func start(context: ${input}) -> Promise<Result<${convertType(
        output
    )}>>")

    stateEnum.values.forEach {
        if (it.name != "Terminate") {
            val type =
                convertType(it.context)
            builder.appendln("func on${it.name}(state: State, context: ${type}) -> Promise<State.${it.name}>")
        }
    }
    builder.appendln("}");

    builder.appendln("public extension ${stateMachineName} {")
    builder.appendln("func start(context: ${input}) -> Promise<Result<${output}>> { return nextState(prev: .begin(context), next: .begin(context)) }")

    builder.appendln("func onStateDidChange(prev: State, curr: State) {}")
    builder.appendln("private func nextState(prev: State, next: State) -> Promise<Result<${output}>> {")
    builder.appendln("\tonStateDidChange(prev:prev, curr:next)")
    builder.appendln("\tswitch(next) {")
    stateEnum.values.forEach {
        if (it.name == "Terminate") {
            builder.appendln("\t\tcase .${it.name.decapitalize()}(let context): return Promise.value(context)")
        } else {
            builder.appendln("\t\tcase .${it.name.decapitalize()}(let context): return on${it.name}(state: prev, context: context).then{ \$0.handle() }.then { self.nextState(prev: next, next: \$0) }")
        }
    }
    builder.appendln("\t}")
    builder.appendln("}")

    stateEnum.values.forEach {
        val type =
            convertType(it.context)
        builder.appendln(when (it.name) {
            "Back" -> "func on${it.name}(state: State, context: ${type}) -> Promise<State.${it.name}> { return Promise.value(.terminate(Result.back(()))) }"
            "Cancel" -> "func on${it.name}(state: State, context: ${type}) -> Promise<State.${it.name}> { return Promise.value(.terminate(Result.cancel(()))) }"
            "End" -> "func on${it.name}(state: State, context: ${type}) -> Promise<State.${it.name}> { return Promise.value(.terminate(Result.success(context))) }"
            "Fail" -> "func on${it.name}(state: State, context: ${type}) -> Promise<State.${it.name}> { return Promise(error:context) }"
            else -> ""
        })
    }
    builder.appendln("}");
    builder.appendln()
}

fun Handler.toSwift(builder: Writer) {
    builder.appendln("fileprivate extension ${name}.${enum.name} {")
    builder.appendln("func handle() -> Promise<${name}> {");
    builder.appendln("switch self {")
    enum.values.forEach {
        val case = it.name.decapitalize()
        if (enum.name == "Fail" && it.name == "Terminate") {
            builder.appendln("case .${case}(let context): return Promise(error: context)")
        } else {
            builder.appendln("case .${case}(let context): return Promise.value(.${case}(context))")
        }
    }
    builder.appendln("}")
    builder.appendln("}");
    builder.appendln("}")
    builder.appendln()
}