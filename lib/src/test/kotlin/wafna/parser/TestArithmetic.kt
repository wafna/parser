package wafna.parser

import kotlin.test.Test
import kotlin.test.assertTrue

class TestArithmetic {

    @Test
    fun `test arithmetic`() {
        val parser = generateParser(grammar) {
            conflictMode = ConflictMode.Shift
        }
        parser.states.forEach { print("--- "); print(it.show) }
        fun testInput(input: List<TerminalToken>) {
            val input = input.iterator()
            val actual = parseToTree(parser, input)
            assertTrue(!input.hasNext(), "Remaining input: ${input.toList().joinToString()}")
            println(actual.show)
        }
        testInput(input = listOf(x, plus, y))
        testInput(input = listOf(x, plus, y, plus, z))
        testInput(input = listOf(x, divide, lparen, y, plus, z, rparen, minus, w))
        testInput(input = listOf(x, plus, lparen, y, minus, z, rparen, times, w))
    }

    private companion object {

        val grammar = listOf(
            Start.produces(Expr, End),
            Expr.produces(Expr, Plus, Expr1),
            Expr.produces(Expr, Minus, Expr1),
            Expr.produces(Expr1),
            Expr1.produces(Expr1, Times, Expr2),
            Expr1.produces(Expr1, Divide, Expr2),
            Expr1.produces(Expr2),
            Expr2.produces(Id),
            Expr2.produces(LParen, Expr, RParen)
        )
    }
}