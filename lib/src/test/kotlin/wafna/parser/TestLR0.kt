package wafna.parser

import kotlin.test.Test
import kotlin.test.assertEquals

fun <T> Iterator<T>.toList(): List<T> = buildList {
    while (hasNext()) {
        add(next())
    }
}

class TestLR0 {
    @Test
    fun lr0() {
        val states = runGrammar(grammar)
//        states.withIndex().forEach { (index, state) ->
//            println(state.show)
//        }
        val input = listOf(Number, Plus, Number).iterator()
        val parseTree = runInput(states, input)
        require(!input.hasNext()) {
            "Remaining input: ${input.toList().joinToString()}"
        }
        assertEquals(
            parseTree,
            PTNode(
                fragment = Expr,
                children = listOf(
                    PTNode(Expr, listOf(PTNode(Term, listOf(PTNode(Number))))),
                    PTNode(Plus),
                    PTNode(Term, listOf(PTNode(Number)))
                )
            )
        )
    }
}