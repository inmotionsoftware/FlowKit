package com.inmotionsoftware.flowkit.compiler

import java.io.Writer

private fun convertType(name: String?): String
        = when (name) {
    null -> "Unit"
    else -> name
}

fun Enumeration.toKotlin(builder: Writer) {
    builder.append("sealed class ${name}()")
    if (!generics.isEmpty()) generics.joinTo(buffer=builder, prefix = "<", separator = ",", postfix = ">")
    builder.appendln(" {")
    aliasess.joinTo(buffer=builder, separator = "\n\t", prefix = "\t") { "public typealias ${it.first} = ${it.second}" }
    builder.appendln()
    values.joinTo(buffer=builder, separator= "\n\t", prefix = "\t") { "class ${it.name.capitalize()}(val context: ${convertType(it.context)}): ${name}()" }
    builder.appendln()
    nested.forEach {
        builder.append("\t")
        it.toKotlin(builder)
        val context = when (it.name) {
            "Terminate" -> return@forEach
            "Fail" -> ".Failure(context)"
            "End" -> ".Success(context)"
            else -> "context"
        }
//        builder.appendln("""
//        constructor(substate: ${it.name}): this() {
//            when (substate) {
//                ${ it.values.joinToString(separator = "\n\t\t") { "is ${it.name.capitalize()} -> this = ${name}.${it.name.capitalize()}(context=substate.context)" } }
//            }
//            }""".trimIndent())
    }
    builder.appendln("}");
    builder.appendln()
}

fun Handler.toKotlin(builder: Writer) {
    builder.appendln("""
        internal fun to${this.state}(substate: ${this.state}.From${this.name}): ${this.state} = 
            when (substate) {
                ${ this.enum.values.filter{ it.name != "Terminate" }.joinToString(separator="\n\t") { "is ${this.state}.From${this.name}.${it.name} -> ${this.state}.${it.name}(context=substate.context)" } }
                ${ this.enum.values.filter{ it.name == "Terminate" }.joinToString(separator="\n\t") { "is ${this.state}.From${this.name}.${it.name} -> ${this.state}.${it.name}(context=Result.${ if (this.name == "End") "Success" else "Failure" }(substate.context))" } }
            }
    """.trimIndent())
}

fun StateMachineGenerator.toKotlin(builder: Writer) {
    val stateEnum = Enumeration(stateName)
    val enums = mutableMapOf<String, Enumeration>()

    val input = convertType(transitions.find { it.from.name == "Begin" }?.type)
    val output = convertType(transitions.find { it.to.name == "End" }?.type)
    val result = "Result<${output}>"

    states.forEach {
        val name = it.name
        stateEnum.values.add(Enumeration.Value(name, it.type ))
        enums[name] = Enumeration(name = "From${name}")
    }

    if (states.find { it.name == "Fail" } == null) {
        val fail = Enumeration.Value("Fail", "Throwable")
        stateEnum.values.add(fail)
        enums["Fail"] = Enumeration(name = "FromFail")
    }

    val terminate = Enumeration.Value("Terminate", result )
    stateEnum.values.add(terminate)

    enums["End"]?.values?.add(Enumeration.Value("Terminate", "${output}"))
    enums["Fail"]?.values?.add(Enumeration.Value("Terminate", "Throwable"))

    transitions.forEach {
        val enum = enums[it.from.name]!!
        enum.values.add(Enumeration.Value(it.to.name, convertType(it.to.type)))
    }

    var defaultInitialState: String? = null
    transitions.find { it.from.name == "Begin" }?.let {
        defaultInitialState = it.to.name
    }

    builder.appendln("""
        package ${ if (namespace.isNotEmpty()) namespace else "com.inmotionsoftware.flowkit.generated" }
        import com.inmotionsoftware.promisekt.Promise
        import com.inmotionsoftware.promisekt.thenMap
        import com.inmotionsoftware.promisekt.map
        import com.inmotionsoftware.promisekt.then
        import com.inmotionsoftware.promisekt.recover
        import com.inmotionsoftware.flowkit.*
        
        """.trimIndent())

    stateEnum.nested = enums.values
    stateEnum.toKotlin(builder)

    enums.forEach {
        Handler(state=stateEnum.name, name=it.key, enum=it.value).toKotlin(builder)
    }

    builder.appendln("""
        interface ${stateMachineName}: StateMachine<${stateName}, ${input}, ${output}> {
            ${ stateEnum.values
                .filter{ it.name != "Terminate"  && it.name != "End" && it.name != "Fail" }
                .joinToString(separator = "\n\t") { "fun on${it.name}(state: ${stateName}, context: ${convertType(it.context)}): Promise<${stateName}.From${it.name}>" } 
            }

            fun onEnd(state: ${stateName}, context: ${output}) :  Promise<${stateName}.FromEnd> = Promise.value(${stateName}.FromEnd.Terminate(context))
            fun onFail(state: ${stateName}, context: Throwable) :  Promise<${stateName}.FromFail> = Promise.value(${stateName}.FromFail.Terminate(context))
            fun onTerminate(state: ${stateName}, context: ${result}) :  Promise<${result}> = Promise.value(context)

            override fun dispatch(state: ${stateName}): Promise<${stateName}> =
                when (state) {
                    ${ stateEnum.values.filter{ it.name != "Terminate" }.joinToString(separator = "\n\t") { "is ${stateName}.${it.name} -> on${it.name}(state=state, context=state.context).map { to${stateName}(substate=it) }" } }
                    is ${stateName}.Terminate -> onTerminate(state=state, context=state.context).map { ${stateName}.Terminate(context=it) }
                }
        
            override fun terminal(state: ${stateName}): ${result}? =
                when (state) {
                    is ${stateName}.Terminate -> state.context
                    else -> null
                }
        
            override fun firstState(context: ${input}): ${stateName} = ${stateName}.Begin(context=context)
        }


    """.trimIndent())
}