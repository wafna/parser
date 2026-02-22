package wafna.parser

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.assertThrows

private operator fun FragmentType.invoke(text: String? = null): Fragment =
    Fragment(this, text)

class TestLR0 {
    @Test
    fun `good x + y`() {
        testInput(
            listOf(x, plus, y),
            PTNode(
                fragment = Expr(),
                children = listOf(
                    PTNode(Expr(), listOf(PTNode(Term(), listOf(PTNode(x))))),
                    PTNode(plus),
                    PTNode(Term(), listOf(PTNode(y)))
                )
            )
        )
    }

    @Test
    fun `good (x)`() {
        testInput(
            listOf(lparen, x, rparen),
            PTNode(
                Expr(),
                listOf(
                    PTNode(
                        Term(), listOf(
                            PTNode(lparen),
                            PTNode(Expr(), listOf(PTNode(Term(), listOf(PTNode(x))))),
                            PTNode(rparen)
                        )
                    )
                )
            )
        )
    }

    @Test
    fun `good x + (y + z)`() {
        testInput(
            listOf(x, plus, lparen, y, plus, z, rparen),
            PTNode(
                Expr(),
                listOf(
                    PTNode(Expr(), listOf(PTNode(Term(), listOf(PTNode(x))))),
                    PTNode(plus),
                    PTNode(
                        Term(),
                        listOf(
                            PTNode(lparen),
                            PTNode(
                                Expr(),
                                listOf(
                                    PTNode(Expr(), listOf(PTNode(Term(), listOf(PTNode(y))))),
                                    PTNode(plus),
                                    PTNode(Term(), listOf(PTNode(z)))
                                )
                            ),
                            PTNode(rparen)
                        )
                    )
                )
            )
        )
    }

    @Test
    fun `bad x y`() {
        assertThrows<Throwable> {
            testInput(
                listOf(x, y),
                PTNode(Start())
            )
        }
    }
    @Test
    fun `bad   ( (`() {
        assertThrows<Throwable> {
            testInput(
                listOf(lparen, lparen),
                PTNode(Start())
            )
        }
    }

    @Test
    fun `bad ) x`() {
        assertThrows<Throwable> {
            testInput(
                listOf(rparen, x),
                PTNode(Start())
            )
        }
    }

    private companion object {
        // Define the vocabulary tagged with friendly names.
        object Start : FragmentType("∅")
        object End : FragmentType("$")
        object Expr : FragmentType("E")
        object Term : FragmentType("T")
        object Id : FragmentType("id")
        object LParen : FragmentType("(")
        object RParen : FragmentType(")")
        object Plus : FragmentType("+")

        val rparen = RParen("(")
        val lparen = LParen("(")
        val plus = Plus("+")
        val x = Id("x")
        val y = Id("y")
        val z = Id("z")

        val grammar = listOf(
            Start(Expr, End),
            Expr(Expr, Plus, Term),
            Expr(Term),
            Term(Id),
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