package wafna.parser

import java.util.Stack

/**
 * Generic parse tree.
 */
data class ParseTree(val token: Token, val children: List<ParseTree> = emptyList())

/**
 * Creates a tree matching the generative structure of the grammar for the input.
 */
fun parseToTree(parser: Parser, input: Iterator<TerminalToken>): ParseTree {
    val tree = Stack<ParseTree>()
    val builder = object : ActionListener() {
        override fun shift(token: Token) {
            tree.push(ParseTree(token))
        }

        override fun reduce(token: NonTerminal, count: Int) {
            val children = List(count) { tree.pop() }.reversed()
            tree.push(ParseTree(NonTerminalToken(token), children))
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

val ParseTree.show: String
    get() = buildString {
        fun showNode(node: ParseTree, indent: Int) {
            repeat(indent) { append("  ") }
            append(node.token.toString())
            appendLine(" [${node.children.size}]")
            node.children.forEach { showNode(it, indent + 1) }
        }
        showNode(this@show, 0)
    }