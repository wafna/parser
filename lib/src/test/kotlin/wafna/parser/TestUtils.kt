package wafna.parser

import java.util.*

fun <T> Iterator<T>.toList(): List<T> = buildList { while (hasNext()) add(next()) }

fun Terminal.token(text: String): TerminalToken =
    TerminalToken(this, text)

val NonTerminal.token: NonTerminalToken
    get() = NonTerminalToken(this)

val Token.show: String
    get() = text ?: type.toString()

data class ParseNode(val token: Token, val children: List<ParseNode> = emptyList())

fun parseToTree(parser: Parser, input: Iterator<TerminalToken>): ParseNode {
    val tree = Stack<ParseNode>()
    // After a reduction the new node is pushed back to the input.
    // This remembers to ignore it when it gets shifted back.
    // Note: there are never two reductions in succession;
    // the new symbol must precipitate a shift to a new reducing state.
    var reduced = false
    val builder = object : ParseListener {
        override fun shift(token: Token) {
            if (reduced) reduced = false
            else tree.push(ParseNode(token))
        }

        override fun reduce(token: NonTerminal, count: Int) {
            require(!reduced) { "Successive reductions." }
            reduced = true
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
            append(node.token.show)
            appendLine(" [${node.children.size}]")
            node.children.forEach { showNode(it, indent + 1) }
        }
        showNode(this@show, 0)
    }