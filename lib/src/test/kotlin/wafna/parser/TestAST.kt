package wafna.parser

import kotlin.test.Test
import java.util.*

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
    @Test
    fun `--- Test AST`() {
        println("--- Grammar [${grammar.size}]")
        grammar.forEach { println(it) }
        val parser = generateParser(grammar) {
            conflictMode = ConflictMode.Shift
        }
        println("--- States [${parser.states.size}]")
        parser.states.forEach { print("--- "); print(it.show) }
        fun run(vararg input: TerminalToken) {
            println("INPUT: ${input.joinToString(" ")}")
            val builder = TreeBuilder()
            runParser(parser, builder, input.iterator())
            println(builder.tree.peek().show)
        }
        run(x, plus, y, minus, z)
        run(x, plus, lparen, y, minus, z, rparen)
        run(lparen, x, plus, y, rparen, minus, z)
        run(x, times, y, divide, z)
        run(x, times, lparen, y, divide, z, rparen)
        run(lparen, x, times, y, rparen, divide, z)

        run(lparen, x, plus, y, rparen, times, z)
    }

    private companion object {
        // Token Types
        //// Augmenting.
        object Start : NonTerminal("@")
        object End : Terminal("$")
        //// Non-terminals.
        object Expr : NonTerminal("E")
        object Expr1 : NonTerminal("E1")
        object Expr2 : NonTerminal("E2")
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
        val minus = Minus.token("-")
        val times = Times.token("*")
        val divide = Divide.token("/")
        val x = Id.token("x")
        val y = Id.token("y")
        val z = Id.token("z")

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

        class TreeBuilder : ParseListener() {
            val tree = Stack<PNode>()
            val ops = Stack<Token>()
            override fun shift(token: Token) {
                when (token.type) {
                    Id -> tree.push(PNode.Id(token))
                    Plus -> ops.push(token)
                    Minus -> ops.push(token)
                    Times -> ops.push(token)
                    Divide -> ops.push(token)
                    else -> {}
                }
            }

            fun reduceOp(token: NonTerminal, count: Int) {
                if (count == 3) {
                    val children = List(2) { tree.pop() }
                    val op = ops.pop()
                    val n = when (op.type) {
                        Plus -> PNode.Plus(children[1], children[0])
                        Minus -> PNode.Minus(children[1], children[0])
                        Times -> PNode.Times(children[1], children[0])
                        Divide -> PNode.Divide(children[1], children[0])
                        else -> error(op.type.toString())
                    }
                    tree.push(n)
                }
            }

            override fun reduce(token: NonTerminal, count: Int) {
                when (token.token.type) {
                    Expr -> reduceOp(token, count)
                    Expr1 -> reduceOp(token, count)
                    else -> {}
                }
            }

            override fun accept() {
                require(tree.size == 1) {
                    "Tree ${tree.size}\n${List(tree.size) { i -> "[$i]\n${tree.pop().show}" }.joinToString("\n")}"
                }
            }
        }
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