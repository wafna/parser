package wafna.parser

import wafna.parser.arithmetic.Divide
import wafna.parser.arithmetic.End
import wafna.parser.arithmetic.EOp
import wafna.parser.arithmetic.EAtom
import wafna.parser.arithmetic.EParens
import wafna.parser.arithmetic.Id
import wafna.parser.arithmetic.LParen
import wafna.parser.arithmetic.Minus
import wafna.parser.arithmetic.Plus
import wafna.parser.arithmetic.RParen
import wafna.parser.arithmetic.Start
import wafna.parser.arithmetic.Times
import wafna.parser.arithmetic.divide
import wafna.parser.arithmetic.lparen
import wafna.parser.arithmetic.minus
import wafna.parser.arithmetic.plus
import wafna.parser.arithmetic.rparen
import wafna.parser.arithmetic.times
import wafna.parser.arithmetic.w
import wafna.parser.arithmetic.x
import wafna.parser.arithmetic.y
import wafna.parser.arithmetic.z
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
            Start.produces(EOp, End),
            EOp.produces(EOp, Plus, EAtom),
            EOp.produces(EOp, Minus, EAtom),
            EOp.produces(EAtom),
            EAtom.produces(EAtom, Times, EParens),
            EAtom.produces(EAtom, Divide, EParens),
            EAtom.produces(EParens),
            EParens.produces(Id),
            EParens.produces(LParen, EOp, RParen)
        )
    }
}