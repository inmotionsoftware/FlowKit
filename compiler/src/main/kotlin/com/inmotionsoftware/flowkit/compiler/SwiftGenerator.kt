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

fun Enumeration.toSwift(builder: Writer) {
    builder.append("public enum ${name}")
    if (!generics.isEmpty()) generics.joinTo(buffer=builder, prefix = "<", separator = ",", postfix = ">")
    builder.appendln("{")
    aliasess.joinTo(buffer=builder, separator = "\n\t") { "public typealias ${it.first} = ${it.second}" }
    builder.appendln()
    values.joinTo(buffer=builder, separator= "\n\t") {"case ${it.name.decapitalize()}(_ context: ${convertType(it.context)})" }
    builder.appendln()
    nested.forEach { it.toSwift(builder) }
    builder.appendln("}");
    builder.appendln()
}

fun StateMachineGenerator.toSwift(builder: Writer) {
    val stateEnum = Enumeration(stateName)
//    stateEnum.generics.add("Input")
//    stateEnum.generics.add("Output")
    val enums = mutableMapOf<String, Enumeration>()

    val input = convertType(transitions.find { it.from.name == "Begin" }?.type)
    val output = convertType(transitions.find { it.to.name == "End" }?.type)

    stateEnum.aliasess.add(Pair("Result", "Swift.Result<${output}, Error>"))

//    val back = Enumeration.Value("Back")
//    val cancel = Enumeration.Value("Cancel")
    val fail = Enumeration.Value("Fail","Error" )

//    stateEnum.values.add(back)
//    stateEnum.values.add(cancel)
    stateEnum.values.add(fail)

    states.forEach {
        val name = it.name
        stateEnum.values.add(Enumeration.Value(name, it.type ))
    }

    stateEnum.values.forEach {
        val name = it.name

        val enum = Enumeration(name = "${name}")
//        enum.values.add( if (name != "Back") back else Enumeration.Value("Terminate","Result<${output}>"))
//        enum.values.add( if (name != "Cancel") cancel else Enumeration.Value("Terminate", "Result<${output}>"))
        enum.values.add( if (name != "Fail") fail else Enumeration.Value("Terminate","Error" ))

        if (name == "End") {
            enum.values.add(Enumeration.Value("Terminate","${output}"))
        }
        enums[name] = enum
    }

    transitions.forEach {
        val enum = enums[it.from.name]!!
        enum.values.add(Enumeration.Value(it.to.name, convertType(it.to.type)))
    }

    var defaultInitialState: String? = null
    transitions.find { it.from.name == "Begin" }?.let {
        defaultInitialState = it.to.name
    }

    builder.appendln("""
    import Foundation
    import PromiseKit
    import FlowKit

    """.trimIndent())

    stateEnum.values.add(Enumeration.Value("Terminate", "Result"))
    stateEnum.nested = enums.values
    stateEnum.toSwift(builder)
    enums.forEach { Handler(stateMachineName, stateEnum.name,it.value).toSwift(builder) }

    builder.appendln("""
    public protocol ${stateMachineName}: Flow where Input == ${input}, Output == ${output} {
        typealias State = ${stateEnum.name}
        func onStateDidChange(prev: State, curr: State)
        func attach(_ promise: Promise<Output>) -> Promise<Output>
        ${ stateEnum.values.filter { it.name != "Terminate" }.concatln { "func on${it.name}(state: State, context: ${convertType(it.context)}) -> Promise<State.${it.name}>" }}
        func onTerminate(state: State, context: State.Result) -> Promise<State.Result>
    }


    extension ${stateMachineName} {
        func startFlow(context: ${input}) -> Promise<${output}> {
            let state = State.begin(context)
            return attach(nextState(prev: state, next: state)
            .map {
                switch($0) {
                    case .success(let context): return context
                    case .failure(let err): throw err
                }
            })
        }
        
        func attach(_ promise: Promise<Output>) -> Promise<Output> {
            return promise
        }

        func onStateDidChange(prev: State, curr: State) {}
        fileprivate func nextState(prev: State, next: State) -> Promise<Swift.Result<${output},Error>> {
            onStateDidChange(prev:prev, curr:next)
            switch(next) { 
            case .terminate(let context): return onTerminate(state: next, context: context)
            ${ stateEnum.values.filter { it.name != "Terminate" }.concatln { "case .${it.name.decapitalize()}(let context): return on${it.name}(state: prev, context: context).then{ $0.handle() }.then { self.nextState(prev: next, next: $0) }" } }
            }
        }
        
        func onTerminate(state: State, context: State.Result) -> Promise<State.Result> { return Promise.value(context) }
        func onEnd(state: State, context: ${output}) -> Promise<State.End> { return Promise.value(.terminate(context)) }
        func onFail(state: State, context: Error) -> Promise<State.Fail> { return Promise.value(.terminate(context)) }
    }
    """.trimIndent());

}

fun Handler.toSwift(builder: Writer) {
    val names = enum.values.map { it.name.decapitalize() }
    builder.appendln("""
    fileprivate extension ${name}.${enum.name} {
        func handle() -> Promise<${name}> {
            switch self { ${ 
                if (!names.contains("terminate")) 
                    ""
                else
                    if (enum.name == "Fail") {
                        "case .terminate(let context): return Promise.value(.terminate(.failure(context)))"
                    } else {
                        "case .terminate(let context): return Promise.value(.terminate(.success(context)))"
                    } 
            }
            ${ names.filter{ it != "terminate" }.concatln { "case .${it}(let context): return Promise.value(.${it}(context))" } }
            }
        }
    }
    """.trimIndent())
}