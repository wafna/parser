package wafna.parser.arithmetic

import wafna.parser.*
import java.util.*

sealed interface AST {
    data class Id(val token: Token) : AST
    data class Plus(val left: AST, val right: AST) : AST
    data class Minus(val left: AST, val right: AST) : AST
    data class Times(val left: AST, val right: AST) : AST
    data class Divide(val left: AST, val right: AST) : AST
}

class ASTBuilder : ActionListener() {
    val tree = Stack<AST>()
    val ops = Stack<Token>()
    override fun shift(token: Token) {
        when (token.type) {
            Id -> tree.push(AST.Id(token))
            Plus -> ops.push(token)
            Minus -> ops.push(token)
            Times -> ops.push(token)
            Divide -> ops.push(token)
            else -> {}
        }
    }

    fun reduceOp(count: Int) {
        if (count == 3) {
            val children = List(2) { tree.pop() }
            val op = ops.pop()
            val n = when (op.type) {
                Plus -> AST.Plus(children[1], children[0])
                Minus -> AST.Minus(children[1], children[0])
                Times -> AST.Times(children[1], children[0])
                Divide -> AST.Divide(children[1], children[0])
                else -> error(op.type.toString())
            }
            tree.push(n)
        }
    }

    override fun reduce(token: NonTerminal, count: Int) {
        when (token) {
            EOp -> reduceOp(count)
            EAtom -> reduceOp(count)
            else -> {}
        }
    }

    override fun accept() {
        require(tree.size == 1) {
            "Tree ${tree.size}\n${List(tree.size) { i -> "[$i]\n${tree.pop()}" }.joinToString("\n")}"
        }
    }
}

fun parseToAST(parser: Parser, input: Iterator<TerminalToken>): AST {
    val builder = ASTBuilder()
    runParser(parser, builder, input)
    return builder.tree.pop().also {
        require(builder.tree.isEmpty())
    }
}

val AST.show: String
    get() = buildString {
        fun show(node: AST, indent: Int) {
            repeat(indent) { append("  ") }
            when (node) {
                is AST.Id -> appendLine(node.token.toString())
                is AST.Plus -> {
                    appendLine("+")
                    show(node.left, indent + 1)
                    show(node.right, indent + 1)
                }

                is AST.Minus -> {
                    appendLine("-")
                    show(node.left, indent + 1)
                    show(node.right, indent + 1)
                }

                is AST.Times -> {
                    appendLine("*")
                    show(node.left, indent + 1)
                    show(node.right, indent + 1)
                }

                is AST.Divide -> {
                    appendLine("/")
                    show(node.left, indent + 1)
                    show(node.right, indent + 1)
                }
            }
        }
        show(this@show, 0)
    }