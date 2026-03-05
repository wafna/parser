package wafna.parser

import kotlin.test.Test
import kotlin.test.assertTrue

private fun Terminal.token(text: String): TerminalToken =
    TerminalToken(this, text)

private val NonTerminal.token: NonTerminalToken
    get() = NonTerminalToken(this)

class TestLR1 {
    @Test
    fun `test ELP`() {
        val parser = generateParser(grammarELP)
//        parser.states.forEach { print("--- "); print(it.show) }
        fun testInput(input: List<TerminalToken>) {
            val input = input.iterator()
            val actual = runParser(parser, input)
            assertTrue(!input.hasNext(), "Remaining input: ${input.toList().joinToString()}")
            println(actual.show)
        }
        testInput(input = listOf(x, plus, y))
        testInput(input = listOf(x, plus, y, plus, z))
        testInput(input = listOf(x, plus, lparen, y, plus, z, rparen))
    }
    @Test
    fun `test ELPT`() {
        println("--- Grammar ELPT")
        grammarELPT.forEach { println(it.toString()) }
        val parser = generateParser(grammarELPT)
        parser.states.forEach { print("--- "); print(it.show) }
        fun testInput(input: List<TerminalToken>) {
            val iterator = input.iterator()
            val actual = runParser(parser, iterator)
            assertTrue(!iterator.hasNext(), "Remaining input: ${iterator.toList().joinToString()}")
            println("INPUT: ${input.joinToString(" ") { it.show }}")
            println("PARSE: ${actual.show}")
        }
        testInput(listOf(x, plus, y, times, z))
        testInput(listOf(x, times, y, plus, z))
        testInput(listOf(x, times, lparen, y, plus, z, rparen))
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
        object Times : Terminal("*")

        // Terminal tokens.
        val lparen = LParen.token("(")
        val rparen = RParen.token(")")
        val plus = Plus.token("+")
        val times = Times.token("*")
        val x = Id.token("x")
        val y = Id.token("y")
        val z = Id.token("z")

        val grammarELP = listOf(
            Start.produces(Expr, End),
            Expr.produces(Expr, Plus, TSum),
            Expr.produces(TSum),
            TSum.produces(Id),
            TSum.produces(LParen, Expr, RParen)
        )
        val grammarELPT = listOf(
            Start.produces(Expr, End),
            Expr.produces(Expr, Plus, TSum),
            Expr.produces(TSum),
            TSum.produces(TSum, Times, TProd),
            TSum.produces(TProd),
            TProd.produces(Id),
            TProd.produces(LParen, Expr, RParen)
        )
    }
}

