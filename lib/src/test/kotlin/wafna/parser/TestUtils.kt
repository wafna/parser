package wafna.parser

import java.util.*

fun <T> Iterator<T>.toList(): List<T> = buildList { while (hasNext()) add(next()) }

fun Terminal.token(text: String): TerminalToken =
    TerminalToken(this, text)

data class ParseNode(val token: Token, val children: List<ParseNode> = emptyList())

/**
 * Creates a tree matching the generative structure of the grammar for the input.
 */
fun parseToTree(parser: Parser, input: Iterator<TerminalToken>): ParseNode {
    val tree = Stack<ParseNode>()
    val builder = object : ParserListener() {
        override fun shift(token: Token) {
            tree.push(ParseNode(token))
        }

        override fun reduce(token: NonTerminal, count: Int) {
            val children = List(count) { tree.pop() }.reversed()
            tree.push(ParseNode(NonTerminalToken(token), children))
        }

        override fun accept() {
            require(tree.size == 2)
            tree.pop().also {
                require(it.token.type == parser.end) { "Internal error." }
            }
        }
    }
    runParser(parser, builder, input)
    return tree.pop().also {
        require(tree.isEmpty())
    }
}

val ParseNode.show: String
    get() = buildString {
        fun showNode(node: ParseNode, indent: Int) {
            repeat(indent) { append("  ") }
            append(node.token.toString())
            appendLine(" [${node.children.size}]")
            node.children.forEach { showNode(it, indent + 1) }
        }
        showNode(this@show, 0)
    }