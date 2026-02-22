package wafna.parser

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

fun <T> Iterator<T>.toList(): List<T> = buildList {
    while (hasNext()) {
        add(next())
    }
}

class TestLR0 {
    @Test
    fun lr0() {
        testInput(
            listOf(Number, Plus, Number),
            PTNode(
                fragment = Expr,
                children = listOf(
                    PTNode(Expr, listOf(PTNode(Term, listOf(PTNode(Number))))),
                    PTNode(Plus),
                    PTNode(Term, listOf(PTNode(Number)))
                )
            )
        )
        testInput(
            listOf(LParen, Number, RParen),
            PTNode(
                Expr,
                listOf(
                    PTNode(
                        Term, listOf(
                            PTNode(LParen),
                            PTNode(Expr, listOf(PTNode(Term, listOf(PTNode(Number))))),
                            PTNode(RParen)
                        )
                    )
                )
            )
        )
        testInput(
            listOf(Number, Plus, LParen, Number, Plus, Number, RParen),
            PTNode(
                Expr,
                listOf(
                    PTNode(Expr, listOf(PTNode(Term, listOf(PTNode(Number))))),
                    PTNode(Plus),
                    PTNode(
                        Term,
                        listOf(
                            PTNode(LParen),
                            PTNode(
                                Expr,
                                listOf(
                                    PTNode(Expr, listOf(PTNode(Term, listOf(PTNode(Number))))),
                                    PTNode(Plus),
                                    PTNode(Term, listOf(PTNode(Number)))
                                )
                            ),
                            PTNode(RParen)
                        )
                    )
                )
            )
        )
    }

    companion object {
        private fun testInput(input: List<Fragment>, expected: PTNode) {
            val input = input.iterator()
            val states = runGrammar(grammar)
//        states.withIndex().forEach { (index, state) ->
//            println(state.show)
//        }
            val actual = runInput(states, input)
            assertTrue(!input.hasNext(), "Remaining input: ${input.toList().joinToString()}")
            assertEquals(expected, actual, "Wrong parse tree.")
        }
    }
}