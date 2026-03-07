package wafna.parser

import wafna.parser.arithmetic.*
import kotlin.test.Test
import kotlin.test.assertTrue

class TestParser {
    @Test
    fun `test ELP`() {
        println("--- Grammar ELP")
        grammarELP.forEach { println(it.toString()) }
        val parser = generateParser(grammarELP)
//        parser.states.forEach { print("--- "); print(it.show) }
        fun testInput(input: List<TerminalToken>) {
            val input = input.iterator()
            val actual = parseToTree(parser, input)
            assertTrue(!input.hasNext(), "Remaining input: ${input.toList().joinToString()}")
            println(actual.show)
        }
        testInput(input = listOf(x, plus, y))
        testInput(input = listOf(x, plus, y, plus, z))
        testInput(input = listOf(x, plus, lparen, y, plus, z, rparen))
    }

    private companion object {
        val grammarELP = listOf(
            Start.produces(Expr, End),
            Expr.produces(Expr, Plus, Prod),
            Expr.produces(Prod),
            Prod.produces(Id),
            Prod.produces(LParen, Expr, RParen)
        )
    }
}

