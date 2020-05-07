package com.inmotionsoftware.flowkit.compiler

import java.io.Writer
import java.lang.StringBuilder

private fun convertType(name: String?): String
        = when (name) {
    null -> "Void"
    else -> name
}

public fun <T, A : Appendable> Iterable<T>.concatln(buffer: A, transform: (T) -> CharSequence): A {
    return this.joinTo(buffer, separator="\n", transform = transform)
}

public fun <T> Iterable<T>.concatln(transform: (T) -> CharSequence): String {
    return this.concatln(buffer=StringBuilder(), transform=transform).toString()
}

public fun <T> Iterable<T>.fork(transform: (T) -> Boolean, a: (T) -> Unit, b: (T) -> Unit) {
    this.forEach { if (transform(it)) a(it) else b(it) }
}

fun Visibility.toSwift(): String =
    when (this) {
        Visibility.PUBLIC -> ""
        Visibility.PRIVATE -> "private"
        Visibility.PROTECTED -> "internal"
    }

fun UmlClass.toSwift(writer: Writer) {
    writer.appendln("""
        public struct ${this.name}: Codable {
            ${ this.fields.joinToString(separator="\n") { "${it.visibility.toSwift()} let ${it.name}: ${it.type} ${ if (it.def == null) "" else "= ${it.def}" }" } }
        }
    """.trimIndent())
}

fun writeSwiftHeader(writer: Writer) {
    writer.appendln("""
        import Foundation
        import PromiseKit
        import FlowKit         
        """.trimIndent())
}

fun Enumeration.toSwift(builder: Writer, isNested: Boolean = false) {
    builder.append("public enum ${name}")
    if (!generics.isEmpty()) generics.joinTo(buffer=builder, prefix = "<", separator = ",", postfix = ">")
    if (!isNested) builder.append(": FlowState")
    builder.appendln(" {")
    aliasess.joinTo(buffer=builder, separator = "\n\t", prefix = "\t") { "public typealias ${it.first} = ${it.second}" }
    builder.appendln()
    values.joinTo(buffer=builder, separator= "\n\t", prefix = "\t") {"case ${it.name.decapitalize()}(_ context: ${convertType(it.context)})" }
    builder.appendln()
    nested.forEach {
        builder.append("\t")
        it.toSwift(builder=builder, isNested=true)
        val context = when (it.name) {
            "Terminate" -> return@forEach
            "Fail" -> ".failure(context)"
            "End" -> ".success(context)"
            else -> "context"
        }
        builder.appendln("""    
        fileprivate init(substate: ${it.name}) {
            switch substate {
                ${ it.values.joinToString(separator = "\n\t\t") { "case .${it.name.decapitalize()}(let context): self = .${it.name.decapitalize()}(${context})" } }
            }            
            }""".trimIndent())
    }
    builder.appendln("}");
    builder.appendln()
}

fun StateMachineGenerator.toSwift(builder: Writer) {
    val stateEnum = Enumeration(stateName)
    val enums = mutableMapOf<String, Enumeration>()
    
    val input = convertType(states.find { it.name == "Begin" }?.type)
    val output = convertType(states.find { it.name == "End" }?.type)

    stateEnum.aliasess.add(Pair("Result", "Swift.Result<${output}, Error>"))

    states.forEach {
        val name = it.name
        stateEnum.values.add(Enumeration.Value(name, it.type ))
        enums[name] = Enumeration(name = name)
    }

    if (states.find { it.name == "Fail" } == null) {
        val fail = Enumeration.Value("Fail", "Error")
        stateEnum.values.add(fail)
        enums["Fail"] = Enumeration(name = "Fail")
    }

    val terminate = Enumeration.Value("Terminate", "Result" )
    stateEnum.values.add(terminate)

    enums["End"]?.values?.add(Enumeration.Value("Terminate", "${output}"))
    enums["Fail"]?.values?.add(Enumeration.Value("Terminate", "Error"))

    transitions.forEach {
        val enum = enums[it.from.name]!!
        enum.values.add(Enumeration.Value(it.to.name, convertType(it.to.type)))
    }

    var defaultInitialState: String? = null
    transitions.find { it.from.name == "Begin" }?.let {
        defaultInitialState = it.to.name
    }

//    stateEnum.values.add(Enumeration.Value("Terminate", "Result"))
    stateEnum.nested = enums.values
    stateEnum.toSwift(builder)
//    enums.forEach { Handler(stateMachineName, stateEnum.name,it.value).toSwift(builder) }

    builder.appendln("""
    public protocol ${stateMachineName}: StateMachine where State == ${this.stateName}, Input == ${input}, Output == ${output} {
        ${ stateEnum.values.filter{it.name != "Terminate"}.concatln { "func on${it.name}(state: State, context: ${convertType(it.context)}) -> Promise<State.${it.name}>" }}
    }

    public extension ${stateMachineName} {
        func getResult(state: State) -> Result? {
            switch state {
            case .terminate(let context): return context
            default: return nil
            }
        }

        func createState(context: Input) -> State { return .begin(context) } 
    
        func dispatch(state: State) -> Promise<State> {
            switch state {
            case .terminate(let context): return onTerminate(state: state, context: context).map { _ in State.terminate(context) }
            ${ enums.values.concatln { "case .${it.name.decapitalize()}(let context): return on${it.name}(state: state, context: context).map { State(substate: \$0) }" } }
            }
        }

        func onEnd(state: State, context: ${output}) -> Promise<State.End> { return Promise.value(.terminate(context)) }
        func onFail(state: State, context: Error) -> Promise<State.Fail> { return Promise.value(.terminate(context)) }
    }
    """.trimIndent());

}

//fun Handler.toSwift(builder: Writer) {
//    val names = enum.values.map { it.name.decapitalize() }
//    builder.appendln("""
//    fileprivate extension ${name}.${enum.name} {
//        func handle() -> Promise<${name}> {
//            switch self { ${
//                if (!names.contains("terminate"))
//                    ""
//                else
//                    if (enum.name == "Fail") {
//                        "case .terminate(let context): return Promise.value(.terminate(.failure(context)))"
//                    } else {
//                        "case .terminate(let context): return Promise.value(.terminate(.success(context)))"
//                    }
//            }
//            ${ names.filter{ it != "terminate" }.concatln { "case .${it}(let context): return Promise.value(.${it}(context))" } }
//            }
//        }
//    }
//    """.trimIndent())
//}
