package wafna.parser

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestLR0 {
    @Test
    fun `x + y`() {
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
    }

    @Test
    fun `(x)`() {
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
    }

    @Test
    fun `x + (y + z)`() {
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

    private companion object {
        // Define the vocabulary tagged with friendly names.
        object Start : Fragment("0")
        object End : Fragment("$")
        object Expr : Fragment("E")
        object Term : Fragment("T")
        object Number : Fragment("N")
        object LParen : Fragment("(")
        object RParen : Fragment(")")
        object Plus : Fragment("+")

        // A grammar is a list of productions.
        // The first defines the start fragment on the LHS and the end fragment at the end of the RHS.
        // These fragments must appear nowhere else.
        val grammar = listOf(
            Start(Expr, End),
            Expr(Expr, Plus, Term),
            Expr(Term),
            Term(Number),
            Term(LParen, Expr, RParen)
        ).apply {
            println("--- Grammar")
            forEach { println(it.show) }
        }

        val parser = runGrammar(grammar).apply {
            println("--- States")
            states.forEach { println(it.show) }
        }

        fun <T> Iterator<T>.toList(): List<T> =
            buildList {
                while (hasNext()) {
                    add(next())
                }
            }

        private fun testInput(input: List<Fragment>, expected: PTNode) {
            val input = input.iterator()
            val actual = runParser(parser, input)
            assertTrue(!input.hasNext(), "Remaining input: ${input.toList().joinToString()}")
            assertEquals(expected, actual, "Wrong parse tree.")
        }
    }
}