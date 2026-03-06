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

// A very simple syntax tree.
sealed interface PNode {
    class Id(val token: Token) : PNode
    class Plus(val left: PNode, val right: PNode) : PNode
    class Minus(val left: PNode, val right: PNode) : PNode
    class Times(val left: PNode, val right: PNode) : PNode
    class Divide(val left: PNode, val right: PNode) : PNode
}

/**
 * Building a syntax tree from the parser event stream.
 * More of a demo than a test.
 */
class TestAST {
//    @Test
//    fun `Test AST`() {
//        println("--- Grammar [${grammar.size}]")
//        grammar.forEach { println(it) }
//        val parser = generateParser(grammar) {
//            conflictMode = ConflictMode.Shift
//        }
//        //        println("--- States [${parser.states.size}]")
//        //        parser.states.forEach { print("--- "); print(it.show) }
//        fun run(vararg input: TerminalToken) {
//            println("INPUT: ${input.joinToString(" ")}")
//            val builder = TreeBuilder()
//            runParser(parser, builder, input.iterator())
//            println(builder.tree.peek().show)
//        }
//        run(x, plus, y, minus, z)
//        run(x, plus, lparen, y, minus, z, rparen)
//        run(lparen, x, plus, y, rparen, minus, z)
//        run(x, times, y, divide, z)
//        run(x, times, lparen, y, divide, z, rparen)
//        run(lparen, x, times, y, rparen, divide, z)
//
//        run(lparen, x, plus, y, rparen, times, z)
//    }

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

val PNode.show: String
    get() = buildString {
        fun show(node: PNode, indent: Int) {
            repeat(indent) { append("  ") }
            when (node) {
                is PNode.Id -> appendLine(node.token.toString())
                is PNode.Plus -> {
                    appendLine("+")
                    show(node.left, indent + 1)
                    show(node.right, indent + 1)
                }

                is PNode.Minus -> {
                    appendLine("-")
                    show(node.left, indent + 1)
                    show(node.right, indent + 1)
                }

                is PNode.Times -> {
                    appendLine("*")
                    show(node.left, indent + 1)
                    show(node.right, indent + 1)
                }

                is PNode.Divide -> {
                    appendLine("/")
                    show(node.left, indent + 1)
                    show(node.right, indent + 1)
                }
            }
        }
        show(this@show, 0)
    }