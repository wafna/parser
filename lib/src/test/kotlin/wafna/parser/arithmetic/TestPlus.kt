package wafna.parser.arithmetic

import wafna.parser.TerminalToken
import wafna.parser.generateParser
import wafna.parser.produces
import wafna.parser.toList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestPlus {
    @Test
    fun test() {
        testInput(listOf(x), AST.Id(x))
        testInput(listOf(x, plus, y), AST.Plus(AST.Id(x), AST.Id(y)))
        testInput(listOf(x, plus, y, plus, z), AST.Plus(AST.Plus(AST.Id(x), AST.Id(y)), AST.Id(z)))
        testInput(listOf(lparen, x, plus, y, rparen, plus, z), AST.Plus(AST.Plus(AST.Id(x), AST.Id(y)), AST.Id(z)))
        testInput(listOf(x, plus, lparen, y, plus, z, rparen), AST.Plus(AST.Id(x), AST.Plus(AST.Id(y), AST.Id(z))))
    }

    companion object {
        val grammar = listOf(
            Start.produces(EOp, End),
            EOp.produces(EOp, Plus, EAtom),
            EOp.produces(EAtom),
            EAtom.produces(Id),
            EAtom.produces(EParens),
            EParens.produces(LParen, EOp, RParen)
        ).apply {
            println("--- Grammar")
            forEach { println(it) }
        }
        val parser = generateParser(grammar)
        fun testInput(input: List<TerminalToken>, expected: AST) {
            println("INPUT: ${input.joinToString(" ")}")
            val input = input.iterator()
            val actual = parseToAST(parser, input)
            assertTrue(!input.hasNext(), "Remaining input: ${input.toList().joinToString()}")
            println(actual.show)
            assertEquals(expected, actual)
        }
    }
}