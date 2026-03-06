package wafna.parser

import kotlin.test.Test
import java.util.*

// A very simple syntax tree.
sealed interface PNode {
    class Id(val token: Token) : PNode
    class Plus(val left: PNode, val right: PNode) : PNode
}

/**
 * More of a demo than a test.
 */
class TestAST {
    @Test
    fun `--- Test AST`() {
        val parser = generateParser(grammar)
        val tree = Stack<PNode>()
        val builder = object : ParseListener() {
            override fun shift(token: Token) {
                when (token.type) {
                    Id -> tree.push(PNode.Id(token))
                    else -> {}
                }
            }

            override fun reduce(token: NonTerminal, count: Int) {
                when (token.token.type) {
                    Expr -> if (count == 3) {
                        val children = List(2) { tree.pop() }
                        tree.push(PNode.Plus(children[1], children[0]))
                    }

                    else -> {}
                }
            }

            override fun accept() {
                require(tree.size == 1)
            }
        }
        runParser(parser, builder, listOf(x, plus, lparen, y, plus, z, rparen).iterator())
        println(tree.peek().show)
    }

    private companion object {
        // Token Types
        //// Augmenting.
        object Start : NonTerminal("@")
        object End : Terminal("$")
        //// Non-terminals.
        object Expr : NonTerminal("E")
        object Tail : NonTerminal("T")
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

        val grammar = listOf(
            Start.produces(Expr, End),
            Expr.produces(Expr, Plus, Tail),
            Expr.produces(Tail),
            Tail.produces(Id),
            Tail.produces(LParen, Expr, RParen)
        )
    }
}

val PNode.show: String
    get() = buildString {
        fun show(node: PNode, indent: Int) {
            repeat(indent) { append("  ") }
            when (node) {
                is PNode.Id -> appendLine(node.token.text)
                is PNode.Plus -> {
                    appendLine("+")
                    show(node.left, indent + 1)
                    show(node.right, indent + 1)
                }
            }
        }
        show(this@show, 0)
    }