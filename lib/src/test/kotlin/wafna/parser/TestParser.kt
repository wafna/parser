package wafna.parser

import wafna.parser.arithmetic.End
import wafna.parser.arithmetic.EOp
import wafna.parser.arithmetic.EAtom
import wafna.parser.arithmetic.EParens
import wafna.parser.arithmetic.Id
import wafna.parser.arithmetic.LParen
import wafna.parser.arithmetic.Plus
import wafna.parser.arithmetic.RParen
import wafna.parser.arithmetic.Start
import wafna.parser.arithmetic.Times
import wafna.parser.arithmetic.lparen
import wafna.parser.arithmetic.plus
import wafna.parser.arithmetic.rparen
import wafna.parser.arithmetic.times
import wafna.parser.arithmetic.x
import wafna.parser.arithmetic.y
import wafna.parser.arithmetic.z
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
            Start.produces(EOp, End),
            EOp.produces(EOp, Plus, EAtom),
            EOp.produces(EAtom),
            EAtom.produces(Id),
            EAtom.produces(LParen, EOp, RParen)
        )
        val grammarELPT = listOf(
            Start.produces(EOp, End),
            EOp.produces(EOp, Plus, EAtom),
            EOp.produces(EAtom),
            EAtom.produces(EAtom, Times, EParens),
            EAtom.produces(EParens),
            EParens.produces(Id),
            EParens.produces(LParen, EOp, RParen)
        )
    }
}

