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

//    if (generics.size > 0) {
//        builder.append("<")
//        val iter = generics.iterator()
//        if (iter.hasNext()) {
//            builder.append(iter.next())
//            while (iter.hasNext()) {
//                builder.append(',')
//                builder.append(iter.next())
//            }
//        }
//        builder.append(">")
//    }

    builder.appendln("{")
    values.forEach {
        builder.appendln("\tcase ${it.name.decapitalize()}(_ context: ${convertType(it.context)})")
    }
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

    stateEnum.values.add(Enumeration.Value("Terminate", "${output}"))
    stateEnum.nested = enums.values
    stateEnum.toSwift(builder)
    enums.forEach { Handler(stateMachineName, stateEnum.name,it.value).toSwift(builder) }

    builder.appendln("""
    public protocol ${stateMachineName}: Flow where Input == ${input}, Output == ${output} {
        typealias State = ${stateEnum.name}
        func onStateDidChange(prev: State, curr: State)
        func attach(_ promise: Promise<Output>) -> Promise<Output>
        ${ stateEnum.values.filter { it.name != "Terminate" }.concatln { "func on${it.name}(state: State, context: ${convertType(it.context)}) -> Promise<State.${it.name}>" }}
    }


    extension ${stateMachineName} {
        func startFlow(context: ${input}) -> Promise<${output}> {
            let state = State.begin(context)
            return attach(nextState(prev: state, next: state)) 
        }
        
        func attach(_ promise: Promise<Output>) -> Promise<Output> {
            return promise
        }

        func onStateDidChange(prev: State, curr: State) {}
        fileprivate func nextState(prev: State, next: State) -> Promise<${output}> {
            onStateDidChange(prev:prev, curr:next)
            switch(next) { 
            ${ stateEnum.values.filter { it.name == "Terminate" }.concatln { "case .${it.name.decapitalize()}(let context): return Promise.value(context)" } }
            ${ stateEnum.values.filter { it.name != "Terminate" }.concatln { "case .${it.name.decapitalize()}(let context): return on${it.name}(state: prev, context: context).then{ $0.handle() }.then { self.nextState(prev: next, next: $0) }" } }
            }
        }
        
        ${stateEnum.values.filter { it.name == "End" }.concatln{"func on${it.name}(state: State, context: ${convertType(it.context)}) -> Promise<State.${it.name}> { return Promise.value(.terminate(context)) }" }}
        ${stateEnum.values.filter { it.name == "Fail" }.concatln{"func on${it.name}(state: State, context: ${convertType(it.context)}) -> Promise<State.${it.name}> { return Promise(error: context) }" }}
    }
    """.trimIndent());

}

fun Handler.toSwift(builder: Writer) {
    val names = enum.values.map { it.name.decapitalize() }
    builder.appendln("""
    fileprivate extension ${name}.${enum.name} {
        func handle() -> Promise<${name}> {
            switch self {
            ${ if (enum.name == "Fail") {
                    names.filter { it == "terminate" }.concatln { "case .${it}(let context): return Promise(error: context)" }
                } else {
                    names.filter { it == "terminate" }.concatln { "case .${it}(let context): return Promise.value(.${it}(context))" }
                } }
            ${ names.filter{ it != "terminate" }.concatln { "case .${it}(let context): return Promise.value(.${it}(context))" } }
            }
        }
    }
    """.trimIndent())
}