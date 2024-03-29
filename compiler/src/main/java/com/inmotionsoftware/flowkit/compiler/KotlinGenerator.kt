// MIT License
//
// Permission is hereby granted, free of charge, to any person obtaining a 
// copy of this software and associated documentation files (the "Software"), 
// to deal in the Software without restriction, including without limitation 
// the rights to use, copy, modify, merge, publish, distribute, sublicense, 
// and/or sell copies of the Software, and to permit persons to whom the 
// Software is furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER 
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING 
// FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
// IN THE SOFTWARE.
//
//  KotlinGenerator.kt
//  FlowKit
//
//  Created by Brian Howard
//  Copyright © 2020 InMotion Software, LLC. All rights reserved.
//

package com.inmotionsoftware.flowkit.compiler

import java.io.Writer

private fun convertType(name: String?): String
        = when (name) {
    null -> "Unit"
    else -> name
}

fun Enumeration.toKotlin(builder: Writer, isNested: Boolean = false) {
    builder.append("sealed class ${name}")
    if (!generics.isEmpty()) generics.joinTo(buffer=builder, prefix = "<", separator = ",", postfix = ">")
    builder.append("()")
    if (!isNested) {
        builder.append(": Parcelable, FlowState")
    }
    builder.appendln(" {")
    aliasess.joinTo(buffer=builder, separator = "\n\t", prefix = "\t") { "public typealias ${it.first} = ${it.second}" }
    builder.appendln()
    if (!isNested) {
        values.joinTo(buffer=builder, separator= "\n\t", prefix = "\t") { "@Parcelize class ${it.name.capitalize()}(val context: ${convertType(it.context)}): ${name}(), Parcelable" }
    } else {
        values.joinTo(buffer=builder, separator= "\n\t", prefix = "\t") { "class ${it.name.capitalize()}(val context: ${convertType(it.context)}): ${name}()" }
    }
    builder.appendln()
    nested.forEach {
        builder.append("\t")
        it.toKotlin(builder=builder, isNested=true)
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

fun UmlEnum.toKotlin(writer: Writer) {
    val hasTypes = this.cases.any { it.value.type != null }
    val name = this.name.toJavaCase()
    if (hasTypes) {
        writer.appendln("sealed class ${name} {")
        this.cases.values.filter { it.type != null }.joinTo(writer, separator = "\t\n", postfix = "\n") { "@Parcelize class ${it.name.toJavaCase()}(val context: ${it.type}): ${name}(), Parcelable" }
        this.cases.values.filter { it.type == null }.joinTo(writer, separator = "\t\n", postfix = "\n") { "@Parcelize class ${it.name.toJavaCase()}: ${name}(), Parcelable" }
        writer.appendln("}")
    } else {
        writer.appendln("enum class ${name} {")
        this.cases.values.joinTo(writer, separator = ",\n\t", postfix = "\n") { it.name }
        writer.appendln("}")
    }
}

fun Visibility.toKotlin(): String =
    when (this) {
        Visibility.PUBLIC -> ""
        Visibility.PRIVATE -> "private"
        Visibility.PROTECTED -> "protected"
    }


fun UmlClass.toKotlin(writer: Writer) {
    writer.appendln("@Parcelize data class ${this.name}( ${this.fields.joinToString(separator=",") { "${it.visibility.toKotlin()} val ${it.name}: ${it.type} ${ if (it.def == null) "" else "= ${it.def}" }" } } ): Parcelable")
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

fun writeKotlinHeader(namespace: String, writer: Writer) {
    writer.appendln("""
        package ${ if (namespace.isNotEmpty()) namespace else "com.inmotionsoftware.flowkit.generated" }
        import com.inmotionsoftware.promisekt.Promise
        import com.inmotionsoftware.promisekt.thenMap
        import com.inmotionsoftware.promisekt.map
        import com.inmotionsoftware.promisekt.then
        import com.inmotionsoftware.promisekt.recover
        import com.inmotionsoftware.flowkit.*
        import android.os.Parcelable
        import kotlinx.parcelize.Parcelize
        """.trimIndent())
}

fun StateMachineGenerator.toKotlin(builder: Writer) {
    val stateEnum = Enumeration(stateName)
    val enums = mutableMapOf<String, Enumeration>()

    val input = convertType(states.find { it.name == "Begin" }?.type)
    val output = convertType(states.find { it.name == "End" }?.type)
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

            @Parcelize class Input(val value:  ${input}): Parcelable

            fun onEnd(state: ${stateName}, context: ${output}) :  Promise<${stateName}.FromEnd> = Promise.value(${stateName}.FromEnd.Terminate(context))
            fun onFail(state: ${stateName}, context: Throwable) :  Promise<${stateName}.FromFail> = Promise.value(${stateName}.FromFail.Terminate(context))

            override fun dispatch(prev: ${stateName}, state: ${stateName}): Promise<${stateName}> =
                when (state) {
                    ${ stateEnum.values.filter{ it.name != "Terminate" }.joinToString(separator = "\n\t") { "is ${stateName}.${it.name} -> on${it.name}(state=state, context=state.context).map { to${stateName}(substate=it) }" } }
                    is ${stateName}.Terminate -> onTerminate(state=state, context=state.context)
                        .map { ${stateName}.Terminate(context=Result.Success(it)) as ${stateName} }
                        .recover { Promise.value(${stateName}.Terminate(Result.Failure(it)) as ${stateName}) }
                }
                
            
            override fun getResult(state: ${stateName}): ${result}? =
                when (state) {
                    is ${stateName}.Terminate -> state.context
                    else -> null
                }
        
            override fun createState(error: Throwable):  ${stateName} = ${stateName}.Fail(context=error)        
            override fun createState(context: ${input}): ${stateName} = ${stateName}.Begin(context=context)
        }


    """.trimIndent())
}