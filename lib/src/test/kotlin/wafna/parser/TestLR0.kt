package wafna.parser

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private operator fun FragmentType.invoke(text: String? = null): Fragment =
    Fragment(this, text)

class TestLR0 {
    @Test
    fun `x + y`() {
        testInput(
            listOf(Number("12"), Plus("+"), Number("24")),
            PTNode(
                fragment = Expr(),
                children = listOf(
                    PTNode(Expr(), listOf(PTNode(Term(), listOf(PTNode(Number("12")))))),
                    PTNode(Plus("+")),
                    PTNode(Term(), listOf(PTNode(Number("24"))))
                )
            )
        )
    }

    @Test
    fun `(x)`() {
        testInput(
            listOf(LParen("("), Number("42"), RParen(")")),
            PTNode(
                Expr(),
                listOf(
                    PTNode(
                        Term(), listOf(
                            PTNode(LParen("(")),
                            PTNode(Expr(), listOf(PTNode(Term(), listOf(PTNode(Number("42")))))),
                            PTNode(RParen(")"))
                        )
                    )
                )
            )
        )
    }

    @Test
    fun `x + (y + z)`() {
        testInput(
            listOf(Number("55"), Plus("+"), LParen("("), Number("336"), Plus("+"), Number("85"), RParen(")")),
            PTNode(
                Expr(),
                listOf(
                    PTNode(Expr(), listOf(PTNode(Term(), listOf(PTNode(Number("55")))))),
                    PTNode(Plus("+")),
                    PTNode(
                        Term(),
                        listOf(
                            PTNode(LParen("(")),
                            PTNode(
                                Expr(),
                                listOf(
                                    PTNode(Expr(), listOf(PTNode(Term(), listOf(PTNode(Number("336")))))),
                                    PTNode(Plus("+")),
                                    PTNode(Term(), listOf(PTNode(Number("85"))))
                                )
                            ),
                            PTNode(RParen(")"))
                        )
                    )
                )
            )
        )
    }

    private companion object {
        // Define the vocabulary tagged with friendly names.
        object Start : FragmentType("∅")
        object End : FragmentType("$")
        object Expr : FragmentType("E")
        object Term : FragmentType("T")
        object Number : FragmentType("N")
        object LParen : FragmentType("(")
        object RParen : FragmentType(")")
        object Plus : FragmentType("+")

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