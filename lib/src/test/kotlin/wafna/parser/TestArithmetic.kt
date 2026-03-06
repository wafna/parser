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
        // Token Types
        //// Augmenting.
        object Start : NonTerminal("@")
        object End : Terminal("$")
        //// Non-terminals.
        object Expr : NonTerminal("E")
        object TSum : NonTerminal("T")
        object TProd : NonTerminal("P")
        //// Terminals.
        object Id : Terminal("id")
        object LParen : Terminal("(")
        object RParen : Terminal(")")
        object Plus : Terminal("+")
        object Minus : Terminal("-")
        object Times : Terminal("*")
        object Divide : Terminal("/")

        // Terminal tokens.
        val lparen = LParen.token("(")
        val rparen = RParen.token(")")
        val plus = Plus.token("+")
        val minus = Plus.token("-")
        val times = Times.token("*")
        val divide = Times.token("/")
        val x = Id.token("x")
        val y = Id.token("y")
        val z = Id.token("z")
        val w = Id.token("w")

        val grammar = listOf(
            Start.produces(Expr, End),
            Expr.produces(Expr, Plus, TSum),
            Expr.produces(Expr, Minus, TSum),
            Expr.produces(TSum),
            TSum.produces(TSum, Times, TProd),
            TSum.produces(TSum, Divide, TProd),
            TSum.produces(TProd),
            TProd.produces(Id),
            TProd.produces(LParen, Expr, RParen)
        )
    }
}