package wafna.parser

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
    @Test
    fun `test ELPT`() {
//        println("--- Grammar ELPT")
//        grammarELPT.forEach { println(it.toString()) }
        val parser = generateParser(grammarELPT) {
            conflictMode = ConflictMode.Shift
        }
        parser.states.forEach { print("--- "); print(it.show) }
        fun testInput(input: List<TerminalToken>) {
            val iterator = input.iterator()
            val actual = parseToTree(parser, iterator)
            assertTrue(!iterator.hasNext(), "Remaining input: ${iterator.toList().joinToString()}")
            println("INPUT: ${input.joinToString(" ") { it.toString() }}")
            println("PARSE: ${actual.show}")
        }
        testInput(listOf(x, plus, y, times, z))
        testInput(listOf(x, times, y, plus, z))
        testInput(listOf(x, times, lparen, y, plus, z, rparen))
    }

    private companion object {
        val grammarELP = listOf(
            Start.produces(Expr, End),
            Expr.produces(Expr, Plus, Expr1),
            Expr.produces(Expr1),
            Expr1.produces(Id),
            Expr1.produces(LParen, Expr, RParen)
        )
        val grammarELPT = listOf(
            Start.produces(Expr, End),
            Expr.produces(Expr, Plus, Expr1),
            Expr.produces(Expr1),
            Expr1.produces(Expr1, Times, Expr2),
            Expr1.produces(Expr2),
            Expr2.produces(Id),
            Expr2.produces(LParen, Expr, RParen)
        )
    }
}

