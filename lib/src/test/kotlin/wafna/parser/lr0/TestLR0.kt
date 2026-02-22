package wafna.parser.lr0

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.assertThrows
import wafna.parser.toList

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
                    PTNode(Expr(), listOf(PTNode(TSum(), listOf(PTNode(x))))),
                    PTNode(plus),
                    PTNode(TSum(), listOf(PTNode(y)))
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
                        TSum(), listOf(
                            PTNode(lparen),
                            PTNode(Expr(), listOf(PTNode(TSum(), listOf(PTNode(x))))),
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
                    PTNode(Expr(), listOf(PTNode(TSum(), listOf(PTNode(x))))),
                    PTNode(plus),
                    PTNode(
                        TSum(),
                        listOf(
                            PTNode(lparen),
                            PTNode(
                                Expr(),
                                listOf(
                                    PTNode(Expr(), listOf(PTNode(TSum(), listOf(PTNode(y))))),
                                    PTNode(plus),
                                    PTNode(TSum(), listOf(PTNode(z)))
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

    @Test
    fun `grammar conflict`() {
        // Shift reduce conflict in state 4.
        assertThrows<Throwable> {
            val grammar = listOf(
                Start(Expr, End),
                Expr(Expr, Plus, TSum),
                Expr(TSum),
                TSum(TSum, Times, TProd),
                TSum(TProd),
                TProd(Id),
                TProd(LParen, Expr, RParen)
            ).apply {
                println("--- Grammar")
                forEach { println(it.show) }
            }
            val parser = runGrammar(grammar).apply {
                println("--- States")
                states.forEach { println(it.show) }
            }
        }
    }

    private companion object {
        // Define the vocabulary tagged with friendly names.
        object Start : FragmentType("∅")
        object End : FragmentType("$")
        object Expr : FragmentType("E")
        object TSum : FragmentType("T")
        object TProd : FragmentType("P")
        object Id : FragmentType("id")
        object LParen : FragmentType("(")
        object RParen : FragmentType(")")
        object Plus : FragmentType("+")
        object Times : FragmentType("*")

        val rparen = RParen("(")
        val lparen = LParen("(")
        val plus = Plus("+")
        val x = Id("x")
        val y = Id("y")
        val z = Id("z")

        val grammar = listOf(
            Start(Expr, End),
            Expr(Expr, Plus, TSum),
            Expr(TSum),
            TSum(Id),
            TSum(LParen, Expr, RParen)
        )

        val parser = runGrammar(grammar)

        private fun testInput(input: List<Fragment>, expected: PTNode) {
            val input = input.iterator()
            val actual = runParser(parser, input)
            assertTrue(!input.hasNext(), "Remaining input: ${input.toList().joinToString()}")
            assertEquals(expected, actual, "Wrong parse tree.")
        }
    }
}